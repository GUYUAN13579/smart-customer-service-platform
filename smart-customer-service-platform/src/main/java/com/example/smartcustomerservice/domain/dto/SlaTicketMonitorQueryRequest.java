package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// SlaTicketMonitorQueryRequest 属于智能客服平台基础代码。
public class SlaTicketMonitorQueryRequest extends PageQuery {

    @Pattern(regexp = "FIRST_RESPONSE|RESOLVE", message = "SLA类型只能是 FIRST_RESPONSE 或 RESOLVE")
    private String type;

    @Min(value = 1, message = "即将超时时间窗口不能小于 1 分钟")
    @Max(value = 10080, message = "即将超时时间窗口最多 10080 分钟")
    private Integer withinMinutes = 30;

    @Pattern(regexp = "P1|P2|P3|P4", message = "优先级只能是 P1、P2、P3 或 P4")
    private String priority;

    @Size(max = 64, message = "分类最多 64 个字符")
    private String category;

    private Long assigneeId;

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getWithinMinutes() {
        return withinMinutes;
    }

    public void setWithinMinutes(Integer withinMinutes) {
        this.withinMinutes = withinMinutes;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }
}
