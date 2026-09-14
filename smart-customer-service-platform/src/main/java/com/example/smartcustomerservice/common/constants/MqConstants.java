package com.example.smartcustomerservice.common.constants;

// MqConstants 属于智能客服平台基础代码。
public final class MqConstants {

    public static final String TICKET_CREATE_QUEUE = "ticket.create";
    public static final String TICKET_ASSIGN_QUEUE = "ticket.assign";
    public static final String TICKET_SLA_DELAY_QUEUE = "ticket.sla.delay";
    public static final String TICKET_ESCALATE_QUEUE = "ticket.escalate";
    public static final String NOTIFICATION_SEND_QUEUE = "notification.send";

    // 知识索引任务：创建或重试任务后投递 taskId，由消费者异步完成 Embedding 与 ES 写入。
    public static final String KNOWLEDGE_INDEX_EXCHANGE = "knowledge.index.exchange";
    public static final String KNOWLEDGE_INDEX_QUEUE = "knowledge.index.queue";
    public static final String KNOWLEDGE_INDEX_ROUTING_KEY = "knowledge.index.execute";
    public static final String KNOWLEDGE_INDEX_TASK_ID_HEADER = "x-knowledge-index-task-id";
    public static final String KNOWLEDGE_INDEX_CORRELATION_PREFIX = "knowledge-index:";
    public static final String KNOWLEDGE_INDEX_DELAY_EXCHANGE = "knowledge.index.delay.exchange";
    public static final String KNOWLEDGE_INDEX_DELAY_QUEUE = "knowledge.index.delay.queue";
    public static final String KNOWLEDGE_INDEX_DELAY_ROUTING_KEY = "knowledge.index.delay";

    public static final String SLA_ALERT_DELAY_EXCHANGE = "sla.alert.delay.exchange";
    public static final String SLA_ALERT_DELAY_QUEUE = "sla.alert.delay.queue";
    public static final String SLA_ALERT_DELAY_ROUTING_KEY = "sla.alert.delay";
    public static final String SLA_ALERT_DEAD_LETTER_EXCHANGE = "sla.alert.dead-letter.exchange";
    public static final String SLA_ALERT_TRIGGER_QUEUE = "sla.alert.trigger.queue";
    public static final String SLA_ALERT_TRIGGER_ROUTING_KEY = "sla.alert.trigger";
    public static final String SLA_ALERT_RETRY_ROUTING_KEY = "sla.alert.retry";
    public static final String SLA_ALERT_ID_HEADER = "x-sla-alert-id";
    public static final String SLA_ALERT_CORRELATION_PREFIX = "sla-alert:";
    public static final String SLA_ALERT_RETRY_EXCHANGE = "sla.alert.retry.exchange";
    // 主消费者达到最大重试次数后，消息由触发队列死信到这一组资源。
    public static final String SLA_ALERT_FAILURE_EXCHANGE = "sla.alert.failure.exchange";
    public static final String SLA_ALERT_FAILURE_QUEUE = "sla.alert.failure.queue";
    public static final String SLA_ALERT_FAILURE_ROUTING_KEY = "sla.alert.failure";

    private MqConstants() {
    }
}
