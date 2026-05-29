package com.echospace.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 二级回复中"被回复的用户"信息 VO（不含头像）
 *
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyToUserVO {
    private Long id;
    private String username;
    private String nickname;
}
