package com.example.smartcustomerservice.service.auth;

import com.example.smartcustomerservice.domain.dto.LoginRequest;
import com.example.smartcustomerservice.domain.vo.LoginUserVO;
import com.example.smartcustomerservice.domain.vo.RefreshTokenVO;

public interface LoginService {
    LoginUserVO login(LoginRequest loginRequest);

    RefreshTokenVO refresh(String refreshToken);

    Boolean logout();
}
