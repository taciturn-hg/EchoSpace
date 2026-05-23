package com.echospace.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类：生成 AccessToken / RefreshToken，解析并验证 JWT
 *
 * @Author: taciturn-hg
 * @Date: 5/22/2026 9:57 下午
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration:1800000}")
    private long accessExpire;

    @Value("${jwt.refresh-token-expiration:604800000}")
    private long refreshExpire;

    private SecretKey secretKey;

    /**
     * 初始化：将配置中的明文字符串转为 HMAC-SHA 密钥
     *
     * @Author: taciturn-hg
     * @Date: 5/22/2026 10:51 下午
     */
    @PostConstruct
    public void init() {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT Token
     *
     * @param type     "access" 生成 AccessToken，"refresh" 生成 RefreshToken
     * @param sub      主题（用户ID）
     * @param username 用户名，存入 claims 便于后续读取
     * @return 签发的 JWT 字符串
     * @Author: taciturn-hg
     * @Date: 5/22/2026 10:51 下午
     */
    public String generateToken(String type, String sub, String username) {
        long expire = "access".equals(type) ? accessExpire : refreshExpire;
        return Jwts.builder()
                .subject(sub)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expire))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * 解析并验证 JWT Token
     *
     * @param token JWT 字符串
     * @return 解析后的 Claims，包含 subject 和自定义 claims
     * @throws io.jsonwebtoken.JwtException 签名无效、过期、格式错误时抛出
     * @Author: taciturn-hg
     * @Date: 5/22/2026 11:01 下午
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
