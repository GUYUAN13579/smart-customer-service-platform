package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class KnowledgeArticleQueryRequest extends PageQuery {

    @Size(max = 128, message = "关键词最多128位")
    private String keyword;

    @Pattern(regexp = "ORDER|PAYMENT|REFUND|LOGISTICS|ACCOUNT|TECHNICAL|COMPLAINT|GENERAL", message = "文章分类不合法")
    private String category;

    @Pattern(regexp = "DRAFT|PUBLISHED|OFFLINE", message = "文章状态不合法")
    private String status;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
