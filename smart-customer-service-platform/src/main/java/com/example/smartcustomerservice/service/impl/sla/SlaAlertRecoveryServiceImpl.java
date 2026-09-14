package com.example.smartcustomerservice.service.impl.sla;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.smartcustomerservice.config.properties.SlaAlertRecoveryProperties;
import com.example.smartcustomerservice.domain.entity.SlaAlert;
import com.example.smartcustomerservice.domain.vo.SlaAlertRecoveryVO;
import com.example.smartcustomerservice.mapper.sla.SlaAlertMapper;
import com.example.smartcustomerservice.mq.message.SlaAlertMessage;
import com.example.smartcustomerservice.mq.producer.SlaAlertMessageProducer;
import com.example.smartcustomerservice.service.sla.SlaAlertRecoveryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
// SlaAlertRecoveryServiceImpl 负责恢复长期未处理的 SLA 告警。
public class SlaAlertRecoveryServiceImpl implements SlaAlertRecoveryService {

    private final SlaAlertMapper slaAlertMapper;
    private final SlaAlertMessageProducer slaAlertMessageProducer;
    private final SlaAlertRecoveryProperties recoveryProperties;

    public SlaAlertRecoveryServiceImpl(SlaAlertMapper slaAlertMapper,
                                       SlaAlertMessageProducer slaAlertMessageProducer,
                                       SlaAlertRecoveryProperties recoveryProperties) {
        this.slaAlertMapper = slaAlertMapper;
        this.slaAlertMessageProducer = slaAlertMessageProducer;
        this.recoveryProperties = recoveryProperties;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlaAlertRecoveryVO recoverStaleAlerts() {
        LocalDateTime now = LocalDateTime.now();
        int batchSize = Math.max(1, recoveryProperties.getBatchSize());
        SlaAlertRecoveryVO recoveryVO = new SlaAlertRecoveryVO();
        recoveryVO.setStartedAt(now);
        recoveryVO.setPendingRecoveredCount(0);
        recoveryVO.setProcessingRecoveredCount(0);
        recoveryVO.setPublishedCount(0);

        LocalDateTime pendingStaleBefore = now
                .minusMinutes(recoveryProperties.getPendingGraceMinutes());
        //获取过期pending
        List<SlaAlert> slaPendingList = slaAlertMapper.selectList(new LambdaQueryWrapper<SlaAlert>()
                .eq(SlaAlert::getStatus, "PENDING")
                .le(SlaAlert::getScheduledAt, now)
                .le(SlaAlert::getUpdatedAt, pendingStaleBefore)
                .orderByAsc(SlaAlert::getUpdatedAt)
                .last("LIMIT " + batchSize)
        );

        //获取过期PROCESSING
        LocalDateTime processingStaleBefore = now
                .minusMinutes(recoveryProperties.getProcessingTimeoutMinutes());
        List<SlaAlert> slaProcessingList = slaAlertMapper.selectList(new LambdaQueryWrapper<SlaAlert>()
                .eq(SlaAlert::getStatus, "PROCESSING")
                .le(SlaAlert::getUpdatedAt, processingStaleBefore)
                .orderByAsc(SlaAlert::getUpdatedAt)
                .last("LIMIT " + batchSize)
        );

        List<SlaAlert> needPublishList = new ArrayList<>();
        //领取过期pending
        for (SlaAlert slaPending : slaPendingList) {
            int updated = slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                    .eq(SlaAlert::getId, slaPending.getId())
                    .eq(SlaAlert::getStatus, "PENDING")
                    .le(SlaAlert::getScheduledAt, now)
                    .le(SlaAlert::getUpdatedAt, pendingStaleBefore)
                    .set(SlaAlert::getLastErrorMessage, "SLA告警待处理超时，系统自动重新投递")
                    .set(SlaAlert::getUpdatedAt, now)
            );
            //只有领取成功才重新投递
            if (updated == 1) {
                needPublishList.add(slaPending);
                recoveryVO.setPendingRecoveredCount(recoveryVO.getPendingRecoveredCount() + 1);
            }
        }

        //将状态回调为PENDING
        for (SlaAlert slaProcessing : slaProcessingList) {
            int updated = slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                    .eq(SlaAlert::getId, slaProcessing.getId())
                    .eq(SlaAlert::getStatus, "PROCESSING")
                    .le(SlaAlert::getUpdatedAt, processingStaleBefore)
                    .set(SlaAlert::getStatus, "PENDING")
                    .set(SlaAlert::getLastErrorMessage, "SLA告警处理超时，系统自动恢复后重新投递")
                    .set(SlaAlert::getUpdatedAt, now)
            );
            //只有回调成功才重新投递
            if (updated == 1) {
                needPublishList.add(slaProcessing);
                recoveryVO.setProcessingRecoveredCount(recoveryVO.getProcessingRecoveredCount() + 1);
            }
        }

        //事务提交后再重投消息
        publishAfterCommit(needPublishList, recoveryVO);
        recoveryVO.setFinishedAt(LocalDateTime.now());
        return recoveryVO;
    }

    private void publishAfterCommit(List<SlaAlert> needPublishList, SlaAlertRecoveryVO recoveryVO) {
        if (needPublishList.isEmpty()) {
            return;
        }

        AtomicInteger publishedCount = new AtomicInteger();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (SlaAlert slaAlert : needPublishList) {
                    try {
                        slaAlertMessageProducer.sendRetryAlert(new SlaAlertMessage(slaAlert.getId()));
                        publishedCount.incrementAndGet();
                    } catch (RuntimeException exception) {
                        //投递失败回调为FAILED
                        markPublishFailed(slaAlert.getId(), exception.getMessage());
                    }
                }
                recoveryVO.setPublishedCount(publishedCount.get());
                recoveryVO.setFinishedAt(LocalDateTime.now());
            }
        });
    }

    private void markPublishFailed(Long alertId, String errorMessage) {
        String message = errorMessage == null ? "SLA告警兜底重投失败" : "SLA告警兜底重投失败：" + errorMessage;
        if (message.length() > 1000) {
            message = message.substring(0, 1000);
        }
        slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                .eq(SlaAlert::getId, alertId)
                .eq(SlaAlert::getStatus, "PENDING")
                .set(SlaAlert::getStatus, "FAILED")
                .set(SlaAlert::getLastErrorMessage, message)
                .set(SlaAlert::getUpdatedAt, LocalDateTime.now())
        );
    }
}
