package com.echospace.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 一级评论 VO，含预加载的二级回复列表
 *
 * @Author: taciturn-hg
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentVO {
    private Long id;
    private Long postId;
    private CommentUserVO user;
    private String content;
    private int likeCount;
    private boolean isLiked;
    private LocalDateTime createdAt;
    /** 预加载的二级回复列表 */
    private List<ReplyVO> replies;
    /** 该评论的二级回复总数 */
    private int replyCount;
    /** 是否还有更多二级回复 */
    private boolean hasMoreReplies;
}
