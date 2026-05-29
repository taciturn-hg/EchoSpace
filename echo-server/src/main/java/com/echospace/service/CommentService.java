package com.echospace.service;

import com.echospace.dto.CreateCommentDTO;
import com.echospace.vo.CommentVO;
import com.echospace.vo.CreateCommentVO;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.ReplyVO;

/**
 * 评论业务接口
 *
 * @Author: taciturn-hg
 */
public interface CommentService {

    /**
     * 发表一级评论或二级回复，同时更新帖子评论数
     *
     * @param postId 帖子 ID
     * @param dto    评论/回复内容
     * @return 新评论 ID
     */
    CreateCommentVO createComment(Long postId, CreateCommentDTO dto);

    /**
     * 游标分页查询帖子的评论列表，每条一级评论预加载若干条二级回复
     *
     * @param postId    帖子 ID
     * @param cursor    上一页游标（格式：{timestamp}_{id}），首页传 null
     * @param size      每页一级评论数
     * @param replySize 每条一级评论预加载的二级回复数
     * @return 游标分页结果
     */
    CursorPageVO<CommentVO> listComments(Long postId, String cursor, int size, int replySize);

    /**
     * 页码分页查询一级评论下的更多二级回复
     *
     * @param commentId 一级评论 ID
     * @param current   页码
     * @param size      每页条数
     * @return 页码分页结果
     */
    PageVO<ReplyVO> listReplies(Long commentId, int current, int size);

    /**
     * 软删除评论（只能删除自己的评论）
     * <p>
     * 删除一级评论时，其下所有状态正常的二级回复也一并软删除，
     * 评论数相应扣减 1 + 被级联删除的回复数。
     * </p>
     *
     * @param commentId 评论 ID
     */
    void deleteComment(Long commentId);
}
