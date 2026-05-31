package com.echospace.security;

import com.echospace.common.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security 核心配置：CORS、CSRF、会话管理、接口授权、JWT 过滤器注册、异常处理
 *
 * @Author: taciturn-hg
 * @Date: 5/22/2026 9:57 下午
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final ObjectMapper objectMapper;
    private final SecurityProperties securityProperties;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter,
                          ObjectMapper objectMapper,
                          SecurityProperties securityProperties) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.objectMapper = objectMapper;
        this.securityProperties = securityProperties;
    }

    /**
     * 配置 Security 过滤器链
     *
     * @param http HttpSecurity 配置对象
     * @return 构建好的 SecurityFilterChain
     * @Author: taciturn-hg
     * @Date: 5/22/2026 9:57 下午
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 在 Security Filter 层优先处理，避免 OPTIONS 预检被拦截
                .cors(withDefaults())
                // 无状态 API，禁用 CSRF
                .csrf(csrf -> csrf.disable())
                // 纯 JWT API，禁用表单登录、HTTP Basic 和默认登出，避免暴露不必要的端点
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                // 无状态会话，不创建 HttpSession
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 接口授权：白名单放行，其余需认证
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(securityProperties.getWhitelist().toArray(new String[0])).permitAll()
                        .anyRequest().authenticated()
                )
                // 未认证/无权限统一返回标准 JSON，避免默认 403 HTML 响应
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                            objectMapper.writeValue(response.getWriter(), Result.error("未登录或登录已过期"));
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
                            objectMapper.writeValue(response.getWriter(), Result.error("无访问权限"));
                        })
                )
                // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
