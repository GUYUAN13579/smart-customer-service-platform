package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

public class KnowledgeChunkRebuildVO {

    private String sourceType;
    private Long sourceId;
    private Integer chunkCount;
    private LocalDateTime rebuiltAt;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public Integer getChunkCount() { return chunkCount; }
    public void setChunkCount(Integer chunkCount) { this.chunkCount = chunkCount; }
    public LocalDateTime getRebuiltAt() { return rebuiltAt; }
    public void setRebuiltAt(LocalDateTime rebuiltAt) { this.rebuiltAt = rebuiltAt; }
}
