package com.example.smartcustomerservice.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("assignment_rule")
public class AssignmentRule {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String ruleName;
    private String category;
    private String priority;
    private Long skillGroupId;
    private Integer ruleWeight;
    private Integer enabled;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
