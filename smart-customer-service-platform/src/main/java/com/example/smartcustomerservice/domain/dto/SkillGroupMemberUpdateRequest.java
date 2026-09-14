package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class SkillGroupMemberUpdateRequest {

    @Min(value = 1, message = "最大处理中工单数至少为 1")
    @Max(value = 1000, message = "最大处理中工单数不能超过 1000")
    private Integer maxActiveTickets;

    @Min(value = 0, message = "status 只能是 0 或 1")
    @Max(value = 1, message = "status 只能是 0 或 1")
    private Integer status;

    public Integer getMaxActiveTickets() { return maxActiveTickets; }
    public void setMaxActiveTickets(Integer maxActiveTickets) { this.maxActiveTickets = maxActiveTickets; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
