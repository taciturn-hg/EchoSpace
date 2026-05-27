package com.echospace.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 公开用户信息视图对象，用于个人主页展示（API 2.1 响应）
 * <p>
 * 与 {@link UserProfileVO} 的区别：UserProfileVO 仅含可编辑字段（avatar / nickname / bio），
 * 用于当前登录用户的资料设置页；PublicUserVO 包含完整的公开统计信息（发帖数 / 粉丝 / 关注）
 * 以及当前登录用户是否已关注该用户。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PublicUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
    private String bio;
    private Integer postCount;
    private Integer followerCount;
    private Integer followingCount;
    private Boolean isFollowed;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}
