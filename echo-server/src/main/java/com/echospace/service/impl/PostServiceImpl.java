package com.echospace.service.impl;

import com.echospace.common.BusinessException;
import com.echospace.document.PostDocument;
import com.echospace.dto.CreatePostDTO;
import com.echospace.dto.UpdatePostDTO;
import com.echospace.entity.Post;
import com.echospace.entity.User;
import com.echospace.entity.UserFavorite;
import com.echospace.entity.UserLike;
import com.echospace.mapper.PostMapper;
import com.echospace.mapper.UserFavoriteMapper;
import com.echospace.mapper.UserLikeMapper;
import com.echospace.mapper.UserMapper;
import com.echospace.security.SecurityUtil;
import com.echospace.service.PostService;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.FavoritePostVO;
import com.echospace.vo.LikePostVO;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.NoSuchIndexException;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 帖子业务实现
 * <p>
 * 核心设计要点：
 * <ul>
 *   <li>游标分页使用 SQL keyset pagination，游标编码格式为 {@code {排序值}_{id}}</li>
 *   <li>编辑操作使用 MyBatis-Plus 乐观锁（version 字段），并发冲突时提示用户刷新</li>
 *   <li>Jsoup 提取富文本 HTML 中的纯文本摘要和首张封面图</li>
 *   <li>点赞/收藏采用 toggle 模式：已操作则取消（delete + count-1），未操作则执行（insert + count+1）</li>
 *   <li>计数更新使用 SQL GREATEST 函数防负值</li>
 *   <li>帖子发布/更新时同步写入 Elasticsearch 索引，try-catch 兜底，ES 失败不影响 MySQL 主流程</li>
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

    @Autowired
    private UserLikeMapper userLikeMapper;

    @Autowired
    private UserFavoriteMapper userFavoriteMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ElasticsearchOperations esOps;

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
        syncPostToEs(post.getId(), userId);
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
        syncPostToEs(postId, post.getUserId());
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
        Long userId = SecurityUtil.getCurrentUserId();
        // 未登录时传 0，LEFT JOIN 匹配不到即 IF(NULL) → FALSE
        long uid = userId != null ? userId : 0L;

        List<PostItemVO> records;
        if ("hot".equals(sort)) {
            Integer cursorCount = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                CursorParts parts = parseCursor(cursor);
                cursorCount = (int) parts.first();
                cursorId = parts.second();
            }
            records = postMapper.selectListHot(uid, cursorCount, cursorId, size + 1);
        } else {
            LocalDateTime cursorTime = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                CursorParts parts = parseCursor(cursor);
                long epochMilli = parts.first();
                cursorTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE);
                cursorId = parts.second();
            }
            records = postMapper.selectListLatest(uid, cursorTime, cursorId, size + 1);
        }

        return buildCursorPage(records, size, sort);
    }

    /**
     * 查询指定用户发布的帖子列表（API 2.7），游标分页逻辑与 {@link #listPosts} 一致
     */
    @Override
    public CursorPageVO<PostItemVO> listUserPosts(Long targetUserId, String cursor, int size, String sort) {
        Long userId = SecurityUtil.getCurrentUserId();
        long uid = userId != null ? userId : 0L;

        List<PostItemVO> records;
        if ("hot".equals(sort)) {
            Integer cursorCount = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                CursorParts parts = parseCursor(cursor);
                cursorCount = (int) parts.first();
                cursorId = parts.second();
            }
            records = postMapper.selectListByUserHot(uid, targetUserId, cursorCount, cursorId, size + 1);
        } else {
            LocalDateTime cursorTime = null;
            Long cursorId = null;
            if (cursor != null && !cursor.isEmpty()) {
                CursorParts parts = parseCursor(cursor);
                long epochMilli = parts.first();
                cursorTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMilli), ZONE);
                cursorId = parts.second();
            }
            records = postMapper.selectListByUserLatest(uid, targetUserId, cursorTime, cursorId, size + 1);
        }

        log.debug("用户帖子列表查询完成 userId={}, sort={}, count={}", targetUserId, sort, records.size());
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

    @Override
    public LikePostVO likePost(Long postId) {
        Long userId = requireCurrentUserId();
        Post post = findPostOrThrow(postId);

        LambdaQueryWrapper<UserLike> query = new LambdaQueryWrapper<UserLike>()
                .eq(UserLike::getUserId, userId)
                .eq(UserLike::getTargetType, 1)
                .eq(UserLike::getTargetId, postId);
        UserLike existing = userLikeMapper.selectOne(query);

        if (existing != null) {
            userLikeMapper.deleteById(existing.getId());
            postMapper.decrementLikeCount(postId);
            log.info("取消点赞 userId={}, postId={}, likeCount={}", userId, postId, post.getLikeCount() - 1);
            return new LikePostVO(false, post.getLikeCount() - 1);
        } else {
            UserLike like = new UserLike();
            like.setUserId(userId);
            like.setTargetType(1);
            like.setTargetId(postId);
            userLikeMapper.insert(like);
            postMapper.incrementLikeCount(postId);
            log.info("点赞成功 userId={}, postId={}, likeCount={}", userId, postId, post.getLikeCount() + 1);
            return new LikePostVO(true, post.getLikeCount() + 1);
        }
    }

    @Override
    public FavoritePostVO favoritePost(Long postId) {
        Long userId = requireCurrentUserId();
        findPostOrThrow(postId);

        LambdaQueryWrapper<UserFavorite> query = new LambdaQueryWrapper<UserFavorite>()
                .eq(UserFavorite::getUserId, userId)
                .eq(UserFavorite::getPostId, postId);
        UserFavorite existing = userFavoriteMapper.selectOne(query);

        if (existing != null) {
            userFavoriteMapper.deleteById(existing.getId());
            postMapper.decrementCollectCount(postId);
            log.info("取消收藏 userId={}, postId={}", userId, postId);
            return new FavoritePostVO(false);
        } else {
            UserFavorite favorite = new UserFavorite();
            favorite.setUserId(userId);
            favorite.setPostId(postId);
            userFavoriteMapper.insert(favorite);
            postMapper.incrementCollectCount(postId);
            log.info("收藏成功 userId={}, postId={}", userId, postId);
            return new FavoritePostVO(true);
        }
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

    /**
     * 将帖子同步写入 Elasticsearch 索引
     * <p>
     * try-catch 兜底，ES 写入失败不影响 MySQL 主流程，仅记录 warn 日志。
     * 后续引入消息队列后可改为异步投递。
     * </p>
     *
     * @param postId 帖子 ID
     * @param userId 帖子作者 ID
     */
    private void syncPostToEs(Long postId, Long userId) {
        try {
            Post post = postMapper.selectById(postId);
            if (post == null || post.getStatus() == 0) {
                return;
            }
            User user = userMapper.selectById(userId);
            if (user == null) {
                return;
            }
            PostDocument doc = buildPostDocument(post, user);
            esOps.save(doc);
            log.info("ES 同步成功 postId={}", postId);
        } catch (NoSuchIndexException e) {
            log.warn("ES 同步跳过：索引 posts 尚未创建，将在下次发帖时由自动配置创建");
        } catch (Exception e) {
            log.warn("ES 同步失败 postId={}, error={}", postId, e.getMessage());
        }
    }

    /**
     * 将 MySQL 实体（Post + User）组装为 ES 文档
     */
    private PostDocument buildPostDocument(Post post, User user) {
        return PostDocument.builder()
                .id(post.getId())
                .title(post.getTitle())
                .contentText(post.getContentText())
                .coverImage(post.getCoverImage())
                .userId(post.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatar(user.getAvatar())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .collectCount(post.getCollectCount())
                .createdAt(post.getCreatedAt().atZone(ZONE).toInstant().toEpochMilli())
                .build();
    }

    @Override
    public int syncAllPostsToEs() {
        List<Post> posts = postMapper.selectList(
                new LambdaQueryWrapper<Post>().eq(Post::getStatus, 1));

        if (posts.isEmpty()) {
            log.info("ES 全量同步：数据库中没有帖子");
            return 0;
        }

        Set<Long> userIds = posts.stream().map(Post::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        int success = 0;
        int fail = 0;
        for (Post post : posts) {
            try {
                User user = userMap.get(post.getUserId());
                if (user == null) {
                    log.warn("ES 同步跳过：作者不存在 postId={}, userId={}", post.getId(), post.getUserId());
                    continue;
                }
                PostDocument doc = buildPostDocument(post, user);
                esOps.save(doc);
                success++;
            } catch (NoSuchIndexException e) {
                log.warn("ES 同步跳过：索引 posts 尚未创建");
            } catch (Exception e) {
                fail++;
                log.warn("ES 同步失败 postId={}, error={}", post.getId(), e.getMessage());
            }
        }

        log.info("ES 全量同步完成 total={}, success={}, fail={}", posts.size(), success, fail);
        return success;
    }
}
