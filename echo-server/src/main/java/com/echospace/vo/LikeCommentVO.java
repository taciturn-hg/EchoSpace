package com.echospace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论点赞/取消响应 VO
 * <p>对应接口：POST /api/comments/{id}/like</p>
 * <p>
 * toggle 模式下返回操作后的点赞状态，
 * 前端据此乐观更新 UI（本地 ±1），具体数据在刷新时同步。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "评论点赞/取消响应")
public class LikeCommentVO {

    @Schema(description = "true=已点赞，false=已取消", example = "true")
    private boolean liked;
}
