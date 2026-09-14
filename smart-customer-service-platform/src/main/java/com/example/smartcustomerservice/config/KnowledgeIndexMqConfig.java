package com.example.smartcustomerservice.config;

import com.example.smartcustomerservice.common.constants.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * 知识索引异步任务的 RabbitMQ 基础设施。
 *
 * <p>生产者后续将 taskId 投递至 exchange，队列消费者再执行向量化和 Elasticsearch 写入。</p>
 */
@Configuration
public class KnowledgeIndexMqConfig {

    @Bean
    public DirectExchange knowledgeIndexExchange() {
        return new DirectExchange(MqConstants.KNOWLEDGE_INDEX_EXCHANGE, true, false);
    }

    @Bean
    public Queue knowledgeIndexQueue() {
        return QueueBuilder.durable(MqConstants.KNOWLEDGE_INDEX_QUEUE).build();
    }

    @Bean
    public Binding knowledgeIndexBinding() {
        return BindingBuilder.bind(knowledgeIndexQueue())
                .to(knowledgeIndexExchange())
                .with(MqConstants.KNOWLEDGE_INDEX_ROUTING_KEY);
    }

    @Bean
    public DirectExchange knowledgeIndexDelayExchange() {
        return new DirectExchange(MqConstants.KNOWLEDGE_INDEX_DELAY_EXCHANGE, true, false);
    }

    @Bean
    public Queue knowledgeIndexDelayQueue() {
        Map<String, Object> arguments = new HashMap<>();
        arguments.put("x-dead-letter-exchange", MqConstants.KNOWLEDGE_INDEX_EXCHANGE);
        arguments.put("x-dead-letter-routing-key", MqConstants.KNOWLEDGE_INDEX_ROUTING_KEY);
        return new Queue(MqConstants.KNOWLEDGE_INDEX_DELAY_QUEUE, true, false, false, arguments);
    }

    @Bean
    public Binding knowledgeIndexDelayBinding() {
        return BindingBuilder.bind(knowledgeIndexDelayQueue())
                .to(knowledgeIndexDelayExchange())
                .with(MqConstants.KNOWLEDGE_INDEX_DELAY_ROUTING_KEY);
    }
}
