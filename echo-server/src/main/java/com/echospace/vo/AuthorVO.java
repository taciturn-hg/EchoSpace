package com.echospace.vo;

import com.echospace.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 作者信息视图对象
 *
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthorVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;

    public static AuthorVO from(User user) {
        return new AuthorVO(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getAvatar()
        );
    }
}
