package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotNull;

public class KnowledgeDocumentCreateRequest {

    @NotNull(message = "文件ID不能为空")
    private Long fileId;

    public Long getFileId() { return fileId; }
    public void setFileId(Long fileId) { this.fileId = fileId; }
}
