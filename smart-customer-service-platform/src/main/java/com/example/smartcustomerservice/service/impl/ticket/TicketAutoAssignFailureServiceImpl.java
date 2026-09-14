package com.example.smartcustomerservice.service.impl.ticket;

import com.example.smartcustomerservice.domain.entity.Ticket;
import com.example.smartcustomerservice.domain.entity.TicketOperationLog;
import com.example.smartcustomerservice.mapper.ticket.TicketMapper;
import com.example.smartcustomerservice.mapper.ticket.TicketOperationLogMapper;
import com.example.smartcustomerservice.service.ticket.TicketAutoAssignFailureService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TicketAutoAssignFailureServiceImpl implements TicketAutoAssignFailureService {

    private final TicketMapper ticketMapper;
    private final TicketOperationLogMapper ticketOperationLogMapper;

    public TicketAutoAssignFailureServiceImpl(TicketMapper ticketMapper,
                                              TicketOperationLogMapper ticketOperationLogMapper) {
        this.ticketMapper = ticketMapper;
        this.ticketOperationLogMapper = ticketOperationLogMapper;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void recordFailure(Long ticketId, String failureReason) {
        Ticket ticket = ticketMapper.selectById(ticketId);
        if (ticket == null || Integer.valueOf(1).equals(ticket.getDeleted())
                || !"APPROVED".equals(ticket.getReviewStatus())
                || !"WAITING_ASSIGN".equals(ticket.getStatus())) {
            return;
        }

        TicketOperationLog operationLog = new TicketOperationLog();
        operationLog.setTicketId(ticketId);
        operationLog.setOperationType("AUTO_ASSIGN_FAILED");
        operationLog.setFromStatus("WAITING_ASSIGN");
        operationLog.setToStatus("WAITING_ASSIGN");
        operationLog.setOperationContent("系统自动派单失败，等待人工派单：" + truncate(failureReason));
        operationLog.setCreatedAt(LocalDateTime.now());
        operationLog.setDeleted(0);
        ticketOperationLogMapper.insert(operationLog);
    }

    private String truncate(String failureReason) {
        String reason = failureReason == null ? "未知原因" : failureReason;
        return reason.length() <= 950 ? reason : reason.substring(0, 950);
    }
}
