package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 刷新 Token 请求 DTO
 * <p>对应接口：POST /api/auth/refresh</p>
 */
@Data
@Schema(description = "刷新 Token 请求")
public class RefreshTokenDTO {

    @Schema(description = "登录时获取的刷新令牌", example = "eyJhbGciOiJIUzI1NiJ9...")
    @NotBlank(message = "refreshToken 不能为空")
    private String refreshToken;
}
