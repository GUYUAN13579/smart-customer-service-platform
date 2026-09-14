package com.example.smartcustomerservice.domain.vo;

// FileContentVO 属于智能客服平台基础代码。
public class FileContentVO {

    private String originalName;
    private String contentType;
    private byte[] content;

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }
}
