package com.example.smartcustomerservice.common.constants;

public final class MqConstants {

    public static final String TICKET_CREATE_QUEUE = "ticket.create";
    public static final String TICKET_ASSIGN_QUEUE = "ticket.assign";
    public static final String TICKET_SLA_DELAY_QUEUE = "ticket.sla.delay";
    public static final String TICKET_ESCALATE_QUEUE = "ticket.escalate";
    public static final String NOTIFICATION_SEND_QUEUE = "notification.send";

    private MqConstants() {
    }
}
