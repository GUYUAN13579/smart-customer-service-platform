package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// TicketCloseRequest 属于智能客服平台基础代码。
public class TicketCloseRequest {

    @NotBlank(message = "关闭原因不能为空")
    @Size(max = 1000, message = "关闭原因最多1000个字符")
    private String closeReason;

    public String getCloseReason() {
        return closeReason;
    }

    public void setCloseReason(String closeReason) {
        this.closeReason = closeReason;
    }
}
