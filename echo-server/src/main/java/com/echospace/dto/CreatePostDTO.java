package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发布帖子请求参数
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "发布帖子请求")
public class CreatePostDTO {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度 1~200")
    @Schema(description = "标题", example = "帖子标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "富文本 HTML")
    private String contentHtml;
}
