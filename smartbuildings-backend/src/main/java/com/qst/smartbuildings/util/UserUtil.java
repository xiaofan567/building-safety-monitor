package com.qst.smartbuildings.util;

import com.qst.smartbuildings.entity.AdminUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

public class UserUtil {

    private UserUtil() {
    }

    public static Long getCurrentUserId() {
        UsernamePasswordAuthenticationToken authentication =
                (UsernamePasswordAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AdminUser) {
            return ((AdminUser) principal).getUserId();
        }
        return null;
    }
}