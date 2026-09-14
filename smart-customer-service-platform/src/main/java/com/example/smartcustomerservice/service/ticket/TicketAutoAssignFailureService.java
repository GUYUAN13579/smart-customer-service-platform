package com.example.smartcustomerservice.service.ticket;

public interface TicketAutoAssignFailureService {

    void recordFailure(Long ticketId, String failureReason);
}
