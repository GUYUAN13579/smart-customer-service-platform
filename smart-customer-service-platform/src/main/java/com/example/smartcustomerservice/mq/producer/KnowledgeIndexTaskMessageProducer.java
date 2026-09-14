package com.example.smartcustomerservice.mq.producer;

import com.example.smartcustomerservice.common.constants.MqConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.mq.message.KnowledgeTaskMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 负责投递知识索引任务；消息仅携带 taskId，消费者再从数据库读取任务与切片最新状态。
 */
@Component
public class KnowledgeIndexTaskMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public KnowledgeIndexTaskMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendKnowledgeIndexTask(KnowledgeTaskMessage message) {
        send(message, MqConstants.KNOWLEDGE_INDEX_EXCHANGE, MqConstants.KNOWLEDGE_INDEX_ROUTING_KEY, null);
    }

    public void sendDelayedKnowledgeIndexTask(KnowledgeTaskMessage message, long delayMillis) {
        if (delayMillis <= 0) {
            sendKnowledgeIndexTask(message);
            return;
        }
        send(message, MqConstants.KNOWLEDGE_INDEX_DELAY_EXCHANGE,
                MqConstants.KNOWLEDGE_INDEX_DELAY_ROUTING_KEY, delayMillis);
    }

    private void send(KnowledgeTaskMessage message, String exchange, String routingKey, Long delayMillis) {
        if (message == null || message.getTaskId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "知识索引任务消息不能为空");
        }

        CorrelationData correlationData = new CorrelationData(
                MqConstants.KNOWLEDGE_INDEX_CORRELATION_PREFIX
                        + message.getTaskId()
                        + ":"
                        + UUID.randomUUID()
        );

        rabbitTemplate.convertAndSend(
                exchange,
                routingKey,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties()
                            .setHeader(MqConstants.KNOWLEDGE_INDEX_TASK_ID_HEADER, message.getTaskId());
                    if (delayMillis != null) {
                        amqpMessage.getMessageProperties().setExpiration(String.valueOf(delayMillis));
                    }
                    return amqpMessage;
                },
                correlationData
        );
    }
}
