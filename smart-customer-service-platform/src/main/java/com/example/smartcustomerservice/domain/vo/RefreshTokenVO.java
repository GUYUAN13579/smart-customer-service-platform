package com.example.smartcustomerservice.domain.vo;

import java.time.LocalDateTime;

// RefreshTokenVO 属于智能客服平台基础代码。
public class RefreshTokenVO {

    private String accessToken;
    private Long expiresIn;
    private LocalDateTime expireAt;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDateTime expireAt) {
        this.expireAt = expireAt;
    }
}
