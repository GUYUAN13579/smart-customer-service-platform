package com.example.smartcustomerservice.domain.dto;

import com.example.smartcustomerservice.common.request.PageQuery;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CustomerQueryRequest extends PageQuery {

    @Size(max = 128, message = "关键词最多 128 位")
    private String keyword;

    @Pattern(regexp = "NORMAL|VIP|ENTERPRISE", message = "客户等级只能是 NORMAL、VIP 或 ENTERPRISE")
    private String level;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
