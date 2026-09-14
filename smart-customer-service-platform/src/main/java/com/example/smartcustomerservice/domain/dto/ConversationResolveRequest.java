package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Size;

// ConversationResolveRequest 用于客服在会话中确认客户问题已解决。
public class ConversationResolveRequest {

    @Size(max = 2000, message = "解决说明最多 2000 位")
    private String solution;

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }
}
