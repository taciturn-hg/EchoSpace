package com.echospace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 头像上传响应 VO
 *
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@Schema(description = "头像上传结果")
public class UploadAvatarVO {

    @Schema(description = "上传后的文件访问 URL", example = "http://localhost:9000/echospace/avatars/abc123.png")
    private String url;
}
