package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public class KnowledgeChunkQueryRequest extends PageQuery {

    @Pattern(regexp = "ARTICLE|DOCUMENT", message = "来源类型只能是 ARTICLE 或 DOCUMENT")
    private String sourceType;

    @Min(value = 1, message = "来源ID必须大于0")
    private Long sourceId;

    @Pattern(regexp = "PENDING|PROCESSING|INDEXED|FAILED|OFFLINE", message = "索引状态不合法")
    private String indexStatus;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getIndexStatus() { return indexStatus; }
    public void setIndexStatus(String indexStatus) { this.indexStatus = indexStatus; }
}
