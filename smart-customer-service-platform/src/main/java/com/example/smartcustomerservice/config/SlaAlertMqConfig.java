package com.example.smartcustomerservice.config;

import com.example.smartcustomerservice.common.constants.MqConstants;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@EnableRabbit
@Configuration
// SlaAlertMqConfig 属于智能客服平台基础代码。
public class SlaAlertMqConfig {

    // TODO: 声明 SLA 延迟交换机、延迟队列、死信交换机、消费队列及其绑定关系。
    @Bean
    public Queue delayQueue() {
        // 延迟队列只负责等待消息 TTL 到期，过期后将消息转交给死信交换机。
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstants.SLA_ALERT_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstants.SLA_ALERT_TRIGGER_ROUTING_KEY);

        return new Queue(MqConstants.SLA_ALERT_DELAY_QUEUE, true, false, false, args);
    }

    @Bean
    public DirectExchange slaAlertDelayExchange() {
        return new DirectExchange(
                MqConstants.SLA_ALERT_DELAY_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public DirectExchange slaAlertRetryExchange(){
        return new DirectExchange(
                MqConstants.SLA_ALERT_RETRY_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Binding slaAlertRetryBinding(){
        // 重试消息不需要等待 TTL，直接路由到与正常告警共用的实际消费队列。
        return BindingBuilder.bind(slaAlertTriggerQueue())
                .to(slaAlertRetryExchange())
                .with(MqConstants.SLA_ALERT_RETRY_ROUTING_KEY);
    }

    @Bean
    public Binding slaAlertDelayBinding() {
        return BindingBuilder.bind(delayQueue())
                .to(slaAlertDelayExchange())
                .with(MqConstants.SLA_ALERT_DELAY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange slaAlertDeadLetterExchange(){
        return new DirectExchange(
                MqConstants.SLA_ALERT_DEAD_LETTER_EXCHANGE,
                true,
                false
        );
    }

    @Bean
    public Queue slaAlertTriggerQueue() {
        // 主消费者最终拒绝消息时，将其转入失败队列，避免无限 requeue 占满正常消费队列。
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", MqConstants.SLA_ALERT_FAILURE_EXCHANGE);
        args.put("x-dead-letter-routing-key", MqConstants.SLA_ALERT_FAILURE_ROUTING_KEY);
        return new Queue(MqConstants.SLA_ALERT_TRIGGER_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding slaAlertTriggerBinding() {
        // 正常消费队列只接收已到期的告警消息，消费者无需自行计算延迟。
        return BindingBuilder.bind(slaAlertTriggerQueue())
                .to(slaAlertDeadLetterExchange())
                .with(MqConstants.SLA_ALERT_TRIGGER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange slaAlertFailureExchange() {
        return new DirectExchange(MqConstants.SLA_ALERT_FAILURE_EXCHANGE, true, false);
    }

    @Bean
    public Queue slaAlertFailureQueue() {
        // 失败队列不再配置死信转发；失败消费者负责记录 FAILED 后正常确认消息，阻止循环。
        return QueueBuilder.durable(MqConstants.SLA_ALERT_FAILURE_QUEUE).build();
    }

    @Bean
    public Binding slaAlertFailureBinding() {
        return BindingBuilder.bind(slaAlertFailureQueue())
                .to(slaAlertFailureExchange())
                .with(MqConstants.SLA_ALERT_FAILURE_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
