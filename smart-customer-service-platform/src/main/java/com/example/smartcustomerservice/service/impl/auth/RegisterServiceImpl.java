package com.example.smartcustomerservice.service.impl.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.dto.RegisterRequest;
import com.example.smartcustomerservice.domain.entity.AuditLog;
import com.example.smartcustomerservice.domain.entity.SysRole;
import com.example.smartcustomerservice.domain.entity.SysUser;
import com.example.smartcustomerservice.domain.entity.SysUserRole;
import com.example.smartcustomerservice.domain.vo.RegisterUserVO;
import com.example.smartcustomerservice.mapper.auth.AuditLogMapper;
import com.example.smartcustomerservice.mapper.auth.SysRoleMapper;
import com.example.smartcustomerservice.mapper.auth.SysUserMapper;
import com.example.smartcustomerservice.mapper.auth.SysUserRoleMapper;
import com.example.smartcustomerservice.service.auth.RegisterService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RegisterServiceImpl implements RegisterService {

    private static final String DEFAULT_ROLE_CODE = "AGENT";

    private final SysUserMapper sysUserMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final AuditLogMapper auditLogMapper;
    private final PasswordEncoder passwordEncoder;

    public RegisterServiceImpl(SysUserMapper sysUserMapper,
                               SysRoleMapper sysRoleMapper,
                               SysUserRoleMapper sysUserRoleMapper,
                               AuditLogMapper auditLogMapper,
                               PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.auditLogMapper = auditLogMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public RegisterUserVO register(RegisterRequest request) {
        Long sameUsernameCount = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())
                .eq(SysUser::getDeleted, CommonConstants.NOT_DELETED));
        if (sameUsernameCount > 0) {
            throw new BusinessException(ResultCode.USERNAME_ALREADY_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        SysUser user = new SysUser();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRealName(request.getRealName());
        user.setEmail(blankToNull(request.getEmail()));
        user.setPhone(blankToNull(request.getPhone()));
        user.setDepartment(blankToNull(request.getDepartment()));
        user.setStatus(CommonConstants.ENABLED);
        user.setDeleted(CommonConstants.NOT_DELETED);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        sysUserMapper.insert(user);

        bindDefaultRoleIfExists(user.getId(), now);
        saveRegisterAuditLog(user, now);

        return toRegisterUserVO(user);
    }

    private void bindDefaultRoleIfExists(Long userId, LocalDateTime now) {
        SysRole role = sysRoleMapper.selectOne(new LambdaQueryWrapper<SysRole>()
                .eq(SysRole::getRoleCode, DEFAULT_ROLE_CODE)
                .eq(SysRole::getStatus, CommonConstants.ENABLED)
                .last("LIMIT 1"));
        if (role == null) {
            return;
        }

        SysUserRole userRole = new SysUserRole();
        userRole.setUserId(userId);
        userRole.setRoleId(role.getId());
        userRole.setCreatedAt(now);
        sysUserRoleMapper.insert(userRole);
    }

    private void saveRegisterAuditLog(SysUser user, LocalDateTime now) {
        AuditLog auditLog = new AuditLog();
        auditLog.setOperatorId(user.getId());
        auditLog.setAction("AUTH_REGISTER");
        auditLog.setResourceType("sys_user");
        auditLog.setResourceId(String.valueOf(user.getId()));
        auditLog.setDetail("{\"username\":\"" + user.getUsername() + "\"}");
        auditLog.setCreatedAt(now);
        auditLogMapper.insert(auditLog);
    }

    private RegisterUserVO toRegisterUserVO(SysUser user) {
        RegisterUserVO vo = new RegisterUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setDepartment(user.getDepartment());
        vo.setStatus(user.getStatus());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
