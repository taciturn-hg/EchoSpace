package com.echospace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.echospace.entity.Post;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 帖子 Mapper，提供游标分页列表查询、详情多表联查、浏览数自增等自定义 SQL
 *
 * @Author: taciturn-hg
 */
public interface PostMapper extends BaseMapper<Post> {

    /**
     * 按最新排序游标分页查询帖子列表
     *
     * @param cursorTime 上一页最后一条的创建时间
     * @param cursorId   上一页最后一条的帖子 ID
     * @param size       查询条数（实际取 size+1 用于判断 hasMore）
     */
    List<PostItemVO> selectListLatest(@Param("cursorTime") LocalDateTime cursorTime,
                                      @Param("cursorId") Long cursorId,
                                      @Param("size") int size);

    /**
     * 按热门排序游标分页查询帖子列表
     *
     * @param cursorCount 上一页最后一条的点赞数
     * @param cursorId    上一页最后一条的帖子 ID
     * @param size        查询条数（实际取 size+1 用于判断 hasMore）
     */
    List<PostItemVO> selectListHot(@Param("cursorCount") Integer cursorCount,
                                   @Param("cursorId") Long cursorId,
                                   @Param("size") int size);

    /**
     * 查询帖子详情，含作者信息、当前用户点赞/收藏/关注状态（多表 LEFT JOIN）
     *
     * @param id     帖子 ID
     * @param userId 当前登录用户 ID
     */
    PostDetailVO selectDetailWithAuthor(@Param("id") Long id,
                                        @Param("userId") Long userId);

    /**
     * 帖子浏览数 +1
     *
     * @param id 帖子 ID
     */
    int incrementViewCount(@Param("id") Long id);

    /**
     * 帖子评论数 +1（发布评论时调用）
     *
     * @param id 帖子 ID
     */
    int incrementCommentCount(@Param("id") Long id);

    /**
     * 帖子评论数 -delta（删除评论时调用）
     *
     * @param id    帖子 ID
     * @param delta 减少的数量
     */
    int decrementCommentCount(@Param("id") Long id, @Param("delta") int delta);
}
