package com.example.smartcustomerservice.mq.consumer;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.smartcustomerservice.common.constants.MqConstants;
import com.example.smartcustomerservice.domain.entity.SlaAlert;
import com.example.smartcustomerservice.mapper.sla.SlaAlertMapper;
import com.example.smartcustomerservice.mq.message.SlaAlertMessage;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
// 消费达到最大重试次数的 SLA 告警，并将其收口为可由管理员人工重试的 FAILED 状态。
public class SlaAlertFailureMessageConsumer {

    private final SlaAlertMapper slaAlertMapper;

    public SlaAlertFailureMessageConsumer(SlaAlertMapper slaAlertMapper) {
        this.slaAlertMapper = slaAlertMapper;
    }

    @RabbitListener(queues = MqConstants.SLA_ALERT_FAILURE_QUEUE)
    @Transactional(rollbackFor = Exception.class)
    public void consumeFailedSlaAlert(SlaAlertMessage payload, Message rawMessage) {
        // 失败队列中的消息已不再重试；只更新仍未完成的告警，不能覆盖 SENT 或 SKIPPED 的最终状态。
        if (payload == null || payload.getAlertId() == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                .eq(SlaAlert::getId, payload.getAlertId())
                .in(SlaAlert::getStatus, "PENDING", "PROCESSING")
                .set(SlaAlert::getStatus, "FAILED")
                .set(SlaAlert::getLastErrorMessage, buildFailureReason(rawMessage))
                .set(SlaAlert::getUpdatedAt, now));
    }

    private String buildFailureReason(Message rawMessage) {
        Object xDeath = rawMessage == null
                ? null
                : rawMessage.getMessageProperties().getHeaders().get("x-death");
        String reason = "SLA 自动消费达到最大重试次数，消息已进入失败队列";
        if (xDeath != null) {
            reason += "；x-death=" + xDeath;
        }
        return reason.length() <= 1000 ? reason : reason.substring(0, 1000);
    }
}
