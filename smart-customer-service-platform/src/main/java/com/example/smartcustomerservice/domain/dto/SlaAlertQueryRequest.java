package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDateTime;

// SlaAlertQueryRequest 属于智能客服平台基础代码。
public class SlaAlertQueryRequest extends PageQuery {

    private Long ticketId;

    @Pattern(regexp = "FIRST_RESPONSE|RESOLVE", message = "告警类型只能是 FIRST_RESPONSE 或 RESOLVE")
    private String alertType;

    @Pattern(regexp = "RISK|OVERDUE", message = "告警级别只能是 RISK 或 OVERDUE")
    private String alertLevel;

    @Pattern(regexp = "PENDING|PROCESSING|SENT|FAILED|SKIPPED", message = "告警状态不正确")
    private String status;

    private LocalDateTime scheduledStartAt;
    private LocalDateTime scheduledEndAt;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(String alertType) {
        this.alertType = alertType;
    }

    public String getAlertLevel() {
        return alertLevel;
    }

    public void setAlertLevel(String alertLevel) {
        this.alertLevel = alertLevel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getScheduledStartAt() {
        return scheduledStartAt;
    }

    public void setScheduledStartAt(LocalDateTime scheduledStartAt) {
        this.scheduledStartAt = scheduledStartAt;
    }

    public LocalDateTime getScheduledEndAt() {
        return scheduledEndAt;
    }

    public void setScheduledEndAt(LocalDateTime scheduledEndAt) {
        this.scheduledEndAt = scheduledEndAt;
    }
}
