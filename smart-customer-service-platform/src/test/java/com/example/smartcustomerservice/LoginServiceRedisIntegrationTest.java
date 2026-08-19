package com.example.smartcustomerservice;

import com.example.smartcustomerservice.common.constants.RedisKeyConstants;
import com.example.smartcustomerservice.domain.dto.LoginRequest;
import com.example.smartcustomerservice.domain.dto.RegisterRequest;
import com.example.smartcustomerservice.domain.vo.LoginUserVO;
import com.example.smartcustomerservice.domain.vo.RegisterUserVO;
import com.example.smartcustomerservice.service.auth.LoginService;
import com.example.smartcustomerservice.service.auth.RegisterService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LoginServiceRedisIntegrationTest {

    @Autowired
    private RegisterService registerService;

    @Autowired
    private LoginService loginService;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loginShouldReturnTokenAndSaveRefreshTokenAndPermissionsToRedis() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        String username = "test_" + suffix;
        String password = "12345678";

        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(username);
        registerRequest.setPassword(password);
        registerRequest.setRealName("测试用户" + suffix);
        registerRequest.setDepartment("测试部门");

        RegisterUserVO registerUser = registerService.register(registerRequest);
        assertNotNull(registerUser.getId());

        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername(username);
        loginRequest.setPassword(password);

        LoginUserVO loginUser = loginService.login(loginRequest);
        assertNotNull(loginUser.getAccessToken());
        assertNotNull(loginUser.getRefreshToken());
        assertFalse(loginUser.getAccessToken().isBlank());
        assertFalse(loginUser.getRefreshToken().isBlank());

        String refreshTokenKey = RedisKeyConstants.format(RedisKeyConstants.LOGIN_REFRESH_TOKEN, loginUser.getId());
        String permissionKey = RedisKeyConstants.format(RedisKeyConstants.USER_PERMISSION, loginUser.getId());

        String redisRefreshToken = stringRedisTemplate.opsForValue().get(refreshTokenKey);
        String redisPermissionJson = stringRedisTemplate.opsForValue().get(permissionKey);
        String expectedPermissionJson = objectMapper.writeValueAsString(loginUser.getPermissionsCodes());

        System.out.println("[TEST] refreshTokenKey=" + refreshTokenKey);
        System.out.println("[TEST] refreshToken from login=" + loginUser.getRefreshToken());
        System.out.println("[TEST] refreshToken from redis=" + redisRefreshToken);
        System.out.println("[TEST] permissionKey=" + permissionKey);
        System.out.println("[TEST] permissionJson from login=" + expectedPermissionJson);
        System.out.println("[TEST] permissionJson from redis=" + redisPermissionJson);

        assertEquals(loginUser.getRefreshToken(), redisRefreshToken);
        assertEquals(expectedPermissionJson, redisPermissionJson);
    }
}
