package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// SlaTicketRemindRequest 属于智能客服平台基础代码。
public class SlaTicketRemindRequest {

    @NotBlank(message = "提醒类型不能为空")
    @Pattern(
            regexp = "FIRST_RESPONSE_RISK|FIRST_RESPONSE_OVERDUE|RESOLVE_RISK|RESOLVE_OVERDUE",
            message = "提醒类型不正确"
    )
    private String remindType;

    @NotNull(message = "接收人ID不能为空")
    private Long receiverId;

    @Size(max = 1000, message = "提醒内容最多 1000 个字符")
    private String content;

    public String getRemindType() {
        return remindType;
    }

    public void setRemindType(String remindType) {
        this.remindType = remindType;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
