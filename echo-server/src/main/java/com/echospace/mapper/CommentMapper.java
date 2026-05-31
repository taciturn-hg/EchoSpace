package com.echospace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echospace.entity.Comment;
import com.echospace.vo.CommentVO;
import com.echospace.vo.ReplyVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 评论 Mapper，提供游标分页列表、二级回复查询、回复数统计等自定义 SQL
 *
 * @Author: taciturn-hg
 */
public interface CommentMapper extends BaseMapper<Comment> {

    /**
     * 按创建时间倒序游标分页查询帖子的一级评论（parent_id=0）
     *
     * @param postId     帖子 ID
     * @param cursorTime 上一页最后一条的创建时间，首次为 null
     * @param cursorId   上一页最后一条的评论 ID，首次为 null
     * @param userId     当前登录用户 ID（用于判断 isLiked）
     * @param size       查询条数（实际取 size+1 用于判断 hasMore）
     */
    List<CommentVO> selectTopLevelComments(@Param("postId") Long postId,
                                           @Param("cursorTime") LocalDateTime cursorTime,
                                           @Param("cursorId") Long cursorId,
                                           @Param("userId") Long userId,
                                           @Param("size") int size);

    /**
     * 查询指定一级评论下的二级回复，按创建时间升序，取最旧的前 limit 条
     *
     * @param parentId 父评论 ID
     * @param userId   当前登录用户 ID（用于判断 isLiked）
     * @param limit    最多返回条数
     */
    List<ReplyVO> selectReplies(@Param("parentId") Long parentId,
                                @Param("userId") Long userId,
                                @Param("limit") int limit);

    /**
     * 统计指定一级评论下状态正常的二级回复总数
     *
     * @param parentId 父评论 ID
     */
    int countReplies(@Param("parentId") Long parentId);

    /**
     * 页码分页查询二级回复（用于"加载更多回复"）
     *
     * @param page     MyBatis-Plus 分页对象
     * @param parentId 父评论 ID
     * @param userId   当前登录用户 ID
     */
    Page<ReplyVO> selectRepliesPage(Page<ReplyVO> page,
                                     @Param("parentId") Long parentId,
                                     @Param("userId") Long userId);

    /**
     * 评论点赞数 +1（点赞时调用）
     *
     * @param id 评论 ID
     */
    int incrementLikeCount(@Param("id") Long id);

    /**
     * 评论点赞数 -1（取消点赞时调用，使用 GREATEST 防负值）
     *
     * @param id 评论 ID
     */
    int decrementLikeCount(@Param("id") Long id);
}
