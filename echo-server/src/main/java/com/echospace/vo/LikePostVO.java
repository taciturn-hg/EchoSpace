package com.echospace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子点赞/取消响应 VO
 * <p>对应接口：POST /api/posts/{id}/like</p>
 * <p>
 * toggle 模式下返回操作后的点赞状态与最新点赞数，
 * 前端可据此乐观更新 UI。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子点赞/取消响应")
public class LikePostVO {

    @Schema(description = "true=已点赞，false=已取消", example = "true")
    private boolean liked;

    @Schema(description = "操作后的最新点赞数", example = "33")
    private int likeCount;
}
