package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Size;

// AiSessionSummaryRequest 属于智能客服平台基础代码。
public class AiSessionSummaryRequest {

    @Size(max = 2000, message = "额外提示词长度不能超过2000个字符")
    private String extraPrompt;

    public String getExtraPrompt() {
        return extraPrompt;
    }

    public void setExtraPrompt(String extraPrompt) {
        this.extraPrompt = extraPrompt;
    }
}
