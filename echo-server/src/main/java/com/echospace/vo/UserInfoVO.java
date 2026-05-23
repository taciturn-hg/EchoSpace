package com.echospace.vo;

import com.echospace.entity.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 当前登录用户信息响应 VO
 * <p>对应接口：GET /api/auth/me</p>
 * <p>仅返回前端展示所需的公开字段，password 等敏感字段不暴露。</p>
 */
@Data
@Schema(description = "当前登录用户信息")
public class UserInfoVO {

    @Schema(description = "用户 ID", example = "1")
    private Long id;

    @Schema(description = "用户名", example = "zhangsan")
    private String username;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "邮箱", example = "zhangsan@example.com")
    private String email;

    @Schema(description = "手机号", example = "13800138000")
    private String phone;

    @Schema(description = "头像 URL", example = "http://localhost:9000/echospace/avatars/default.png")
    private String avatar;

    @Schema(description = "个人简介", example = "这个人很懒，什么都没写")
    private String bio;

    @Schema(description = "注册时间", example = "2026-05-20 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 从 User 实体转换为 VO，只映射需要返回的字段
     *
     * @param user 数据库用户实体
     * @return 脱敏后的用户信息 VO
     */
    public static UserInfoVO from(User user) {
        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setCreatedAt(user.getCreatedAt());
        return vo;
    }
}
