package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class ConversationMessageCreateRequest {

    @NotBlank(message = "发送方类型不能为空")
    @Pattern(regexp = "CUSTOMER|AI|AGENT|SYSTEM", message = "发送方类型只能是 CUSTOMER、AI、AGENT 或 SYSTEM")
    private String senderType;

    private Long senderId;

    @NotBlank(message = "消息内容不能为空")
    @Size(max = 5000, message = "消息内容最多 5000 位")
    private String content;

    @Pattern(regexp = "TEXT|IMAGE|FILE|SYSTEM", message = "消息类型只能是 TEXT、IMAGE、FILE 或 SYSTEM")
    private String messageType = "TEXT";

    @Size(max = 64, message = "意图最多 64 位")
    private String intent;

    private BigDecimal confidence;

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public Long getSenderId() {
        return senderId;
    }

    public void setSenderId(Long senderId) {
        this.senderId = senderId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public BigDecimal getConfidence() {
        return confidence;
    }

    public void setConfidence(BigDecimal confidence) {
        this.confidence = confidence;
    }
}
