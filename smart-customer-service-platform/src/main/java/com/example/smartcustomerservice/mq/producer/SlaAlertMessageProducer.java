package com.example.smartcustomerservice.mq.producer;

import com.example.smartcustomerservice.common.constants.MqConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.mq.message.SlaAlertMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
// SlaAlertMessageProducer 属于智能客服平台基础代码。
public class SlaAlertMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    public SlaAlertMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendDelayedAlert(SlaAlertMessage message, long delayMillis) {
        // TODO: 将告警消息按 delayMillis 投递到 SLA 延迟交换机。
        if (message == null || message.getAlertId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SLA告警消息不能为空");
        }
        if (delayMillis < 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "延迟时间不能小于0");
        }

        CorrelationData correlationData = new CorrelationData(
                MqConstants.SLA_ALERT_CORRELATION_PREFIX
                        + message.getAlertId()
                        + ":"
                        + UUID.randomUUID()
        );
        // 该队列没有固定 TTL，而是为每条消息设置 expiration；到期后消息经 DLX 路由到真实消费队列。
        // CorrelationData 与消息头中的 alertId 用于发布确认回调定位具体告警记录。
        rabbitTemplate.convertAndSend(
                MqConstants.SLA_ALERT_DELAY_EXCHANGE,
                MqConstants.SLA_ALERT_DELAY_ROUTING_KEY,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties()
                            .setExpiration(String.valueOf(delayMillis));
                    amqpMessage.getMessageProperties()
                            .setHeader(MqConstants.SLA_ALERT_ID_HEADER, message.getAlertId());
                    return amqpMessage;
                },
                correlationData
        );
    }

    public void sendRetryAlert(SlaAlertMessage message) {
        if (message == null || message.getAlertId() == null) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "SLA告警消息不能为空");
        }

        CorrelationData correlationData = new CorrelationData(
                MqConstants.SLA_ALERT_CORRELATION_PREFIX
                        + message.getAlertId()
                        + ":"
                        + UUID.randomUUID()
        );
        // 重试交换机直连实际消费队列，避免零延迟消息被延迟队列中的长 TTL 消息阻塞。
        // 仍携带关联 ID 和告警 ID，使发布确认回调可以统一处理失败补偿。
        rabbitTemplate.convertAndSend(
                MqConstants.SLA_ALERT_RETRY_EXCHANGE,
                MqConstants.SLA_ALERT_RETRY_ROUTING_KEY,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties()
                            .setHeader(MqConstants.SLA_ALERT_ID_HEADER, message.getAlertId());
                    return amqpMessage;
                },
                correlationData
        );
    }

}
