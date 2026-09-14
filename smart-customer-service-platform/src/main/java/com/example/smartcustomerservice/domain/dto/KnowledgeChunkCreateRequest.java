package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class KnowledgeChunkCreateRequest {

    @NotBlank(message = "来源类型不能为空")
    @Pattern(regexp = "ARTICLE|DOCUMENT", message = "来源类型只能是 ARTICLE 或 DOCUMENT")
    private String sourceType;

    @NotNull(message = "来源ID不能为空")
    @Min(value = 1, message = "来源ID必须大于0")
    private Long sourceId;

    @NotBlank(message = "切片内容不能为空")
    private String content;

    @Size(max = 5000, message = "切片元数据最多5000位")
    private String metadata;

    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getMetadata() { return metadata; }
    public void setMetadata(String metadata) { this.metadata = metadata; }
}
