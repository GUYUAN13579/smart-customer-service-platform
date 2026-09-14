package com.example.smartcustomerservice.domain.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// AiTicketDraftVO 属于智能客服平台基础代码。
public class AiTicketDraftVO {

    private Long sessionId;
    private String title;
    private String originalContent;
    private String aiSummary;
    private String category;
    private String priority;
    private String suggestedAction;
    private BigDecimal aiConfidence;
    private List<KnowledgeSearchVO> knowledgeReferences;
    private LocalDateTime createdAt;

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getOriginalContent() {
        return originalContent;
    }

    public void setOriginalContent(String originalContent) {
        this.originalContent = originalContent;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getSuggestedAction() {
        return suggestedAction;
    }

    public void setSuggestedAction(String suggestedAction) {
        this.suggestedAction = suggestedAction;
    }

    public BigDecimal getAiConfidence() {
        return aiConfidence;
    }

    public void setAiConfidence(BigDecimal aiConfidence) {
        this.aiConfidence = aiConfidence;
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
