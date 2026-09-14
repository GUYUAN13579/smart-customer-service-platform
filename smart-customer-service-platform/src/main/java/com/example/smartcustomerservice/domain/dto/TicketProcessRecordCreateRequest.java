package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

// TicketProcessRecordCreateRequest 属于智能客服平台基础代码。
public class TicketProcessRecordCreateRequest {

    @Pattern(regexp = "NOTE|CONTACT_CUSTOMER|PROCESS|WAITING|OTHER", message = "处理记录类型不合法")
    private String recordType = "NOTE";

    @NotBlank(message = "处理内容不能为空")
    private String content;

    @Min(value = 0, message = "客户可见标记只能是0或1")
    @Max(value = 1, message = "客户可见标记只能是0或1")
    private Integer visibleToCustomer = 0;

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getVisibleToCustomer() {
        return visibleToCustomer;
    }

    public void setVisibleToCustomer(Integer visibleToCustomer) {
        this.visibleToCustomer = visibleToCustomer;
    }
}
