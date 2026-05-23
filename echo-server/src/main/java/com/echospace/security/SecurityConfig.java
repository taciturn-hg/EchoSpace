package com.echospace.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.springframework.security.config.Customizer.withDefaults;

/**
 * Spring Security 核心配置：CORS、CSRF、会话管理、接口授权、JWT 过滤器注册
 *
 * @Author: taciturn-hg
 * @Date: 5/22/2026 9:57 下午
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

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
                // 无状态会话，不创建 HttpSession
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 接口授权：白名单放行，其余需认证
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/login", "/api/auth/register", "/api/auth/refresh").permitAll()
                        .anyRequest().authenticated()
                )
                // JWT 过滤器在 UsernamePasswordAuthenticationFilter 之前执行
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
