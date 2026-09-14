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
// LoginServiceImpl 属于智能客服平台基础代码。
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
        // 先按用户名查询账号，再使用 BCrypt matches 比较密码；不能对明文密码再次 encode 后查询，
        // 因为 BCrypt 每次编码都会产生不同盐值。
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

        // 用户、角色、权限采用多对多关联。这里只加载启用角色，避免已禁用角色继续获得权限。
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

        // 权限最终以 permissionCode 表示，既用于前端菜单/按钮控制，也用于后端 @PreAuthorize 鉴权。
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

        // accessToken 是短期业务凭证；refreshToken 是长期换取凭证，二者必须带不同 tokenType。
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getUsername(), roleCodes);
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId(), user.getUsername());

        // 将 refreshToken 和权限缓存分别存入 Redis：前者支持主动登出与刷新校验，
        // 后者让 JWT 过滤器无需在每个请求中重复查库。
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
        // 刷新时同时验证 JWT 签名/类型和 Redis 中的最新值，旧 refreshToken 在重新登录或登出后失效。
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

        // 刷新 accessToken 时重新加载角色和权限，确保权限调整会在下一次刷新后生效。
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
        // JWT 自身无状态，删除 Redis 中的 refreshToken 才能实现主动登出；
        // 同时删除权限缓存，避免下一位使用该账号时读取旧权限。
        stringRedisTemplate.delete(refreshTokenKey);
        stringRedisTemplate.delete(permissionKey);
        SecurityContextHolder.clearContext();
        return true;
    }
}
