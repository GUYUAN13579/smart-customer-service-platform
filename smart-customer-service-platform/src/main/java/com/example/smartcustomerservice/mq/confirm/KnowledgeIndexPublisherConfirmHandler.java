package com.example.smartcustomerservice.mq.confirm;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.smartcustomerservice.common.constants.MqConstants;
import com.example.smartcustomerservice.domain.entity.KnowledgeIndexTask;
import com.example.smartcustomerservice.mapper.knowledge.KnowledgeIndexTaskMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 处理知识索引任务消息的发布确认失败，保证任务不会因消息未到达 RabbitMQ 而长期停留在 PENDING。
 *
 * 回调由 {@link SlaAlertPublisherConfirmHandler} 统一注册后分发，避免多个组件互相覆盖
 * RabbitTemplate 的 ConfirmCallback 或 ReturnsCallback。
 */
@Component
public class KnowledgeIndexPublisherConfirmHandler {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIndexPublisherConfirmHandler.class);

    private final KnowledgeIndexTaskMapper knowledgeIndexTaskMapper;

    public KnowledgeIndexPublisherConfirmHandler(KnowledgeIndexTaskMapper knowledgeIndexTaskMapper) {
        this.knowledgeIndexTaskMapper = knowledgeIndexTaskMapper;
    }

    public boolean supports(CorrelationData correlationData) {
        return correlationData != null
                && correlationData.getId() != null
                && correlationData.getId().startsWith(MqConstants.KNOWLEDGE_INDEX_CORRELATION_PREFIX);
    }

    public boolean supports(Message message) {
        if (message == null) {
            return false;
        }
        return message.getMessageProperties().getHeaders().containsKey(MqConstants.KNOWLEDGE_INDEX_TASK_ID_HEADER);
    }

    public void handleConfirm(CorrelationData correlationData, boolean ack, String cause) {
        if (ack) {
            return;
        }
        markTaskFailed(extractTaskId(correlationData == null ? null : correlationData.getId()),
                "知识索引消息未到达交换机：" + safeReason(cause));
    }

    public void handleReturnedMessage(ReturnedMessage returnedMessage) {
        Long taskId = returnedMessage == null ? null : extractTaskId(returnedMessage.getMessage());
        String reason = returnedMessage == null
                ? "知识索引消息未路由到队列：未知原因"
                : "知识索引消息未路由到队列：replyCode=" + returnedMessage.getReplyCode()
                + ", replyText=" + safeReason(returnedMessage.getReplyText());
        markTaskFailed(taskId, reason);
    }

    private Long extractTaskId(String correlationId) {
        if (correlationId == null || !correlationId.startsWith(MqConstants.KNOWLEDGE_INDEX_CORRELATION_PREFIX)) {
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

    private Long extractTaskId(Message message) {
        Map<String, Object> headers = message.getMessageProperties().getHeaders();
        Object taskId = headers.get(MqConstants.KNOWLEDGE_INDEX_TASK_ID_HEADER);
        if (taskId instanceof Number number) {
            return number.longValue();
        }
        if (taskId instanceof String value) {
            try {
                return Long.valueOf(value);
            } catch (NumberFormatException exception) {
                return null;
            }
        }
        return null;
    }

    private void markTaskFailed(Long taskId, String reason) {
        if (taskId == null) {
            log.warn("无法定位知识索引任务，发布失败原因：{}", reason);
            return;
        }
        int updated = knowledgeIndexTaskMapper.update(null, new LambdaUpdateWrapper<KnowledgeIndexTask>()
                .eq(KnowledgeIndexTask::getId, taskId)
                .eq(KnowledgeIndexTask::getStatus, "PENDING")
                .set(KnowledgeIndexTask::getStatus, "FAILED")
                .set(KnowledgeIndexTask::getErrorMessage, truncate(reason))
                .set(KnowledgeIndexTask::getFinishedAt, LocalDateTime.now())
                .set(KnowledgeIndexTask::getUpdatedAt, LocalDateTime.now()));
        if (updated == 0) {
            log.warn("知识索引任务 {} 发布失败，但当前状态不是 PENDING，原因：{}", taskId, reason);
        }
    }

    private String safeReason(String reason) {
        return reason == null || reason.isBlank() ? "未知原因" : reason;
    }

    private String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000);
    }
}
