package com.echospace.service.impl;

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
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

/**
 * 帖子业务实现
 * <p>
 * 核心设计要点：
 * <ul>
 *   <li>游标分页使用 SQL keyset pagination，游标编码格式为 {@code {排序值}_{id}}</li>
 *   <li>编辑操作使用 MyBatis-Plus 乐观锁（version 字段），并发冲突时提示用户刷新</li>
 *   <li>Jsoup 提取富文本 HTML 中的纯文本摘要和首张封面图</li>
 * </ul>
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Service
public class PostServiceImpl implements PostService {

    private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");

    /**
     * 帖子富文本 HTML 清洗白名单：
     * 允许 TipTap StarterKit + Image + Link 生成的标签和属性，
     * img/src 和 a/href 仅允许 http/https/mailto/tel 协议。
     */
    private static final Safelist POST_SAFELIST;

    static {
        POST_SAFELIST = new Safelist()
                .addTags("p", "br", "strong", "em", "s", "u",
                        "h1", "h2", "h3", "h4",
                        "blockquote", "pre", "code",
                        "ul", "ol", "li",
                        "hr", "img", "a")
                .addAttributes("img", "src", "alt")
                .addAttributes("a", "href", "rel")
                .addAttributes("pre", "class")
                .addAttributes("code", "class")
                .addProtocols("img", "src", "http", "https")
                .addProtocols("a", "href", "http", "https", "mailto", "tel");
    }

    @Autowired
    private PostMapper postMapper;

    @Override
    public Long createPost(CreatePostDTO dto) {
        Long userId = requireCurrentUserId();

        String safeHtml = sanitizeHtml(dto.getContentHtml());
        String contentText = extractText(safeHtml);
        String coverImage = extractCoverImage(safeHtml);

        Post post = new Post();
        post.setUserId(userId);
        post.setTitle(dto.getTitle());
        post.setContentHtml(safeHtml);
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

        String safeHtml = sanitizeHtml(dto.getContentHtml());
        update.setContentHtml(safeHtml);
        update.setContentText(extractText(safeHtml));
        update.setCoverImage(extractCoverImage(safeHtml));

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
                CursorParts parts = parseCursor(cursor);
                cursorCount = (int) parts.first();
                cursorId = parts.second();
            }
            records = postMapper.selectListHot(cursorCount, cursorId, size + 1);
        } else {
            LocalDateTime cursorTime = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                CursorParts parts = parseCursor(cursor);
                long epochMilli = parts.first();
                cursorTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE);
                cursorId = parts.second();
            }
            records = postMapper.selectListLatest(cursorTime, cursorId, size + 1);
        }

        return buildCursorPage(records, size, sort);
    }

    /**
     * 查询指定用户发布的帖子列表（API 2.7），游标分页逻辑与 {@link #listPosts} 一致
     */
    @Override
    public CursorPageVO<PostItemVO> listUserPosts(Long userId, String cursor, int size, String sort) {
        List<PostItemVO> records;
        if ("hot".equals(sort)) {
            Integer cursorCount = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                CursorParts parts = parseCursor(cursor);
                cursorCount = (int) parts.first();
                cursorId = parts.second();
            }
            records = postMapper.selectListByUserHot(userId, cursorCount, cursorId, size + 1);
        } else {
            LocalDateTime cursorTime = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                CursorParts parts = parseCursor(cursor);
                long epochMilli = parts.first();
                cursorTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE);
                cursorId = parts.second();
            }
            records = postMapper.selectListByUserLatest(userId, cursorTime, cursorId, size + 1);
        }

        log.debug("用户帖子列表查询完成 userId={}, sort={}, count={}", userId, sort, records.size());
        return buildCursorPage(records, size, sort);
    }

    // ---------- 内部辅助 ----------

    /**
     * 对游标分页查询结果（size+1 条）进行裁剪，并构造下一页游标
     *
     * @param records 查询结果（size+1 条）
     * @param size    前端请求的每页条数
     * @param sort    排序方式，用于决定游标编码格式
     * @return 裁剪后的游标分页结果
     */
    private CursorPageVO<PostItemVO> buildCursorPage(List<PostItemVO> records, int size, String sort) {
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
        page.setCount(records.size());
        return page;
    }

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
     * 根据 ID 查询帖子，不存在或已软删除则抛出 404
     */
    private Post findPostOrThrow(Long postId) {
        Post post = postMapper.selectById(postId);
        if (post == null || post.getStatus() == 0) {
            log.warn("帖子不存在 postId={}", postId);
            throw BusinessException.notFound("帖子不存在");
        }
        return post;
    }

    /**
     * 使用 Jsoup + 白名单清洗富文本 HTML，移除危险标签和非法协议。
     * 调用方保证 html 非空。
     */
    private String sanitizeHtml(String html) {
        return Jsoup.clean(html, POST_SAFELIST);
    }

    /**
     * 使用 Jsoup 从富文本 HTML 提取纯文本摘要
     */
    private String extractText(String html) {
        Document doc = Jsoup.parse(html);
        return doc.text();
    }

    /**
     * 使用 Jsoup 从富文本 HTML 提取首张图片地址作为封面
     */
    private String extractCoverImage(String html) {
        Document doc = Jsoup.parse(html);
        Element firstImg = doc.select("img").first();
        return firstImg != null ? firstImg.attr("src") : null;
    }

    /**
     * 游标解析结果：first=排序值（timestamp 或 likeCount），second=帖子 ID
     */
    private record CursorParts(long first, long second) {}

    /**
     * 解析并校验游标字符串：{first}_{second}，非法格式或数字解析失败时抛出业务异常
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

    /**
     * 构造下一页游标：最新→{timestamp}_{id}，热门→{likeCount}_{id}
     */
    private String buildCursor(PostItemVO item, String sort) {
        if ("hot".equals(sort)) {
            return item.getLikeCount() + "_" + item.getId();
        }
        long epochMilli = item.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli();
        return epochMilli + "_" + item.getId();
    }
}
