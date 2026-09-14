package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

// TicketProcessRecordVO 属于智能客服平台基础代码。
public class TicketProcessRecordVO {

    private Long id;
    private Long ticketId;
    private Long operatorId;
    private String recordType;
    private String content;
    private Integer visibleToCustomer;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getVisibleToCustomer() {
        return visibleToCustomer;
    }

    public void setVisibleToCustomer(Integer visibleToCustomer) {
        this.visibleToCustomer = visibleToCustomer;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
