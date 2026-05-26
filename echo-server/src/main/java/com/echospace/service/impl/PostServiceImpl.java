package com.echospace.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.echospace.common.BusinessException;
import com.echospace.dto.CreatePostDTO;
import com.echospace.dto.UpdatePostDTO;
import com.echospace.entity.Post;
import com.echospace.mapper.PostMapper;
import com.echospace.security.SecurityUtil;
import com.echospace.service.PostService;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    @Autowired
    private PostMapper postMapper;

    @Override
    public Long createPost(CreatePostDTO dto) {
        Long userId = requireCurrentUserId();

        String contentText = extractText(dto.getContentHtml());
        String coverImage = extractCoverImage(dto.getContentHtml());

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle());
        post.setContentHtml(dto.getContentHtml());
        post.setContentText(contentText);
        post.setCoverImage(coverImage);
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setCollectCount(0);
        post.setViewCount(0);
        post.setStatus(1);
        post.setIsPinned(0);

        postMapper.insert(post);
        log.info("帖子发布成功 userId={}, postId={}, title={}", userId, post.getId(), dto.getTitle());
        return post.getId();
    }

    @Override
    public PostDetailVO getPostDetail(Long postId) {
        Long userId = requireCurrentUserId();
        PostDetailVO detail = postMapper.selectDetailWithAuthor(postId, userId);
        if (detail == null) {
            log.warn("帖子不存在 postId={}", postId);
            throw BusinessException.notFound("帖子不存在");
        }

        postMapper.incrementViewCount(postId);
        detail.setViewCount(detail.getViewCount() + 1);

        log.info("帖子详情查询完成 postId={}, viewCount={}", postId, detail.getViewCount());
        return detail;
    }

    @Override
    public void updatePost(Long postId, UpdatePostDTO dto) {
        Long userId = requireCurrentUserId();
        Post post = findPostOrThrow(postId);
        if (!post.getUserId().equals(userId)) {
            log.warn("编辑帖子失败：非本人操作 userId={}, postId={}", userId, postId);
            throw BusinessException.forbidden("只能编辑自己的帖子");
        }

        Post update = new Post();
        update.setId(postId);
        update.setVersion(post.getVersion());
        update.setTitle(dto.getTitle());
        update.setContentHtml(dto.getContentHtml());
        update.setContentText(extractText(dto.getContentHtml()));
        update.setCoverImage(extractCoverImage(dto.getContentHtml()));

        int rows = postMapper.updateById(update);
        if (rows == 0) {
            log.warn("编辑帖子并发冲突 postId={}", postId);
            throw BusinessException.conflict("帖子已被他人修改，请刷新后重试");
        }
        log.info("帖子编辑完成 userId={}, postId={}", userId, postId);
    }

    @Override
    public void deletePost(Long postId) {
        Long userId = requireCurrentUserId();
        Post post = findPostOrThrow(postId);
        if (!post.getUserId().equals(userId)) {
            log.warn("删除帖子失败：非本人操作 userId={}, postId={}", userId, postId);
            throw BusinessException.forbidden("只能删除自己的帖子");
        }

        Post update = new Post();
        update.setId(postId);
        update.setStatus(0);
        postMapper.updateById(update);
        log.info("帖子已删除 userId={}, postId={}", userId, postId);
    }

    @Override
    public CursorPageVO<PostItemVO> listPosts(String cursor, int size, String sort) {
        List<PostItemVO> records;
        if ("hot".equals(sort)) {
            Integer cursorCount = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                String[] parts = parseCursor(cursor);
                cursorCount = Integer.parseInt(parts[0]);
                cursorId = Long.parseLong(parts[1]);
            }
            records = postMapper.selectListHot(cursorCount, cursorId, size + 1);
        } else {
            LocalDateTime cursorTime = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                String[] parts = parseCursor(cursor);
                long epochMilli = Long.parseLong(parts[0]);
                cursorTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE);
                cursorId = Long.parseLong(parts[1]);
            }
            records = postMapper.selectListLatest(cursorTime, cursorId, size + 1);
        }

        boolean hasMore = records.size() > size;
        if (hasMore) {
            records.remove(size);
        }

        String nextCursor = null;
        if (hasMore && !records.isEmpty()) {
            PostItemVO last = records.get(records.size() - 1);
            nextCursor = buildCursor(last, sort);
        }

        CursorPageVO<PostItemVO> page = new CursorPageVO<>();
        page.setRecords(records);
        page.setCursor(nextCursor);
        page.setHasMore(hasMore);
        page.setSize(records.size());
        return page;
    }

    // ---------- 内部辅助 ----------

    private Long requireCurrentUserId() {
        Long userId = SecurityUtil.getCurrentUserId();
        if (userId == null) {
            log.warn("未登录或登录已过期");
            throw BusinessException.unauthorized("未登录或登录已过期");
        }
        return userId;
    }

    private Post findPostOrThrow(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            log.warn("帖子不存在 postId={}", postId);
            throw BusinessException.notFound("帖子不存在");
        }
        return post;
    }

    private String extractText(String html) {
        Document doc = Jsoup.parse(html);
        return doc.text();
    }

    private String extractCoverImage(String html) {
        Document doc = Jsoup.parse(html);
        Element firstImg = doc.select("img").first();
        return firstImg != null ? firstImg.attr("src") : null;
    }

    private String[] parseCursor(String cursor) {
        int idx = cursor.lastIndexOf('_');
        if (idx <= 0) {
            throw new BusinessException("游标格式不正确");
        }
        return new String[]{cursor.substring(0, idx), cursor.substring(idx + 1)};
    }

    private String buildCursor(PostItemVO item, String sort) {
        if ("hot".equals(sort)) {
            return item.getLikeCount() + "_" + item.getId();
        }
        long epochMilli = item.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli();
        return epochMilli + "_" + item.getId();
    }
}
