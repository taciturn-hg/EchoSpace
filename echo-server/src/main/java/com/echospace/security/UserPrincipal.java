package com.echospace.security;

/**
 * 自定义认证主体：将 userId 和 username 封装为 principal，
 * 存入 SecurityContextHolder，避免复用语义不明确的 details 字段。
 *
 * @Author: taciturn-hg
 * @Date: 5/23/2026
 */
public record UserPrincipal(Long userId, String username) {
}
