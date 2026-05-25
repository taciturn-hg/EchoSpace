package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 更新账号设置请求 DTO
 * <p>对应接口：PUT /api/users/me/settings</p>
 * <p>
 * phone 与 email 均为可选：用户每次只需修改其中一项，Service 层基于 null 判断是否需要执行更新。
 * 提交的新值需与库中其他用户唯一不冲突，否则抛出 409 Conflict（在 Service 层校验）。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "更新账号设置请求")
public class UpdateSettingsDTO {

    @Schema(description = "新手机号，需唯一", example = "13900139000")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "新邮箱，需唯一", example = "new@example.com")
    @Email(message = "邮箱格式不正确")
    private String email;
}
