package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 用户注册请求 DTO
 * <p>对应接口：POST /api/auth/register</p>
 */
@Data
@Schema(description = "用户注册请求")
public class RegisterDTO {

    @Schema(description = "用户名，长度 2~50", example = "zhangsan")
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 50, message = "用户名长度须在 2~50 之间")
    private String username;

    @Schema(description = "手机号，需唯一", example = "13800138000")
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "邮箱，需唯一", example = "zhangsan@example.com")
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;

    @Schema(description = "密码，长度 6~100", example = "123456")
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 100, message = "密码长度须在 6~100 之间")
    private String password;

    @Schema(description = "确认密码，须与 password 一致", example = "123456")
    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
