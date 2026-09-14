package com.example.smartcustomerservice.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("skill_group_member")
public class SkillGroupMember {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long skillGroupId;
    private Long userId;
    private Integer maxActiveTickets;
    private Integer status;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSkillGroupId() { return skillGroupId; }
    public void setSkillGroupId(Long skillGroupId) { this.skillGroupId = skillGroupId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getMaxActiveTickets() { return maxActiveTickets; }
    public void setMaxActiveTickets(Integer maxActiveTickets) { this.maxActiveTickets = maxActiveTickets; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
