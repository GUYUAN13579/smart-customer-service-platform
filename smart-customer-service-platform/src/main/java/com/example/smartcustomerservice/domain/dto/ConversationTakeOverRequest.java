package com.example.smartcustomerservice.domain.dto;

public class ConversationTakeOverRequest {

    private Long currentAgentId;

    public Long getCurrentAgentId() {
        return currentAgentId;
    }

    public void setCurrentAgentId(Long currentAgentId) {
        this.currentAgentId = currentAgentId;
    }
}
