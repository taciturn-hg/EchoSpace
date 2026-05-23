package com.echospace.security;

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
     * @return 用户 ID，未登录时返回 null
     * @Author: taciturn-hg
     * @Date: 5/22/2026 11:14 下午
     */
    public static Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        return (Long) auth.getPrincipal();
    }

    /**
     * 获取当前用户名
     *
     * @return 用户名，未登录时返回 null
     * @Author: taciturn-hg
     * @Date: 5/22/2026 11:14 下午
     */
    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) {
            return null;
        }
        return (String) auth.getDetails();
    }
}
