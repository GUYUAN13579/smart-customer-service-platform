package com.example.smartcustomerservice.listener.ticket;

import com.example.smartcustomerservice.event.ticket.TicketApprovedEvent;
import com.example.smartcustomerservice.service.ticket.TicketAutoAssignFailureService;
import com.example.smartcustomerservice.service.ticket.TicketService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class TicketApprovedAutoAssignListener {

    private final TicketService ticketService;
    private final TicketAutoAssignFailureService ticketAutoAssignFailureService;

    public TicketApprovedAutoAssignListener(TicketService ticketService,
                                            TicketAutoAssignFailureService ticketAutoAssignFailureService) {
        this.ticketService = ticketService;
        this.ticketAutoAssignFailureService = ticketAutoAssignFailureService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketApproved(TicketApprovedEvent event) {
        try {
            // 审核事务提交后再派单，派单失败不会影响审核通过结果。
            ticketService.autoAssignTicket(event.getTicketId());
        } catch (Exception exception) {
            ticketAutoAssignFailureService.recordFailure(event.getTicketId(), exception.getMessage());
        }
    }
}
