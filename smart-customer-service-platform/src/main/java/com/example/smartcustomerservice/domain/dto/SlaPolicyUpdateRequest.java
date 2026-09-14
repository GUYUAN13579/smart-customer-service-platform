package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// SlaPolicyUpdateRequest 属于智能客服平台基础代码。
public class SlaPolicyUpdateRequest {

    private Long id;

    @NotBlank(message = "SLA策略名称不能为空")
    @Size(max = 128, message = "SLA策略名称最多 128 位")
    private String policyName;

    @Size(max = 64, message = "工单分类最多 64 位")
    private String category;

    @NotBlank(message = "优先级不能为空")
    @Size(max = 16, message = "优先级最多 16 位")
    private String priority;

    @NotNull(message = "首次响应时限不能为空")
    @Min(value = 1, message = "首次响应时限不能小于 1 分钟")
    private Integer firstResponseMinutes;

    @NotNull(message = "解决时限不能为空")
    @Min(value = 1, message = "解决时限不能小于 1 分钟")
    private Integer resolveMinutes;

    @Min(value = 0, message = "enabled 只能是 0 或 1")
    @Max(value = 1, message = "enabled 只能是 0 或 1")
    private Integer enabled = 1;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
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

    public Integer getFirstResponseMinutes() {
        return firstResponseMinutes;
    }

    public void setFirstResponseMinutes(Integer firstResponseMinutes) {
        this.firstResponseMinutes = firstResponseMinutes;
    }

    public Integer getResolveMinutes() {
        return resolveMinutes;
    }

    public void setResolveMinutes(Integer resolveMinutes) {
        this.resolveMinutes = resolveMinutes;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
