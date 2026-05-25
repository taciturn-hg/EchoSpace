package com.echospace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 图片上传响应 VO
 *
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@Schema(description = "图片上传结果")
public class UploadImageVO {

    @Schema(description = "上传后的文件访问 URL", example = "http://localhost:9000/echospace/images/def456.jpg")
    private String url;
}
