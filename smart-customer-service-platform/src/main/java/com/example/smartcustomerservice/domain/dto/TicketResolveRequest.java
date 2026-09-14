package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

// TicketResolveRequest 属于智能客服平台基础代码。
public class TicketResolveRequest {

    @NotBlank(message = "解决方案不能为空")
    private String solution;

    @Min(value = 0, message = "客户可见标记只能是0或1")
    @Max(value = 1, message = "客户可见标记只能是0或1")
    private Integer visibleToCustomer = 1;

    public String getSolution() {
        return solution;
    }

    public void setSolution(String solution) {
        this.solution = solution;
    }

    public Integer getVisibleToCustomer() {
        return visibleToCustomer;
    }

    public void setVisibleToCustomer(Integer visibleToCustomer) {
        this.visibleToCustomer = visibleToCustomer;
    }
}
