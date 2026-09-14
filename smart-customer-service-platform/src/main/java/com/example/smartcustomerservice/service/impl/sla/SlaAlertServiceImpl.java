package com.example.smartcustomerservice.service.impl.sla;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.PageResult;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.SlaAlertQueryRequest;
import com.example.smartcustomerservice.domain.entity.SlaAlert;
import com.example.smartcustomerservice.domain.vo.SlaAlertVO;
import com.example.smartcustomerservice.mapper.sla.SlaAlertMapper;
import com.example.smartcustomerservice.mq.message.SlaAlertMessage;
import com.example.smartcustomerservice.mq.producer.SlaAlertMessageProducer;
import com.example.smartcustomerservice.service.sla.SlaAlertService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
// SlaAlertServiceImpl 属于智能客服平台基础代码。
public class SlaAlertServiceImpl implements SlaAlertService {

    private final SlaAlertMapper slaAlertMapper;
    private final SlaAlertMessageProducer slaAlertMessageProducer;

    public SlaAlertServiceImpl(SlaAlertMapper slaAlertMapper, SlaAlertMessageProducer slaAlertMessageProducer) {
        this.slaAlertMapper = slaAlertMapper;
        this.slaAlertMessageProducer = slaAlertMessageProducer;
    }

    @Override
    public PageResult<SlaAlertVO> pageAlerts(SlaAlertQueryRequest request) {
        // TODO: 分页查询 SLA 自动告警记录。
        SlaAlertQueryRequest safeRequest = request == null ? new SlaAlertQueryRequest() : request;
        Page<SlaAlert> page = new Page<>(safeRequest.getPage(), safeRequest.getSize());
        LambdaQueryWrapper<SlaAlert> wrapper = new LambdaQueryWrapper<SlaAlert>()
                .eq(safeRequest.getTicketId() != null, SlaAlert::getTicketId, safeRequest.getTicketId())
                .eq(StringUtils.hasText(safeRequest.getAlertLevel()), SlaAlert::getAlertLevel, safeRequest.getAlertLevel())
                .eq(StringUtils.hasText(safeRequest.getAlertType()), SlaAlert::getAlertType, safeRequest.getAlertType())
                .eq(StringUtils.hasText(safeRequest.getStatus()), SlaAlert::getStatus, safeRequest.getStatus())
                .ge(safeRequest.getScheduledStartAt() != null, SlaAlert::getScheduledAt, safeRequest.getScheduledStartAt())
                .le(safeRequest.getScheduledEndAt() != null, SlaAlert::getScheduledAt, safeRequest.getScheduledEndAt())
                .orderByDesc(SlaAlert::getScheduledAt)
                .orderByDesc(SlaAlert::getId);
        Page<SlaAlert> slaAlertPage = slaAlertMapper.selectPage(page, wrapper);
        List<SlaAlert> slaAlertList = slaAlertPage.getRecords();
        List<SlaAlertVO> slaAlertVOS = slaAlertList.stream().map(
                entity -> {
                    SlaAlertVO slaAlertVO = new SlaAlertVO();
                    BeanUtils.copyProperties(entity, slaAlertVO);
                    return slaAlertVO;
                }
                ).toList();
        return PageResult.of(slaAlertVOS, slaAlertPage.getCurrent(), slaAlertPage.getSize(), slaAlertPage.getTotal());
    }

    @Override
    public SlaAlertVO getAlert(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SLA告警ID不能为空");
        }

        SlaAlert slaAlert = slaAlertMapper.selectById(id);
        if (slaAlert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SLA告警不存在");
        }
        return toSlaAlertVO(slaAlert);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SlaAlertVO retryAlert(Long id) {
        if (id == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SLA告警ID不能为空");
        }

        SlaAlert slaAlert = slaAlertMapper.selectById(id);
        if (slaAlert == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "SLA告警不存在");
        }
        if (!"FAILED".equals(slaAlert.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "只有失败状态的SLA告警可以重试");
        }

        LocalDateTime now = LocalDateTime.now();
        // 以 FAILED 为更新前置条件，防止页面打开后该告警已被其他操作恢复却再次重试。
        int updated = slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                .eq(SlaAlert::getId, id)
                .eq(SlaAlert::getStatus, "FAILED")
                .set(SlaAlert::getStatus, "PENDING")
                .set(SlaAlert::getScheduledAt, now)
                .set(SlaAlert::getSentAt, null)
                .set(SlaAlert::getRetryCount, slaAlert.getRetryCount() == null ? 1 : slaAlert.getRetryCount() + 1)
                .set(SlaAlert::getLastErrorMessage, null)
                .set(SlaAlert::getUpdatedAt, now));
        if (updated != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "SLA告警状态已变更，请刷新后重试");
        }

        // 事务提交后直接投递到重试交换机，消费者会按正常自动告警流程再次处理该记录。
        slaAlert.setStatus("PENDING");
        slaAlert.setScheduledAt(now);
        slaAlert.setSentAt(null);
        slaAlert.setRetryCount(slaAlert.getRetryCount() == null ? 1 : slaAlert.getRetryCount() + 1);
        slaAlert.setLastErrorMessage(null);
        slaAlert.setUpdatedAt(now);

        publishSlaAlertAfterCommit(slaAlert);

        return toSlaAlertVO(slaAlert);
    }

    private void publishSlaAlertAfterCommit(SlaAlert slaAlert) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                publishSlaAlert(slaAlert);
            }
        });
    }

    private void publishSlaAlert(SlaAlert slaAlert) {
        try {
            slaAlertMessageProducer.sendRetryAlert(new SlaAlertMessage(slaAlert.getId()));
        } catch (RuntimeException exception) {
            // 提交后发送失败无法回滚主事务，因此将告警标为 FAILED，交由后续重试接口补偿。
            slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                    .eq(SlaAlert::getId, slaAlert.getId())
                    .eq(SlaAlert::getStatus, "PENDING")
                    .set(SlaAlert::getStatus, "FAILED")
                    .set(SlaAlert::getLastErrorMessage, truncateAlertError("重试消息投递异常：" + exception.getMessage()))
                    .set(SlaAlert::getUpdatedAt, LocalDateTime.now()));
        }
    }

    private String truncateAlertError(String message) {
        if (message == null) {
            return "重试消息投递异常";
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    private SlaAlertVO toSlaAlertVO(SlaAlert slaAlert) {
        SlaAlertVO slaAlertVO = new SlaAlertVO();
        BeanUtils.copyProperties(slaAlert, slaAlertVO);
        return slaAlertVO;
    }
}
