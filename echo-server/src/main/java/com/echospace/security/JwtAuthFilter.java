package com.echospace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * JWT 认证过滤器：在每个请求到达 Controller 之前解析 Authorization Header 中的 Bearer Token，
 * 验证通过后将用户信息写入 SecurityContextHolder，后续组件即可通过 SecurityUtil 获取当前用户。
 *
 * @Author: taciturn-hg
 * @Date: 5/22/2026 9:57 下午
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    /**
     * 过滤器核心逻辑
     *
     * @param request      HTTP 请求
     * @param response     HTTP 响应
     * @param filterChain  过滤器链
     * @Author: taciturn-hg
     * @Date: 5/22/2026 9:57 下午
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 1. 从 Header 取 Token
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        String token = authHeader.substring(7);

        // 2. 解析 JWT → userId + username，异常时返回 401
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token已过期\"}");
            return;
        } catch (JwtException e) {
            response.setStatus(401);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"Token无效\"}");
            return;
        }

        // 3. 可选：查 Redis/MySQL 校验用户状态（是否被禁用）
        // if (redisTemplate.opsForValue().get("user:ban:" + userId) != null) { ... }

        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);

        // 4. 写入 SecurityContextHolder（无密码的认证信息）
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        // 把 username 存到 details 里，方便 Controller 取
        authentication.setDetails(username);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }
}
