package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class AssignmentRuleQueryRequest extends PageQuery {

    @Size(max = 128, message = "关键词最多 128 位")
    private String keyword;

    @Size(max = 64, message = "分类最多 64 位")
    private String category;

    @Size(max = 16, message = "优先级最多 16 位")
    private String priority;

    @Min(value = 0, message = "enabled 只能是 0 或 1")
    @Max(value = 1, message = "enabled 只能是 0 或 1")
    private Integer enabled;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
