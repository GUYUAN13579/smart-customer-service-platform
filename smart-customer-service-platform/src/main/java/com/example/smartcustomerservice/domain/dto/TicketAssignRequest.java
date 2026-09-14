package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// TicketAssignRequest 属于智能客服平台基础代码。
public class TicketAssignRequest {

    @NotNull(message = "处理客服ID不能为空")
    private Long assigneeId;

    private Long skillGroupId;

    @Size(max = 500, message = "派单备注最多500个字符")
    private String assignRemark;

    public Long getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getSkillGroupId() {
        return skillGroupId;
    }

    public void setSkillGroupId(Long skillGroupId) {
        this.skillGroupId = skillGroupId;
    }

    public String getAssignRemark() {
        return assignRemark;
    }

    public void setAssignRemark(String assignRemark) {
        this.assignRemark = assignRemark;
    }
}
