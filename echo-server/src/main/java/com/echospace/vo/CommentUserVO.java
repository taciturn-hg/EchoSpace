package com.echospace.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 评论/回复的用户信息 VO
 *
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentUserVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatar;
}
