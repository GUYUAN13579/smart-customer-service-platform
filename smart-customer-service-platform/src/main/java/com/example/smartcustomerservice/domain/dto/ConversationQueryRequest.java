package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class ConversationQueryRequest extends PageQuery {

    @Size(max = 128, message = "关键词最多 128 位")
    private String keyword;

    @Pattern(regexp = "ACTIVE|TAKEN_OVER|CLOSED", message = "会话状态只能是 ACTIVE、TAKEN_OVER 或 CLOSED")
    private String status;

    @Pattern(regexp = "WEB|APP|WECHAT|PHONE|EMAIL", message = "渠道只能是 WEB、APP、WECHAT、PHONE 或 EMAIL")
    private String channel;

    private Long customerId;

    private Long currentAgentId;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getCustomerId() {
        return customerId;
    }

    public void setCustomerId(Long customerId) {
        this.customerId = customerId;
    }

    public Long getCurrentAgentId() {
        return currentAgentId;
    }

    public void setCurrentAgentId(Long currentAgentId) {
        this.currentAgentId = currentAgentId;
    }
}
