package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class KnowledgeIndexTaskQueryRequest extends PageQuery {

    @Pattern(regexp = "ARTICLE|DOCUMENT", message = "来源类型只能是 ARTICLE 或 DOCUMENT")
    private String sourceType;

    @Min(value = 1, message = "来源ID必须大于0")
    private Long sourceId;

    @Pattern(regexp = "PARSE|CHUNK|EMBED|INDEX|DELETE", message = "任务类型不合法")
    private String taskType;

    @Pattern(regexp = "PENDING|PROCESSING|SUCCESS|FAILED|CANCELLED", message = "任务状态不合法")
    private String status;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
