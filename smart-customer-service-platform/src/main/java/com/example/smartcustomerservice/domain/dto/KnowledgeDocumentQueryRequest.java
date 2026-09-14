package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class KnowledgeDocumentQueryRequest extends PageQuery {

    @Size(max = 255, message = "关键词最多255位")
    private String keyword;

    @Pattern(regexp = "UPLOADED|PARSING|PARSED|INDEXED|FAILED|OFFLINE", message = "文档状态不合法")
    private String status;

    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
