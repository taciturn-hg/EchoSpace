package com.echospace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.echospace.common.BusinessException;
import com.echospace.dto.CreateCommentDTO;
import com.echospace.entity.Comment;
import com.echospace.entity.Post;
import com.echospace.entity.UserLike;
import com.echospace.mapper.CommentMapper;
import com.echospace.mapper.PostMapper;
import com.echospace.mapper.UserLikeMapper;
import com.echospace.security.SecurityUtil;
import com.echospace.service.CommentService;
import com.echospace.vo.CommentVO;
import com.echospace.vo.CreateCommentVO;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.LikeCommentVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.ReplyVO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * 评论业务实现
 * <p>
 * 核心设计要点：
 * <ul>
 *   <li>一级评论使用游标分页，格式为 {@code {timestamp}_{id}}</li>
 *   <li>二级回复预加载使用简单 LIMIT 查询，随一级评论一并组装</li>
 *   <li>加载更多回复使用 MyBatis-Plus 传统页码分页</li>
 *   <li>删除一级评论时级联软删除其下所有二级回复</li>
 * </ul>
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Service
public class CommentServiceImpl implements CommentService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    private final CommentMapper commentMapper;
    private final PostMapper postMapper;
    private final UserLikeMapper userLikeMapper;

    public CommentServiceImpl(CommentMapper commentMapper,
                              PostMapper postMapper,
                              UserLikeMapper userLikeMapper) {
        this.commentMapper = commentMapper;
        this.postMapper = postMapper;
        this.userLikeMapper = userLikeMapper;
    }

    @Override
    public CreateCommentVO createComment(Long postId, CreateCommentDTO dto) {
        Long userId = requireCurrentUserId();

        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() != 1) {
            log.warn("评论失败：帖子不存在 postId={}", postId);
            throw BusinessException.notFound("帖子不存在");
        }

        Long parentId = dto.getParentId();

        if (parentId != 0) {
            Comment parentComment = commentMapper.selectById(parentId);
            if (parentComment == null || parentComment.getStatus() != 1) {
                log.warn("评论失败：被回复的评论不存在 parentId={}", parentId);
                throw BusinessException.notFound("被回复的评论不存在");
            }
            if (!parentComment.getPostId().equals(postId)) {
                log.warn("评论失败：回复所属帖子不匹配 parentId={}, postId={}", parentId, postId);
                throw new BusinessException("回复所属帖子不匹配");
            }
            if (parentComment.getParentId() != 0) {
                log.warn("评论失败：不能对二级回复进行回复 parentId={}", parentId);
                throw new BusinessException("只能回复一级评论");
            }
        }

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setParentId(parentId);
        comment.setReplyToUid(dto.getReplyToUid());
        comment.setContent(dto.getContent().trim());
        comment.setLikeCount(0);
        comment.setStatus(1);

        commentMapper.insert(comment);
        postMapper.incrementCommentCount(postId);

        log.info("评论发表成功 userId={}, postId={}, commentId={}, parentId={}",
                userId, postId, comment.getId(), parentId);
        return new CreateCommentVO(comment.getId());
    }

    @Override
    public CursorPageVO<CommentVO> listComments(Long postId, String cursor, int size, int replySize) {
        Long userId = requireCurrentUserId();

        LocalDateTime cursorTime = null;
        Long cursorId = null;
        if (cursor != null && !cursor.isEmpty()) {
            CursorParts parts = parseCursor(cursor);
            long epochMilli = parts.first();
            cursorTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE);
            cursorId = parts.second();
        }

        List<CommentVO> records = commentMapper.selectTopLevelComments(
                postId, cursorTime, cursorId, userId, size + 1);

        boolean hasMore = records.size() > size;
        if (hasMore) {
            records.remove(size);
        }

        String nextCursor = null;
        if (!records.isEmpty()) {
            for (CommentVO comment : records) {
                int totalReplies = commentMapper.countReplies(comment.getId());
                List<ReplyVO> replies = commentMapper.selectReplies(comment.getId(), userId, replySize);
                comment.setReplies(replies != null ? replies : new ArrayList<>());
                comment.setReplyCount(totalReplies);
                comment.setHasMoreReplies(totalReplies > replySize);
            }

            if (hasMore) {
                CommentVO last = records.get(records.size() - 1);
                long epochMilli = last.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli();
                nextCursor = epochMilli + "_" + last.getId();
            }
        }

        CursorPageVO<CommentVO> page = new CursorPageVO<>();
        page.setRecords(records);
        page.setCursor(nextCursor);
        page.setHasMore(hasMore);
        page.setCount(records.size());
        return page;
    }

    @Override
    public PageVO<ReplyVO> listReplies(Long commentId, int current, int size) {
        Long userId = requireCurrentUserId();

        Comment parentComment = commentMapper.selectById(commentId);
        if (parentComment == null || parentComment.getStatus() != 1) {
            log.warn("查询回复失败：评论不存在 commentId={}", commentId);
            throw BusinessException.notFound("评论不存在");
        }
        if (parentComment.getParentId() != 0) {
            log.warn("查询回复失败：只能查询一级评论的回复 commentId={}", commentId);
            throw new BusinessException("只能查询一级评论的回复");
        }

        Page<ReplyVO> page = new Page<>(current, size);
        Page<ReplyVO> result = commentMapper.selectRepliesPage(page, commentId, userId);
        return PageVO.of(result);
    }

    @Override
    public void deleteComment(Long commentId) {
        Long userId = requireCurrentUserId();

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            log.warn("删除评论失败：评论不存在 commentId={}", commentId);
            throw BusinessException.notFound("评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            log.warn("删除评论失败：非本人操作 userId={}, commentId={}", userId, commentId);
            throw BusinessException.forbidden("只能删除自己的评论");
        }

        comment.setStatus(-1);
        commentMapper.updateById(comment);

        int deletedCount = 1;

        if (comment.getParentId() == 0) {
            List<Comment> replies = commentMapper.selectList(
                    new LambdaQueryWrapper<Comment>()
                            .eq(Comment::getParentId, commentId)
                            .eq(Comment::getStatus, 1));
            for (Comment reply : replies) {
                reply.setStatus(-1);
                commentMapper.updateById(reply);
                deletedCount++;
            }
            log.info("一级评论已删除，级联删除{}条二级回复 commentId={}", replies.size(), commentId);
        }

        postMapper.decrementCommentCount(comment.getPostId(), deletedCount);
        log.info("评论删除完成 userId={}, commentId={}, deletedCount={}", userId, commentId, deletedCount);
    }

    /**
     * 对评论进行点赞或取消点赞（toggle 模式），对应 API 4.5
     * <p>
     * targetType=2 表示评论点赞。
     * 已点赞则删除记录并减一计数，未点赞则插入记录并加一计数。
     * 仅返回操作后的点赞状态，前端本地 ±1 更新 UI，具体数据在刷新时同步。
     * </p>
     */
    @Override
    @Transactional
    public LikeCommentVO likeComment(Long commentId) {
        Long userId = requireCurrentUserId();

        Comment comment = commentMapper.selectById(commentId);
        if (comment == null || comment.getStatus() != 1) {
            log.warn("点赞评论失败：评论不存在 commentId={}", commentId);
            throw BusinessException.notFound("评论不存在");
        }

        LambdaQueryWrapper<UserLike> query = new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, 2)
                .eq(UserLike::getTargetId, commentId);
        UserLike existing = userLikeMapper.selectOne(query);

        if (existing != null) {
            userLikeMapper.deleteById(existing.getId());
            commentMapper.decrementLikeCount(commentId);
            log.info("取消评论点赞 userId={}, commentId={}", userId, commentId);
            return new LikeCommentVO(false);
        }

        UserLike like = new UserLike();
        like.setUserId(userId);
        like.setTargetType(2);
        like.setTargetId(commentId);
        try {
            userLikeMapper.insert(like);
        } catch (DuplicateKeyException e) {
            log.info("评论点赞已存在（并发冲突） userId={}, commentId={}", userId, commentId);
            return new LikeCommentVO(true);
        }
        commentMapper.incrementLikeCount(commentId);
        log.info("评论点赞成功 userId={}, commentId={}", userId, commentId);
        return new LikeCommentVO(true);
    }

    // ---------- 内部辅助 ----------

    /**
     * 从 SecurityContext 获取当前登录用户 ID，未登录则抛出 401
     */
    private Long requireCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            log.warn("未登录或登录已过期");
            throw BusinessException.unauthorized("未登录或登录已过期");
        }
        return userId;
    }

    /**
     * 游标解析结果：first=timestamp，second=评论 ID
     */
    private record CursorParts(long first, long second) {}

    /**
     * 解析并校验游标字符串：{timestamp}_{id}，非法格式或数字解析失败时抛出业务异常
     */
    private CursorParts parseCursor(String cursor) {
        int idx = cursor.lastIndexOf('_');
        if (idx <= 0) {
            throw new BusinessException("游标格式不正确");
        }
        try {
            return new CursorParts(
                    Long.parseLong(cursor.substring(0, idx)),
                    Long.parseLong(cursor.substring(idx + 1))
            );
        } catch (NumberFormatException e) {
            throw new BusinessException("游标格式不正确");
        }
    }
}
