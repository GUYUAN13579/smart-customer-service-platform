package com.example.smartcustomerservice.controller.auth;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.dto.RegisterRequest;
import com.example.smartcustomerservice.domain.vo.RegisterUserVO;
import com.example.smartcustomerservice.service.auth.RegisterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证注册")
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/auth")
public class RegisterController {

    private final RegisterService registerService;

    public RegisterController(RegisterService registerService) {
        this.registerService = registerService;
    }

    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public ApiResult<RegisterUserVO> register(@Valid @RequestBody RegisterRequest request) {
        return ApiResult.success(registerService.register(request));
    }
}
