package com.echospace.security;

import com.echospace.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SecurityProperties securityProperties;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    private List<String> whitelist;

    /**
     * 缓存白名单列表，避免每次请求重复调用 securityProperties.getWhitelist()
     *
     * @Author: taciturn-hg
     * @Date: 5/23/2026
     */
    @PostConstruct
    public void init() {
        this.whitelist = securityProperties.getWhitelist();
    }

    /**
     * 白名单路径直接跳过整个过滤器，不进入 doFilterInternal
     *
     * @param request HTTP 请求
     * @return 命中白名单时返回 true，跳过过滤器
     * @Author: taciturn-hg
     * @Date: 5/23/2026
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return whitelist.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, uri));
    }

    /**
     * 过滤器核心逻辑
     *
     * @param request     HTTP 请求
     * @param response    HTTP 响应
     * @param filterChain 过滤器链
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

        // 2. 解析 JWT → userId + username，异常时返回 401 + Result 结构
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            writeUnauthorized(response, "Token已过期");
            return;
        } catch (JwtException e) {
            writeUnauthorized(response, "Token无效");
            return;
        }

        // 3. 提取 userId + username，subject 非数字时按 Token 无效处理
        Long userId;
        try {
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank()) {
                throw new NumberFormatException("subject is empty");
            }
            userId = Long.valueOf(subject);
        } catch (NumberFormatException e) {
            writeUnauthorized(response, "Token无效");
            return;
        }
        String username = claims.get("username", String.class);
        if (username == null || username.isBlank()) {
            writeUnauthorized(response, "Token无效");
            return;
        }

        // 4. 校验 Token 类型：缺失/空视为格式无效，非 access 视为类型错误
        String tokenType = claims.get("type", String.class);
        if (tokenType == null || tokenType.isBlank()) {
            writeUnauthorized(response, "Token无效");
            return;
        }
        if (!JwtUtil.TokenType.ACCESS.claimValue().equals(tokenType)) {
            writeUnauthorized(response, "Token类型错误，请使用AccessToken");
            return;
        }

        // 5. 可选：查 Redis/MySQL 校验用户状态（是否被禁用）
        // if (redisTemplate.opsForValue().get("user:ban:" + userId) != null) { ... }

        // 6. 写入 SecurityContextHolder（无密码的认证信息）
        UserPrincipal principal = new UserPrincipal(userId, username);
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * 向响应写出 401 + 统一 Result JSON，集中管理响应头与序列化逻辑
     *
     * @param response HTTP 响应
     * @param message  返回给客户端的错误描述
     * @Author: taciturn-hg
     * @Date: 5/23/2026
     */
    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        objectMapper.writeValue(response.getWriter(), Result.error(message));
    }
}
