package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

// TicketReviewRequest 属于智能客服平台基础代码。
public class TicketReviewRequest {

    @NotBlank(message = "审核结果不能为空")
    @Pattern(regexp = "APPROVED|REJECTED", message = "审核结果只能是 APPROVED 或 REJECTED")
    private String reviewResult;

    @Size(max = 255, message = "工单标题最多255个字符")
    private String title;

    @Size(max = 5000, message = "工单内容最多5000个字符")
    private String content;

    @Size(max = 5000, message = "AI标准化总结最多5000个字符")
    private String aiSummary;

    @Size(max = 64, message = "问题分类最多64个字符")
    private String category;

    @Pattern(regexp = "P1|P2|P3|P4", message = "优先级只能是 P1、P2、P3 或 P4")
    private String priority;

    @Size(max = 5000, message = "建议处理动作最多5000个字符")
    private String suggestedAction;

    @Size(max = 500, message = "审核备注最多500个字符")
    private String reviewRemark;

    public String getReviewResult() {
        return reviewResult;
    }

    public void setReviewResult(String reviewResult) {
        this.reviewResult = reviewResult;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getReviewRemark() {
        return reviewRemark;
    }

    public void setReviewRemark(String reviewRemark) {
        this.reviewRemark = reviewRemark;
    }
}
