package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

// AiSessionSummaryVO 属于智能客服平台基础代码。
public class AiSessionSummaryVO {

    private Long sessionId;
    private String summary;
    private String customerProblem;
    private String customerEmotion;
    private String keyInformation;
    private String suggestedNextAction;
    private LocalDateTime createdAt;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getCustomerProblem() {
        return customerProblem;
    }

    public void setCustomerProblem(String customerProblem) {
        this.customerProblem = customerProblem;
    }

    public String getCustomerEmotion() {
        return customerEmotion;
    }

    public void setCustomerEmotion(String customerEmotion) {
        this.customerEmotion = customerEmotion;
    }

    public String getKeyInformation() {
        return keyInformation;
    }

    public void setKeyInformation(String keyInformation) {
        this.keyInformation = keyInformation;
    }

    public String getSuggestedNextAction() {
        return suggestedNextAction;
    }

    public void setSuggestedNextAction(String suggestedNextAction) {
        this.suggestedNextAction = suggestedNextAction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
