package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 更新资料设置请求 DTO
 * <p>对应接口：PUT /api/users/me/profile</p>
 * <p>
 * 三个字段全部为可选：前端只提交用户实际修改过的字段，未传字段在服务层会被跳过更新（保持原值）。
 * Service 层通过 null 判断是否需要更新对应列，避免误把空值写回数据库。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "更新资料设置请求")
public class UpdateProfileDTO {

    @Schema(description = "头像 URL（由文件上传接口生成）", example = "http://localhost:9000/echospace/avatars/1.png")
    @Size(max = 500, message = "头像 URL 长度不能超过 500")
    private String avatar;

    @Schema(description = "昵称，最长 50 字", example = "张三")
    @Size(max = 50, message = "昵称长度最长 50 字")
    private String nickname;

    @Schema(description = "个人简介，最长 500 字", example = "新个性签名")
    @Size(max = 500, message = "个人简介最长 500 字")
    private String bio;
}
