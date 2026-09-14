package com.example.smartcustomerservice.domain.dto;

// ConversationTakeOverRequest 属于智能客服平台基础代码。
public class ConversationTakeOverRequest {

    private Long currentAgentId;

    public Long getCurrentAgentId() {
        return currentAgentId;
    }

    public void setCurrentAgentId(Long currentAgentId) {
        this.currentAgentId = currentAgentId;
    }
}
