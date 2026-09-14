package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 知识库语义检索请求。
 */
public class KnowledgeSearchRequest {

    @NotBlank(message = "检索内容不能为空")
    @Size(max = 2000, message = "检索内容不能超过2000个字符")
    private String query;

    @Pattern(regexp = "ARTICLE|DOCUMENT", message = "来源类型只能是 ARTICLE 或 DOCUMENT")
    private String sourceType;

    @Size(max = 64, message = "知识分类不能超过64个字符")
    private String category;

    @Min(value = 1, message = "topK 最小为 1")
    @Max(value = 20, message = "topK 最大为 20")
    private Integer topK = 5;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getTopK() {
        return topK;
    }

    public void setTopK(Integer topK) {
        this.topK = topK;
    }
}
