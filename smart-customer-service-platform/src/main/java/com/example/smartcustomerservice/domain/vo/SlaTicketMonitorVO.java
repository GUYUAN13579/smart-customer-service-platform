package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

// SlaTicketMonitorVO 属于智能客服平台基础代码。
public class SlaTicketMonitorVO {

    private Long ticketId;
    private String ticketNo;
    private String title;
    private String category;
    private String priority;
    private String status;
    private Long assigneeId;
    private String riskType;
    private LocalDateTime deadlineAt;
    private Long remainingMinutes;
    private Long overdueMinutes;
    private LocalDateTime firstResponseDeadline;
    private LocalDateTime resolveDeadline;
    private LocalDateTime firstRespondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getTicketId() {
        return ticketId;
    }

    public void setTicketId(Long ticketId) {
        this.ticketId = ticketId;
    }

    public String getTicketNo() {
        return ticketNo;
    }

    public void setTicketNo(String ticketNo) {
        this.ticketNo = ticketNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public String getRiskType() {
        return riskType;
    }

    public void setRiskType(String riskType) {
        this.riskType = riskType;
    }

    public LocalDateTime getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(LocalDateTime deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public Long getRemainingMinutes() {
        return remainingMinutes;
    }

    public void setRemainingMinutes(Long remainingMinutes) {
        this.remainingMinutes = remainingMinutes;
    }

    public Long getOverdueMinutes() {
        return overdueMinutes;
    }

    public void setOverdueMinutes(Long overdueMinutes) {
        this.overdueMinutes = overdueMinutes;
    }

    public LocalDateTime getFirstResponseDeadline() {
        return firstResponseDeadline;
    }

    public void setFirstResponseDeadline(LocalDateTime firstResponseDeadline) {
        this.firstResponseDeadline = firstResponseDeadline;
    }

    public LocalDateTime getResolveDeadline() {
        return resolveDeadline;
    }

    public void setResolveDeadline(LocalDateTime resolveDeadline) {
        this.resolveDeadline = resolveDeadline;
    }

    public LocalDateTime getFirstRespondedAt() {
        return firstRespondedAt;
    }

    public void setFirstRespondedAt(LocalDateTime firstRespondedAt) {
        this.firstRespondedAt = firstRespondedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
