package com.example.smartcustomerservice.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class LoginUserVO {
    private Long id;
    private String username;
    private String realName;
    private List<String> roleCodes;
    private List<String> permissionsCodes;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private LocalDateTime issuedAt;
    private LocalDateTime expireAt;
}
