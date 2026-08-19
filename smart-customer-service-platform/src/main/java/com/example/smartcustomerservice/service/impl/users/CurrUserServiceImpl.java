package com.example.smartcustomerservice.service.impl.users;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.smartcustomerservice.common.constants.CommonConstants;
import com.example.smartcustomerservice.common.exception.BusinessException;
import com.example.smartcustomerservice.common.result.ResultCode;
import com.example.smartcustomerservice.domain.entity.SysPermission;
import com.example.smartcustomerservice.domain.entity.SysRole;
import com.example.smartcustomerservice.domain.entity.SysRolePermission;
import com.example.smartcustomerservice.domain.entity.SysUser;
import com.example.smartcustomerservice.domain.entity.SysUserRole;
import com.example.smartcustomerservice.domain.vo.CurrUserVO;
import com.example.smartcustomerservice.domain.vo.MenuVO;
import com.example.smartcustomerservice.mapper.auth.SysPermissionMapper;
import com.example.smartcustomerservice.mapper.auth.SysRoleMapper;
import com.example.smartcustomerservice.mapper.auth.SysRolePermissionMapper;
import com.example.smartcustomerservice.mapper.auth.SysUserMapper;
import com.example.smartcustomerservice.mapper.auth.SysUserRoleMapper;
import com.example.smartcustomerservice.security.SecurityUtils;
import com.example.smartcustomerservice.service.users.CurrUserService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class CurrUserServiceImpl implements CurrUserService {

    private static final String MENU_RESOURCE_TYPE = "MENU";

    private final SysUserMapper sysUserMapper;
    private final SysUserRoleMapper sysUserRoleMapper;
    private final SysRoleMapper sysRoleMapper;
    private final SysRolePermissionMapper sysRolePermissionMapper;
    private final SysPermissionMapper sysPermissionMapper;

    public CurrUserServiceImpl(SysUserMapper sysUserMapper,
                               SysUserRoleMapper sysUserRoleMapper,
                               SysRoleMapper sysRoleMapper,
                               SysRolePermissionMapper sysRolePermissionMapper,
                               SysPermissionMapper sysPermissionMapper) {
        this.sysUserMapper = sysUserMapper;
        this.sysUserRoleMapper = sysUserRoleMapper;
        this.sysRoleMapper = sysRoleMapper;
        this.sysRolePermissionMapper = sysRolePermissionMapper;
        this.sysPermissionMapper = sysPermissionMapper;
    }

    @Override
    public CurrUserVO getCurrentUser() {
        Long userId = requireCurrentUserId();
        SysUser user = getEnabledUser(userId);
        List<SysRole> roles = getEnabledRoles(userId);
        List<SysPermission> permissions = getPermissionsByRoles(roles);

        CurrUserVO vo = new CurrUserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRealName(user.getRealName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatarUrl(user.getAvatarUrl());
        vo.setDepartment(user.getDepartment());
        vo.setStatus(user.getStatus());
        vo.setRoleCodes(roles.stream().map(SysRole::getRoleCode).toList());
        vo.setPermissionCodes(permissions.stream().map(SysPermission::getPermissionCode).toList());
        return vo;
    }

    @Override
    public List<MenuVO> getCurrentMenus() {
        Long userId = requireCurrentUserId();
        getEnabledUser(userId);
        List<SysRole> roles = getEnabledRoles(userId);
        List<SysPermission> menuPermissions = getPermissionsByRoles(roles).stream()
                .filter(permission -> MENU_RESOURCE_TYPE.equals(permission.getResourceType()))
                .sorted(Comparator.comparing(SysPermission::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(SysPermission::getId))
                .toList();
        return buildMenuTree(menuPermissions);
    }

    private Long requireCurrentUserId() {
        Long userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    private SysUser getEnabledUser(Long userId) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getId, userId)
                .eq(SysUser::getDeleted, CommonConstants.NOT_DELETED));
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        if (!Objects.equals(user.getStatus(), CommonConstants.ENABLED)) {
            throw new BusinessException(ResultCode.USER_DISABLED);
        }
        return user;
    }

    private List<SysRole> getEnabledRoles(Long userId) {
        List<SysUserRole> userRoles = sysUserRoleMapper.selectList(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId));
        List<Long> roleIds = userRoles.stream().map(SysUserRole::getRoleId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }
        return sysRoleMapper.selectList(new LambdaQueryWrapper<SysRole>()
                .in(SysRole::getId, roleIds)
                .eq(SysRole::getStatus, CommonConstants.ENABLED));
    }

    private List<SysPermission> getPermissionsByRoles(List<SysRole> roles) {
        List<Long> roleIds = roles.stream().map(SysRole::getId).toList();
        if (roleIds.isEmpty()) {
            return List.of();
        }

        List<SysRolePermission> rolePermissions = sysRolePermissionMapper.selectList(
                new LambdaQueryWrapper<SysRolePermission>()
                        .in(SysRolePermission::getRoleId, roleIds));
        List<Long> permissionIds = rolePermissions.stream()
                .map(SysRolePermission::getPermissionId)
                .distinct()
                .toList();
        if (permissionIds.isEmpty()) {
            return List.of();
        }

        return sysPermissionMapper.selectList(new LambdaQueryWrapper<SysPermission>()
                .in(SysPermission::getId, permissionIds));
    }

    private List<MenuVO> buildMenuTree(List<SysPermission> permissions) {
        Map<Long, MenuVO> menuMap = new LinkedHashMap<>();
        for (SysPermission permission : permissions) {
            menuMap.put(permission.getId(), toMenuVO(permission));
        }

        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO menu : menuMap.values()) {
            if (menu.getParentId() != null && menuMap.containsKey(menu.getParentId())) {
                menuMap.get(menu.getParentId()).getChildren().add(menu);
            } else {
                roots.add(menu);
            }
        }

        sortMenus(roots);
        return roots;
    }

    private void sortMenus(List<MenuVO> menus) {
        menus.sort(Comparator.comparing(MenuVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuVO::getId));
        for (MenuVO menu : menus) {
            sortMenus(menu.getChildren());
        }
    }

    private MenuVO toMenuVO(SysPermission permission) {
        MenuVO vo = new MenuVO();
        vo.setId(permission.getId());
        vo.setPermissionCode(permission.getPermissionCode());
        vo.setPermissionName(permission.getPermissionName());
        vo.setResourceType(permission.getResourceType());
        vo.setResourcePath(permission.getResourcePath());
        vo.setParentId(permission.getParentId());
        vo.setSortOrder(permission.getSortOrder());
        return vo;
    }
}
