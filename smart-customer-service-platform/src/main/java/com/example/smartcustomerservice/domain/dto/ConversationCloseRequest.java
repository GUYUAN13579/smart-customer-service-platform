package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Size;

// ConversationCloseRequest 属于智能客服平台基础代码。
public class ConversationCloseRequest {

    @Size(max = 255, message = "关闭原因最多 255 位")
    private String closeReason;

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }
}
