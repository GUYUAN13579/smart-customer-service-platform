package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AssignmentRuleCreateRequest {

    @NotBlank(message = "规则名称不能为空")
    @Size(max = 128, message = "规则名称最多 128 位")
    private String ruleName;

    @Size(max = 64, message = "分类最多 64 位")
    private String category;

    @Size(max = 16, message = "优先级最多 16 位")
    private String priority;

    @NotNull(message = "技能组ID不能为空")
    private Long skillGroupId;

    @Min(value = 0, message = "规则权重不能小于 0")
    private Integer ruleWeight = 0;

    @Min(value = 0, message = "enabled 只能是 0 或 1")
    @Max(value = 1, message = "enabled 只能是 0 或 1")
    private Integer enabled = 1;

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public Long getSkillGroupId() {
        return skillGroupId;
    }

    public void setSkillGroupId(Long skillGroupId) {
        this.skillGroupId = skillGroupId;
    }

    public Integer getRuleWeight() {
        return ruleWeight;
    }

    public void setRuleWeight(Integer ruleWeight) {
        this.ruleWeight = ruleWeight;
    }

    public Integer getEnabled() {
        return enabled;
    }

    public void setEnabled(Integer enabled) {
        this.enabled = enabled;
    }
}
