package com.echospace.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 二级回复 VO
 *
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReplyVO {
    private Long id;
    private Long postId;
    private Long parentId;
    private CommentUserVO user;
    private ReplyToUserVO replyToUser;
    private String content;
    private int likeCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
}
