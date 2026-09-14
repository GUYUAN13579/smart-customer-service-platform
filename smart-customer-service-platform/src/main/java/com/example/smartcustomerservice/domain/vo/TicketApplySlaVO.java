package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

// TicketApplySlaVO 属于智能客服平台基础代码。
public class TicketApplySlaVO {

    private Long ticketId;
    private String ticketNo;
    private Long slaPolicyId;
    private String policyName;
    private String category;
    private String priority;
    private Integer firstResponseMinutes;
    private Integer resolveMinutes;
    private LocalDateTime baseTime;
    private LocalDateTime firstResponseDeadline;
    private LocalDateTime resolveDeadline;

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

    public Long getSlaPolicyId() {
        return slaPolicyId;
    }

    public void setSlaPolicyId(Long slaPolicyId) {
        this.slaPolicyId = slaPolicyId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
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

    public Integer getFirstResponseMinutes() {
        return firstResponseMinutes;
    }

    public void setFirstResponseMinutes(Integer firstResponseMinutes) {
        this.firstResponseMinutes = firstResponseMinutes;
    }

    public Integer getResolveMinutes() {
        return resolveMinutes;
    }

    public void setResolveMinutes(Integer resolveMinutes) {
        this.resolveMinutes = resolveMinutes;
    }

    public LocalDateTime getBaseTime() {
        return baseTime;
    }

    public void setBaseTime(LocalDateTime baseTime) {
        this.baseTime = baseTime;
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
}
