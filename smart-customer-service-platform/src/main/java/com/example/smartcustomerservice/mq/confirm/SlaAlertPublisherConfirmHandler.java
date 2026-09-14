package com.example.smartcustomerservice.mq.confirm;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.smartcustomerservice.common.constants.MqConstants;
import com.example.smartcustomerservice.domain.entity.SlaAlert;
import com.example.smartcustomerservice.mapper.sla.SlaAlertMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
// SlaAlertPublisherConfirmHandler 属于智能客服平台基础代码。
public class SlaAlertPublisherConfirmHandler {

    private static final Logger log = LoggerFactory.getLogger(SlaAlertPublisherConfirmHandler.class);

    private final RabbitTemplate rabbitTemplate;
    private final SlaAlertMapper slaAlertMapper;
    private final KnowledgeIndexPublisherConfirmHandler knowledgeIndexPublisherConfirmHandler;

    public SlaAlertPublisherConfirmHandler(RabbitTemplate rabbitTemplate,
                                           SlaAlertMapper slaAlertMapper,
                                           KnowledgeIndexPublisherConfirmHandler knowledgeIndexPublisherConfirmHandler) {
        this.rabbitTemplate = rabbitTemplate;
        this.slaAlertMapper = slaAlertMapper;
        this.knowledgeIndexPublisherConfirmHandler = knowledgeIndexPublisherConfirmHandler;
    }

    @PostConstruct
    public void registerCallbacks() {
        // confirm 表示消息是否到达交换机；return 表示到达交换机后是否能路由到队列。
        // 两种失败都要落库为 FAILED，避免告警永久停留在 PENDING 且没有补偿入口。
        rabbitTemplate.setMandatory(true);
        rabbitTemplate.setConfirmCallback(this::handleConfirm);
        rabbitTemplate.setReturnsCallback(this::handleReturnedMessage);
    }

    private void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
        if (knowledgeIndexPublisherConfirmHandler.supports(correlationData)) {
            knowledgeIndexPublisherConfirmHandler.handleConfirm(correlationData, ack, cause);
            return;
        }
        if (ack) {
            return;
        }

        Long alertId = extractAlertId(correlationData == null ? null : correlationData.getId());
        markAlertFailed(alertId, "消息未到达交换机：" + safeReason(cause));
    }

    private void handleReturnedMessage(ReturnedMessage returnedMessage) {
        if (returnedMessage != null && knowledgeIndexPublisherConfirmHandler.supports(returnedMessage.getMessage())) {
            knowledgeIndexPublisherConfirmHandler.handleReturnedMessage(returnedMessage);
            return;
        }
        Long alertId = extractAlertId(returnedMessage.getMessage());
        String reason = "消息未路由到队列：replyCode=" + returnedMessage.getReplyCode()
                + ", replyText=" + safeReason(returnedMessage.getReplyText());
        markAlertFailed(alertId, reason);
    }

    private Long extractAlertId(String correlationId) {
        if (correlationId == null || !correlationId.startsWith(MqConstants.SLA_ALERT_CORRELATION_PREFIX)) {
            return null;
        }

        String[] parts = correlationId.split(":", 3);
        if (parts.length < 2) {
            return null;
        }

        try {
            return Long.valueOf(parts[1]);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Long extractAlertId(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Object alertId = headers.get(MqConstants.SLA_ALERT_ID_HEADER);
        if (alertId instanceof Number number) {
            return number.longValue();
        }
        if (alertId instanceof String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private void markAlertFailed(Long alertId, String reason) {
        if (alertId == null) {
            log.warn("无法定位 SLA 告警，发布失败原因：{}", reason);
            return;
        }

        int updated = slaAlertMapper.update(null, new LambdaUpdateWrapper<SlaAlert>()
                .eq(SlaAlert::getId, alertId)
                .eq(SlaAlert::getStatus, "PENDING")
                .set(SlaAlert::getStatus, "FAILED")
                .set(SlaAlert::getLastErrorMessage, truncate(reason))
                .set(SlaAlert::getUpdatedAt, LocalDateTime.now()));
        if (updated == 0) {
            log.warn("SLA 告警 {} 发布失败，但当前状态不是 PENDING，原因：{}", alertId, reason);
        }
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "未知原因" : reason;
    }

    private String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
