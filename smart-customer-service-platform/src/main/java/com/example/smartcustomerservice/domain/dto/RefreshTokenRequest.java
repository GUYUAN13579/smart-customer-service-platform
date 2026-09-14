package com.example.smartcustomerservice.domain.dto;

import jakarta.validation.constraints.NotBlank;

// RefreshTokenRequest 属于智能客服平台基础代码。
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken不能为空")
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
