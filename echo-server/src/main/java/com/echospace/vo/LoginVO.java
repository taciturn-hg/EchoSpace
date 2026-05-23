package com.echospace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录 / 刷新 Token 响应 VO
 * <p>对应接口：POST /api/auth/login 和 POST /api/auth/refresh</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "登录 / 刷新 Token 响应")
public class LoginVO {

    @Schema(description = "访问令牌，有效期 30 分钟", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "刷新令牌，有效期 7 天", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "accessToken 过期时间，单位秒", example = "1800")
    private long expiresIn;
}
