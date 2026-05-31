package com.echospace.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 关注/取消关注响应 VO
 * <p>对应接口：POST /api/users/{id}/follow</p>
 * <p>
 * toggle 模式下返回操作后的关注状态，
 * 前端可据此乐观更新 UI。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "关注/取消关注响应")
public class FollowVO {

    @Schema(description = "true=已关注，false=已取消", example = "true")
    private boolean followed;
}
