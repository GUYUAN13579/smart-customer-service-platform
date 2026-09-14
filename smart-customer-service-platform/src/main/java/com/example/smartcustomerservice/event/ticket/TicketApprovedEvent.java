package com.example.smartcustomerservice.event.ticket;

public class TicketApprovedEvent {

    private final Long ticketId;

    public TicketApprovedEvent(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getTicketId() {
        return ticketId;
    }
}
