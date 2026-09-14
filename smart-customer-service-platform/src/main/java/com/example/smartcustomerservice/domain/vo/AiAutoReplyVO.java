package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

// AiAutoReplyVO 属于智能客服平台基础代码。
public class AiAutoReplyVO {

    private Long sessionId;
    private String answer;
    private String model;
    private ConversationMessageVO message;
    private List<KnowledgeSearchVO> knowledgeReferences;
    private LocalDateTime createdAt;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public ConversationMessageVO getMessage() {
        return message;
    }

    public void setMessage(ConversationMessageVO message) {
        this.message = message;
    }

    public List<KnowledgeSearchVO> getKnowledgeReferences() {
        return knowledgeReferences;
    }

    public void setKnowledgeReferences(List<KnowledgeSearchVO> knowledgeReferences) {
        this.knowledgeReferences = knowledgeReferences;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
