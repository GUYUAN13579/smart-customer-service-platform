package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class SkillGroupCreateRequest {

    @NotBlank(message = "技能组名称不能为空")
    @Size(max = 128, message = "技能组名称最多 128 位")
    private String groupName;

    @Size(max = 64, message = "工单分类最多 64 位")
    private String category;

    @Min(value = 0, message = "status 只能是 0 或 1")
    @Max(value = 1, message = "status 只能是 0 或 1")
    private Integer status = 1;

    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
