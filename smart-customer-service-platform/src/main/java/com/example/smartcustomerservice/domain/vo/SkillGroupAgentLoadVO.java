package com.example.smartcustomerservice.domain.vo;

public class SkillGroupAgentLoadVO {

    private Long memberId;
    private Long userId;
    private String username;
    private String realName;
    private Integer maxActiveTickets;
    private Long activeTicketCount;

    public Long getMemberId() { return memberId; }
    public void setMemberId(Long memberId) { this.memberId = memberId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getMaxActiveTickets() { return maxActiveTickets; }
    public void setMaxActiveTickets(Integer maxActiveTickets) { this.maxActiveTickets = maxActiveTickets; }
    public Long getActiveTicketCount() { return activeTicketCount; }
    public void setActiveTicketCount(Long activeTicketCount) { this.activeTicketCount = activeTicketCount; }
}
