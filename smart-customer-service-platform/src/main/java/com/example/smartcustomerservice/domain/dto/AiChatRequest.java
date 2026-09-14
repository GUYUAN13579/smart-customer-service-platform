package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

// AiChatRequest 属于智能客服平台基础代码。
public class AiChatRequest {

    private Long sessionId;

    @NotBlank(message = "问题不能为空")
    @Size(max = 4000, message = "问题长度不能超过4000个字符")
    private String question;

    @Size(max = 2000, message = "系统提示词长度不能超过2000个字符")
    private String systemPrompt;

    @Size(max = 5, message = "最多只能附加5个文件")
    private List<Long> fileIds;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }
}
