package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.Size;

import java.util.List;

// AiAutoReplyRequest 属于智能客服平台基础代码。
public class AiAutoReplyRequest {

    @Size(max = 4000, message = "客户问题长度不能超过4000个字符")
    private String customerQuestion;

    @Size(max = 5, message = "最多只能附加5个文件")
    private List<Long> fileIds;

    public String getCustomerQuestion() {
        return customerQuestion;
    }

    public void setCustomerQuestion(String customerQuestion) {
        this.customerQuestion = customerQuestion;
    }

    public List<Long> getFileIds() {
        return fileIds;
    }

    public void setFileIds(List<Long> fileIds) {
        this.fileIds = fileIds;
    }
}
