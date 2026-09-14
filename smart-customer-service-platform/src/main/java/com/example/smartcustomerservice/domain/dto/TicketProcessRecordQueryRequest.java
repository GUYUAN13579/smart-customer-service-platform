package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;

// TicketProcessRecordQueryRequest 属于智能客服平台基础代码。
public class TicketProcessRecordQueryRequest extends PageQuery {

    @Pattern(regexp = "START|NOTE|CONTACT_CUSTOMER|PROCESS|WAITING|RESOLVE|CLOSE|OTHER", message = "处理记录类型不合法")
    private String recordType;

    private Integer visibleToCustomer;

    public String getRecordType() {
        return recordType;
    }

    public void setRecordType(String recordType) {
        this.recordType = recordType;
    }

    public Integer getVisibleToCustomer() {
        return visibleToCustomer;
    }

    public void setVisibleToCustomer(Integer visibleToCustomer) {
        this.visibleToCustomer = visibleToCustomer;
    }
}
