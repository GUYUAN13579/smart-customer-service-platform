package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;
import java.util.List;

// AiChatVO 属于智能客服平台基础代码。
public class AiChatVO {

    private String question;
    private String answer;
    private String model;
    private List<KnowledgeSearchVO> knowledgeReferences;
    private LocalDateTime createdAt;

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
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
