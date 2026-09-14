package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Size;

// TicketStartRequest 属于智能客服平台基础代码。
public class TicketStartRequest {

    @Size(max = 500, message = "处理说明最多500个字符")
    private String remark;

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
