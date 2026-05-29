package com.echospace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帖子收藏/取消响应 VO
 * <p>对应接口：POST /api/posts/{id}/favorite</p>
 * <p>
 * toggle 模式下返回操作后的收藏状态，
 * 前端可据此乐观更新 UI。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子收藏/取消响应")
public class FavoritePostVO {

    @Schema(description = "true=已收藏，false=已取消", example = "true")
    private boolean favorited;
}
