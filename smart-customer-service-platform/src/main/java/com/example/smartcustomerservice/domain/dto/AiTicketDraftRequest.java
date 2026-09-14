package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

// AiTicketDraftRequest 属于智能客服平台基础代码。
public class AiTicketDraftRequest {

    @Size(max = 4000, message = "用户原始诉求最多4000个字符")
    private String originalContent;

    @Size(max = 1000, message = "补充要求最多1000个字符")
    private String extraRequirement;

    @Size(max = 5, message = "最多只能附加5个文件")
    private List<Long> fileIds;

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getExtraRequirement() {
        return extraRequirement;
    }

    public void setExtraRequirement(String extraRequirement) {
        this.extraRequirement = extraRequirement;
    }

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }
}
