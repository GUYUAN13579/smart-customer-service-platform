package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;

// NotificationQueryRequest 属于智能客服平台基础代码。
public class NotificationQueryRequest extends PageQuery {

    @Pattern(regexp = "UNREAD|READ", message = "读取状态只能是 UNREAD 或 READ")
    private String readStatus;

    @Pattern(regexp = "SLA_RISK|SLA_OVERDUE|TICKET_ASSIGNED|SYSTEM", message = "通知类型不正确")
    private String type;

    @Pattern(regexp = "TICKET|SLA|CONVERSATION|SYSTEM", message = "业务类型不正确")
    private String businessType;

    public String getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(String readStatus) {
        this.readStatus = readStatus;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }
}
