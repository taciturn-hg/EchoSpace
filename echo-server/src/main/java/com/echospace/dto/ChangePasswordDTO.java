package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 修改密码请求 DTO
 * <p>对应接口：PUT /api/users/me/password</p>
 * <p>
 * 三字段均为必填；Service 层会校验：
 * 1) oldPassword 与库中 BCrypt 哈希一致；
 * 2) newPassword 与 confirmPassword 完全一致；
 * 3) newPassword 与 oldPassword 不能相同（避免无意义的更新）。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "修改密码请求")
public class ChangePasswordDTO {

    @Schema(description = "原密码", example = "123456")
    @NotBlank(message = "原密码不能为空")
    private String oldPassword;

    @Schema(description = "新密码，长度 6~20", example = "654321")
    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "新密码长度须在 6~20 之间")
    private String newPassword;

    @Schema(description = "确认新密码，须与 newPassword 一致", example = "654321")
    @NotBlank(message = "确认新密码不能为空")
    private String confirmPassword;
}
