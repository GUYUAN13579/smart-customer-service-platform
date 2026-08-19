package com.example.smartcustomerservice.security;

import java.util.List;

public class LoginUserContext {

    private Long userId;
    private String username;
    private List<String> roleCodes;
    private List<String> permissionCodes;

    public LoginUserContext() {
    }

    public LoginUserContext(Long userId, String username, List<String> roleCodes, List<String> permissionCodes) {
        this.userId = userId;
        this.username = username;
        this.roleCodes = roleCodes;
        this.permissionCodes = permissionCodes;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoleCodes() {
        return roleCodes;
    }

    public void setRoleCodes(List<String> roleCodes) {
        this.roleCodes = roleCodes;
    }

    public List<String> getPermissionCodes() {
        return permissionCodes;
    }

    public void setPermissionCodes(List<String> permissionCodes) {
        this.permissionCodes = permissionCodes;
    }
}
