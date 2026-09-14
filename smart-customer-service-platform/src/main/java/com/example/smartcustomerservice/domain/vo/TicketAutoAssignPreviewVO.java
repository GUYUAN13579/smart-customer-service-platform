package com.example.smartcustomerservice.domain.vo;

import java.util.List;

public class TicketAutoAssignPreviewVO {

    private Long ticketId;
    private String ticketNo;
    private String category;
    private String priority;
    private Long assignmentRuleId;
    private String assignmentRuleName;
    private Integer ruleWeight;
    private Long skillGroupId;
    private String skillGroupName;
    private Long selectedAgentId;
    private String selectedAgentName;
    private String unavailableReason;
    private List<SkillGroupAgentLoadVO> candidates;

    public Long getTicketId() { return ticketId; }
    public void setTicketId(Long ticketId) { this.ticketId = ticketId; }
    public String getTicketNo() { return ticketNo; }
    public void setTicketNo(String ticketNo) { this.ticketNo = ticketNo; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }
    public Long getAssignmentRuleId() { return assignmentRuleId; }
    public void setAssignmentRuleId(Long assignmentRuleId) { this.assignmentRuleId = assignmentRuleId; }
    public String getAssignmentRuleName() { return assignmentRuleName; }
    public void setAssignmentRuleName(String assignmentRuleName) { this.assignmentRuleName = assignmentRuleName; }
    public Integer getRuleWeight() { return ruleWeight; }
    public void setRuleWeight(Integer ruleWeight) { this.ruleWeight = ruleWeight; }
    public Long getSkillGroupId() { return skillGroupId; }
    public void setSkillGroupId(Long skillGroupId) { this.skillGroupId = skillGroupId; }
    public String getSkillGroupName() { return skillGroupName; }
    public void setSkillGroupName(String skillGroupName) { this.skillGroupName = skillGroupName; }
    public Long getSelectedAgentId() { return selectedAgentId; }
    public void setSelectedAgentId(Long selectedAgentId) { this.selectedAgentId = selectedAgentId; }
    public String getSelectedAgentName() { return selectedAgentName; }
    public void setSelectedAgentName(String selectedAgentName) { this.selectedAgentName = selectedAgentName; }
    public String getUnavailableReason() { return unavailableReason; }
    public void setUnavailableReason(String unavailableReason) { this.unavailableReason = unavailableReason; }
    public List<SkillGroupAgentLoadVO> getCandidates() { return candidates; }
    public void setCandidates(List<SkillGroupAgentLoadVO> candidates) { this.candidates = candidates; }
}
