package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;

// TicketOperationLogQueryRequest 属于智能客服平台基础代码。
public class TicketOperationLogQueryRequest extends PageQuery {

    @Pattern(regexp = "CREATE|REVIEW_APPROVE|REVIEW_REJECT|ASSIGN|START|ADD_RECORD|RESOLVE|CLOSE", message = "操作类型不合法")
    private String operationType;

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }
}
