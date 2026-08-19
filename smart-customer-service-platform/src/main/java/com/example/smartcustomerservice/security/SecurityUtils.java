package com.example.smartcustomerservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static LoginUserContext getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUserContext currentUser)) {
            return null;
        }

        return currentUser;
    }

    public static Long getCurrentUserId() {
        LoginUserContext currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.getUserId();
    }

    public static String getCurrentUsername() {
        LoginUserContext currentUser = getCurrentUser();
        return currentUser == null ? null : currentUser.getUsername();
    }
}
