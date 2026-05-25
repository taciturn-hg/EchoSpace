package com.echospace.vo;

import com.echospace.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 资料设置回显 VO
 * <p>对应接口：GET /api/users/me/profile</p>
 * <p>仅返回资料设置页表单需要的字段：头像、昵称、个人简介，避免暴露其他敏感信息。</p>
 *
 * @Author: taciturn-hg
 */
@Data
@Schema(description = "资料设置信息")
public class UserProfileVO {

    @Schema(description = "头像 URL", example = "http://localhost:9000/echospace/avatars/1.png")
    private String avatar;

    @Schema(description = "昵称", example = "张三")
    private String nickname;

    @Schema(description = "个人简介", example = "这个人很懒，什么都没写")
    private String bio;

    /**
     * 从 User 实体转换为资料设置 VO
     *
     * @param user 数据库用户实体
     * @return 仅含 avatar / nickname / bio 的 VO
     */
    public static UserProfileVO from(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setAvatar(user.getAvatar());
        vo.setNickname(user.getNickname());
        vo.setBio(user.getBio());
        return vo;
    }
}
