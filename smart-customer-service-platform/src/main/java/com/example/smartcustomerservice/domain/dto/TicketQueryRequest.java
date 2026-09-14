package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// TicketQueryRequest 属于智能客服平台基础代码。
public class TicketQueryRequest extends PageQuery {

    @Size(max = 128, message = "关键词最多128个字符")
    private String keyword;

    private Long customerId;

    private Long sessionId;

    @Pattern(regexp = "PENDING_REVIEW|WAITING_ASSIGN|ASSIGNED|PROCESSING|RESOLVED|CLOSED|REJECTED", message = "工单状态不合法")
    private String status;

    @Pattern(regexp = "PENDING_REVIEW|APPROVED|REJECTED", message = "审核状态不合法")
    private String reviewStatus;

    @Pattern(regexp = "P1|P2|P3|P4", message = "优先级只能是 P1、P2、P3 或 P4")
    private String priority;

    @Size(max = 64, message = "分类最多64个字符")
    private String category;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
