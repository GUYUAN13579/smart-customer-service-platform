package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public class KnowledgeArticleCreateRequest {

    @NotBlank(message = "文章标题不能为空")
    @Size(max = 255, message = "文章标题最多255位")
    private String title;

    @Size(max = 1000, message = "文章摘要最多1000位")
    private String summary;

    @NotBlank(message = "文章正文不能为空")
    private String content;

    @NotBlank(message = "文章分类不能为空")
    @Pattern(regexp = "ORDER|PAYMENT|REFUND|LOGISTICS|ACCOUNT|TECHNICAL|COMPLAINT|GENERAL", message = "文章分类不合法")
    private String category = "GENERAL";

    @Size(max = 20, message = "标签最多20个")
    private List<@NotBlank(message = "标签不能为空") @Size(max = 32, message = "单个标签最多32位") String> tags;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
}
