package com.example.smartcustomerservice.service.impl.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.constants.RedisKeyConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.LoginRequest;
import com.example.smartcustomerservice.domain.entity.*;
import com.example.smartcustomerservice.domain.vo.LoginUserVO;
import com.example.smartcustomerservice.domain.vo.RefreshTokenVO;
import com.example.smartcustomerservice.mapper.auth.*;
import com.example.smartcustomerservice.security.JwtTokenProvider;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.auth.LoginService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
public class LoginServiceImpl implements LoginService {
    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final AuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;
    private final JwtTokenProvider jwtTokenProvider;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public LoginServiceImpl(SysUserMapper sysUserMapper,
                            SysRoleMapper sysRoleMapper,
                            SysUserRoleMapper sysUserRoleMapper,
                            AuditLogMapper auditLogMapper,
                            PasswordEncoder passwordEncoder,
                            SysRolePermissionMapper sysRolePermissionMapper,
                            SysPermissionMapper sysPermissionMapper,
                            JwtTokenProvider jwtTokenProvider,
                            StringRedisTemplate stringRedisTemplate,
                            ObjectMapper objectMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysPermissionMapper = sysPermissionMapper;
        this.jwtTokenProvider = jwtTokenProvider;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public LoginUserVO login(LoginRequest loginRequest) {
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        //查询sys_user表，比对登录信息是否正确
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getUsername, username)
                        .eq(SysUser::getDeleted, 0)
        );
        if (user == null) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BusinessException(ResultCode.USERNAME_OR_PASSWORD_ERROR);
        }
        if (!Objects.equals(user.getStatus(), 1)) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        //查询sys_role和sys_user_role表，获取用户角色信息
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, user.getId())
        );

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<SysRole> roles = roleIds.isEmpty()
                ? List.of()
                : sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1)
        );

        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .toList();

        List<Long> enabledRoleIds = roles.stream()
                .map(SysRole::getId)
                .toList();

        //根据roleCodes查询sys_role_permission表获取权限列表
        List<SysRolePermission> rolePermissions = roles.isEmpty()
                ? List.of()
                : sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, enabledRoleIds)
        );

        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .toList();

        List<SysPermission> sysPermissions = permissionIds.isEmpty()
                ? List.of()
                : sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permissionIds)
        );

        List<String> permissionCodes = sysPermissions.stream()
                .map(SysPermission::getPermissionCode)
                .toList();

        //获取accessToken和refreshToken
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), roleCodes);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getUsername());

        //把token和权限放到redis里，方便后续使用
        String permissionJson;
        try {
            permissionJson = objectMapper.writeValueAsString(permissionCodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String refreshTokenKey = RedisKeyConstants.format(RedisKeyConstants.LOGIN_REFRESH_TOKEN, user.getId());
        String permissionKey = RedisKeyConstants.format(RedisKeyConstants.USER_PERMISSION, user.getId());

        // refreshToken，用于刷新 accessToken 和退出登录主动失效
        stringRedisTemplate.opsForValue().set(
                refreshTokenKey,
                refreshToken,
                jwtTokenProvider.getRefreshTokenExpireSeconds(),
                TimeUnit.SECONDS
        );

        // 权限缓存，用于鉴权时少查数据库
        stringRedisTemplate.opsForValue().set(
                permissionKey,
                permissionJson,
                jwtTokenProvider.getAccessTokenExpireSeconds(),
                TimeUnit.SECONDS
        );

        System.out.println("[LOGIN] userId=" + user.getId() + ", username=" + user.getUsername());
        System.out.println("[LOGIN] roleCodes=" + roleCodes);
        System.out.println("[LOGIN] permissionCodes=" + permissionCodes);
        System.out.println("[LOGIN] redis refreshToken key=" + refreshTokenKey
                + ", value=" + stringRedisTemplate.opsForValue().get(refreshTokenKey));
        System.out.println("[LOGIN] redis permission key=" + permissionKey
                + ", value=" + stringRedisTemplate.opsForValue().get(permissionKey));

        //构建LoginUserVO，返回前端
        LoginUserVO loginUserVO = new LoginUserVO();
        loginUserVO.setId(user.getId());
        loginUserVO.setUsername(user.getUsername());
        loginUserVO.setRealName(user.getRealName());
        loginUserVO.setPermissionsCodes(permissionCodes);
        loginUserVO.setAccessToken(accessToken);
        loginUserVO.setRefreshToken(refreshToken);
        loginUserVO.setRoleCodes(roleCodes);
        loginUserVO.setExpiresIn(jwtTokenProvider.getAccessTokenExpireSeconds());
        loginUserVO.setIssuedAt(LocalDateTime.now());
        loginUserVO.setExpireAt(jwtTokenProvider.getExpireAt(accessToken));

        return loginUserVO;
    }

    @Override
    public RefreshTokenVO refresh(String refreshToken) {
        if (!jwtTokenProvider.isRefreshToken(refreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        Long userId = jwtTokenProvider.getUserId(refreshToken);
        String username = jwtTokenProvider.getUsername(refreshToken);
        String refreshTokenKey = RedisKeyConstants.format(RedisKeyConstants.LOGIN_REFRESH_TOKEN, userId);
        String redisRefreshToken = stringRedisTemplate.opsForValue().get(refreshTokenKey);
        if (!refreshToken.equals(redisRefreshToken)) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }

        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getDeleted, CommonConstants.NOT_DELETED));
        if (user == null) {
            throw new BusinessException(ResultCode.TOKEN_INVALID);
        }
        if (!Objects.equals(user.getStatus(), CommonConstants.ENABLED)) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }

        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
        );

        List<Long> roleIds = userRoles.stream()
                .map(SysUserRole::getRoleId)
                .toList();

        List<SysRole> roles = roleIds.isEmpty()
                ? List.of()
                : sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>()
                        .in(SysRole::getId, roleIds)
                        .eq(SysRole::getStatus, 1)
        );

        List<String> roleCodes = roles.stream()
                .map(SysRole::getRoleCode)
                .toList();

        List<Long> enabledRoleIds = roles.stream()
                .map(SysRole::getId)
                .toList();

        List<SysRolePermission> rolePermissions = roles.isEmpty()
                ? List.of()
                : sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, enabledRoleIds)
        );

        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .toList();

        List<SysPermission> sysPermissions = permissionIds.isEmpty()
                ? List.of()
                : sysPermissionMapper.selectList(
                new LambdaQueryWrapper<SysPermission>()
                        .in(SysPermission::getId, permissionIds)
        );

        List<String> permissionCodes = sysPermissions.stream()
                .map(SysPermission::getPermissionCode)
                .toList();

        String permissionJson;
        try {
            permissionJson = objectMapper.writeValueAsString(permissionCodes);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String accessToken = jwtTokenProvider.createAccessToken(userId, username, roleCodes);
        String permissionKey = RedisKeyConstants.format(RedisKeyConstants.USER_PERMISSION, userId);
        stringRedisTemplate.opsForValue().set(
                permissionKey,
                permissionJson,
                jwtTokenProvider.getAccessTokenExpireSeconds(),
                TimeUnit.SECONDS
        );

        RefreshTokenVO refreshTokenVO = new RefreshTokenVO();
        refreshTokenVO.setAccessToken(accessToken);
        refreshTokenVO.setExpireAt(jwtTokenProvider.getExpireAt(accessToken));
        refreshTokenVO.setExpiresIn(jwtTokenProvider.getAccessTokenExpireSeconds());

        return refreshTokenVO;
    }

    @Override
    public Boolean logout() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        String refreshTokenKey = RedisKeyConstants.format(RedisKeyConstants.LOGIN_REFRESH_TOKEN, userId);
        String permissionKey = RedisKeyConstants.format(RedisKeyConstants.USER_PERMISSION, userId);
        stringRedisTemplate.delete(refreshTokenKey);
        stringRedisTemplate.delete(permissionKey);
        SecurityContextHolder.clearContext();
        return true;
    }
}
