package com.echospace.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 发表评论/回复请求 DTO
 * <p>
 * parentId=0 表示一级评论，非0表示二级回复。
 * replyToUid 仅在二级回复时填写，记录被回复的用户ID。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "发表评论/回复请求")
public class CreateCommentDTO {

    @NotNull(message = "父评论ID不能为空")
    @Schema(description = "父评论ID：0=一级评论，非0=二级回复", example = "0")
    private Long parentId;

    @Schema(description = "被回复的用户ID（仅二级回复时填写）", example = "3")
    private Long replyToUid;

    @NotBlank(message = "评论内容不能为空")
    @Size(min = 1, max = 5000, message = "评论内容长度须在1~5000之间")
    @Schema(description = "评论内容，长度 1~5000", example = "写得太好了，支持！")
    private String content;
}
