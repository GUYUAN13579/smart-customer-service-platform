package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

public class ConversationSessionVO {

    private Long id;
    private String sessionNo;
    private Long customerId;
    private String channel;
    private String status;
    private Integer aiEnabled;
    private Long currentAgentId;
    private LocalDateTime lastMessageAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Integer aiEnabled) {
        this.aiEnabled = aiEnabled;
    }

    public Long getCurrentAgentId() {
        return currentAgentId;
    }

    public void setCurrentAgentId(Long currentAgentId) {
        this.currentAgentId = currentAgentId;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
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
