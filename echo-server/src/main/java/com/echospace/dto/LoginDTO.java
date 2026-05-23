package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户登录请求 DTO
 * <p>对应接口：POST /api/auth/login</p>
 * <p>account 字段支持用户名 / 邮箱 / 手机号三种形式，由 Service 层自动识别。</p>
 */
@Data
@Schema(description = "用户登录请求")
public class LoginDTO {

    @Schema(description = "登录账号：用户名 / 邮箱 / 手机号", example = "zhangsan")
    @NotBlank(message = "账号不能为空")
    private String account;

    @Schema(description = "密码", example = "123456")
    @NotBlank(message = "密码不能为空")
    private String password;
}
