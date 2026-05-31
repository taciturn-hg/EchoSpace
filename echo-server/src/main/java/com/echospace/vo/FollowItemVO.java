package com.echospace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 粉丝/关注列表项 VO
 * <p>对应接口：GET /api/users/{id}/followers 和 GET /api/users/{id}/following</p>
 *
 * @Author: taciturn-hg
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "粉丝/关注列表项")
public class FollowItemVO {

    @Schema(description = "用户ID", example = "2")
    private Long id;

    @Schema(description = "用户名", example = "lisi")
    private String username;

    @Schema(description = "昵称（可为空）", example = "李四")
    private String nickname;

    @Schema(description = "头像URL", example = "http://...")
    private String avatar;

    @Schema(description = "关注时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime followedAt;
}
