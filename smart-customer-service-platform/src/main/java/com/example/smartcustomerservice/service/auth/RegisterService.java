package com.example.smartcustomerservice.service.auth;

import com.example.smartcustomerservice.domain.dto.RegisterRequest;
import com.example.smartcustomerservice.domain.vo.RegisterUserVO;

// RegisterService 属于智能客服平台基础代码。
public interface RegisterService {

    RegisterUserVO register(RegisterRequest request);
}
