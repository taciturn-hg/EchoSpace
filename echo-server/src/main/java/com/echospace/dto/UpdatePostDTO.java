package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "编辑帖子请求")
public class UpdatePostDTO {

    @NotBlank(message = "标题不能为空")
    @Size(min = 1, max = 200, message = "标题长度 1~200")
    @Schema(description = "新标题", example = "修改后的标题")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Schema(description = "新的富文本 HTML")
    private String contentHtml;
}
