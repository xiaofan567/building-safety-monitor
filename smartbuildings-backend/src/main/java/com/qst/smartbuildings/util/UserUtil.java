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
        if (principal instanceof Number number) {
            return number.longValue();
        }
        if (principal instanceof CharSequence sequence && sequence.length() > 0) {
            try {
                return Long.parseLong(sequence.toString());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        if (principal instanceof java.util.Map<?, ?> map) {
            Object id = map.get("userId");
            if (id instanceof Number number) {
                return number.longValue();
            }
            if (id != null) {
                try {
                    return Long.parseLong(id.toString());
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }
        if (principal instanceof AdminUser) {
            return ((AdminUser) principal).getUserId();
        }
        return null;
    }
}

