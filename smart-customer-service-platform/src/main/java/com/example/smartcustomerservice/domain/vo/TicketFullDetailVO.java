package com.example.smartcustomerservice.domain.vo;

import java.util.List;

// TicketFullDetailVO 属于智能客服平台基础代码。
public class TicketFullDetailVO {

    private TicketVO ticket;
    private List<TicketProcessRecordVO> processRecords;
    private List<TicketOperationLogVO> operationLogs;

    public TicketVO getTicket() {
        return ticket;
    }

    public void setTicket(TicketVO ticket) {
        this.ticket = ticket;
    }

    public List<TicketProcessRecordVO> getProcessRecords() {
        return processRecords;
    }

    public void setProcessRecords(List<TicketProcessRecordVO> processRecords) {
        this.processRecords = processRecords;
    }

    public List<TicketOperationLogVO> getOperationLogs() {
        return operationLogs;
    }

    public void setOperationLogs(List<TicketOperationLogVO> operationLogs) {
        this.operationLogs = operationLogs;
    }
}
