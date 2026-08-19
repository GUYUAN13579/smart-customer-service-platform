package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class ConversationCreateRequest {

    @NotNull(message = "客户ID不能为空")
    private Long customerId;

    @NotBlank(message = "渠道不能为空")
    @Pattern(regexp = "WEB|APP|WECHAT|PHONE|EMAIL", message = "渠道只能是 WEB、APP、WECHAT、PHONE 或 EMAIL")
    private String channel;

    @Min(value = 0, message = "aiEnabled 只能是 0 或 1")
    @Max(value = 1, message = "aiEnabled 只能是 0 或 1")
    private Integer aiEnabled = 1;

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

    public Integer getAiEnabled() {
        return aiEnabled;
    }

    public void setAiEnabled(Integer aiEnabled) {
        this.aiEnabled = aiEnabled;
    }
}
