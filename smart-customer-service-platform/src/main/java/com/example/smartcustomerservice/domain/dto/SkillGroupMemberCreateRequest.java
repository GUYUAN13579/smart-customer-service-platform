package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class SkillGroupMemberCreateRequest {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @Min(value = 1, message = "最大处理中工单数至少为 1")
    @Max(value = 1000, message = "最大处理中工单数不能超过 1000")
    private Integer maxActiveTickets = 20;

    @Min(value = 0, message = "status 只能是 0 或 1")
    @Max(value = 1, message = "status 只能是 0 或 1")
    private Integer status = 1;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getMaxActiveTickets() { return maxActiveTickets; }
    public void setMaxActiveTickets(Integer maxActiveTickets) { this.maxActiveTickets = maxActiveTickets; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
