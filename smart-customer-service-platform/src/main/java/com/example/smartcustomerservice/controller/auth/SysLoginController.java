package com.example.smartcustomerservice.controller.auth;

import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.result.ApiResult;
import com.example.smartcustomerservice.domain.dto.LoginRequest;
import com.example.smartcustomerservice.domain.dto.RefreshTokenRequest;
import com.example.smartcustomerservice.domain.vo.LoginUserVO;
import com.example.smartcustomerservice.domain.vo.RefreshTokenVO;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.auth.LoginService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Tag(name = "系统用户登陆")
@RestController
@RequestMapping(CommonConstants.API_PREFIX + "/auth")
public class SysLoginController {
    private final LoginService loginService;

    public SysLoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public ApiResult<LoginUserVO> login(@Valid @RequestBody LoginRequest request)
    {

        return ApiResult.success(loginService.login(request));
    }

    @PostMapping("/refresh")
    public ApiResult<RefreshTokenVO> refresh(@Valid @RequestBody RefreshTokenRequest request)
    {

        return ApiResult.success(loginService.refresh(request.getRefreshToken()));
    }

    @PostMapping("/logout")
    public ApiResult<Boolean> logout()
    {
        return ApiResult.success(loginService.logout());
    }



}
