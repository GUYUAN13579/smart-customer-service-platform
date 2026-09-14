package com.example.smartcustomerservice.domain.vo;

/** 客服在某个技能组内的实时工单负载。 */
public class AgentWorkloadStatisticsVO {

    private Long skillGroupMemberId;
    private Long skillGroupId;
    private String skillGroupName;
    private Long userId;
    private String username;
    private String realName;
    private Integer memberStatus;
    private Integer maxActiveTickets;
    private Long assignedTicketCount;
    private Long processingTicketCount;
    private Long activeTicketCount;
    private Double workloadRate;
    private Boolean availableForAssign;

    public Long getSkillGroupMemberId() { return skillGroupMemberId; }
    public void setSkillGroupMemberId(Long skillGroupMemberId) { this.skillGroupMemberId = skillGroupMemberId; }
    public Long getSkillGroupId() { return skillGroupId; }
    public void setSkillGroupId(Long skillGroupId) { this.skillGroupId = skillGroupId; }
    public String getSkillGroupName() { return skillGroupName; }
    public void setSkillGroupName(String skillGroupName) { this.skillGroupName = skillGroupName; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getMemberStatus() { return memberStatus; }
    public void setMemberStatus(Integer memberStatus) { this.memberStatus = memberStatus; }
    public Integer getMaxActiveTickets() { return maxActiveTickets; }
    public void setMaxActiveTickets(Integer maxActiveTickets) { this.maxActiveTickets = maxActiveTickets; }
    public Long getAssignedTicketCount() { return assignedTicketCount; }
    public void setAssignedTicketCount(Long assignedTicketCount) { this.assignedTicketCount = assignedTicketCount; }
    public Long getProcessingTicketCount() { return processingTicketCount; }
    public void setProcessingTicketCount(Long processingTicketCount) { this.processingTicketCount = processingTicketCount; }
    public Long getActiveTicketCount() { return activeTicketCount; }
    public void setActiveTicketCount(Long activeTicketCount) { this.activeTicketCount = activeTicketCount; }
    public Double getWorkloadRate() { return workloadRate; }
    public void setWorkloadRate(Double workloadRate) { this.workloadRate = workloadRate; }
    public Boolean getAvailableForAssign() { return availableForAssign; }
    public void setAvailableForAssign(Boolean availableForAssign) { this.availableForAssign = availableForAssign; }
}
