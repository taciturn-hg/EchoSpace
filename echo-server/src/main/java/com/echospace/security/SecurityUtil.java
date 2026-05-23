package com.echospace.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Security 工具类：从 SecurityContextHolder 中获取当前登录用户信息
 *
 * @Author: taciturn-hg
 * @Date: 5/22/2026 9:57 下午
 */
public class SecurityUtil {

    /**
     * 获取当前用户 ID
     *
     * @return 用户 ID，未登录或类型不匹配时返回 null
     * @Author: taciturn-hg
     * @Date: 5/22/2026 11:14 下午
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object principal = auth.getPrincipal();
        if (principal instanceof Long id) {
            return id;
        }
        return null;
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未登录或类型不匹配时返回 null
     * @Author: taciturn-hg
     * @Date: 5/22/2026 11:14 下午
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || auth instanceof AnonymousAuthenticationToken) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof String username) {
            return username;
        }
        return null;
    }
}
