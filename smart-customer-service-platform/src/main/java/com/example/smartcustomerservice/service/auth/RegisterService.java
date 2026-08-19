package com.example.smartcustomerservice.service.auth;

import com.example.smartcustomerservice.domain.dto.RegisterRequest;
import com.example.smartcustomerservice.domain.vo.RegisterUserVO;

public interface RegisterService {

    RegisterUserVO register(RegisterRequest request);
}
