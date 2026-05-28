package com.echospace.service;

import com.echospace.dto.CreatePostDTO;
import com.echospace.dto.UpdatePostDTO;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.FavoritePostVO;
import com.echospace.vo.LikePostVO;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;

/**
 * 帖子业务接口
 *
 * @Author: taciturn-hg
 */
public interface PostService {

    /**
     * 发布帖子，自动从 HTML 提取纯文本摘要和封面图
     *
     * @param dto 标题 + 富文本 HTML
     * @return 新帖子 ID
     */
    Long createPost(CreatePostDTO dto);

    /**
     * 查询帖子详情，同时浏览数 +1
     *
     * @param postId 帖子 ID
     * @return 帖子详情（含作者信息、当前用户点赞/收藏/关注状态）
     */
    PostDetailVO getPostDetail(Long postId);

    /**
     * 编辑帖子，需校验所有权（非本人禁止编辑），使用乐观锁防并发冲突
     *
     * @param postId 帖子 ID
     * @param dto    新的标题和富文本内容
     */
    void updatePost(Long postId, UpdatePostDTO dto);

    /**
     * 软删除帖子，需校验所有权
     *
     * @param postId 帖子 ID
     */
    void deletePost(Long postId);

    /**
     * 游标分页查询帖子列表
     *
     * @param cursor 上一页游标（格式：{排序值}_{id}），首页传 null
     * @param size   每页条数
     * @param sort   排序方式：created_at=最新，hot=热门（按点赞数）
     * @return 游标分页结果，含下一页游标和 hasMore 标记
     */
    CursorPageVO<PostItemVO> listPosts(String cursor, int size, String sort);

    /**
     * 帖子点赞/取消点赞（toggle 模式）
     * <p>已点赞则取消（delete + likeCount-1），未点赞则点赞（insert + likeCount+1）。</p>
     *
     * @param postId 帖子 ID
     * @return 操作后的点赞状态和最新点赞数
     */
    LikePostVO likePost(Long postId);

    /**
     * 帖子收藏/取消收藏（toggle 模式）
     * <p>已收藏则取消（delete + collectCount-1），未收藏则收藏（insert + collectCount+1）。</p>
     *
     * @param postId 帖子 ID
     * @return 操作后的收藏状态
     */
    FavoritePostVO favoritePost(Long postId);
}
