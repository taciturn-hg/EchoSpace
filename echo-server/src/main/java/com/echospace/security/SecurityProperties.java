package com.echospace.security;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Security 配置属性：绑定 application.yaml 中 security.* 前缀的配置项。
 * 白名单路径由此统一管理，SecurityConfig（授权层）和 JwtAuthFilter（Filter 层）均从此处读取，
 * 避免路径硬编码和 public static 数组被外部篡改的风险。
 *
 * @Author: taciturn-hg
 * @Date: 5/23/2026 2:49 下午
 */
@ConfigurationProperties(prefix = "security")
@Data
public class SecurityProperties {

    /** 不需要 JWT 认证的路径白名单，对应 yaml 中的 security.whitelist */
    private List<String> whitelist = List.of();
}
