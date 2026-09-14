package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

public class SkillGroupMemberVO {

    private Long id;
    private Long skillGroupId;
    private String skillGroupName;
    private String skillGroupCategory;
    private Long userId;
    private String username;
    private String realName;
    private Integer maxActiveTickets;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillGroupId() { return skillGroupId; }
    public void setSkillGroupId(Long skillGroupId) { this.skillGroupId = skillGroupId; }
    public String getSkillGroupName() { return skillGroupName; }
    public void setSkillGroupName(String skillGroupName) { this.skillGroupName = skillGroupName; }
    public String getSkillGroupCategory() { return skillGroupCategory; }
    public void setSkillGroupCategory(String skillGroupCategory) { this.skillGroupCategory = skillGroupCategory; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Integer getMaxActiveTickets() { return maxActiveTickets; }
    public void setMaxActiveTickets(Integer maxActiveTickets) { this.maxActiveTickets = maxActiveTickets; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
