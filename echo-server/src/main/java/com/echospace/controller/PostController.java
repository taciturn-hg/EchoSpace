package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.dto.CreatePostDTO;
import com.echospace.dto.UpdatePostDTO;
import com.echospace.service.PostService;
import com.echospace.service.SearchService;
import com.echospace.vo.CreatePostVO;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.FavoritePostVO;
import com.echospace.vo.LikePostVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 帖子模块控制器
 * <p>
 * 提供帖子发布、详情查询、编辑、软删除、游标分页列表、点赞/取消点赞、收藏/取消收藏、搜索 8 个 REST 端点。
 * 编辑和删除操作校验帖子所有权，非本人操作返回 403。
 * 点赞/收藏采用 toggle 模式，同一请求路径反复调用可在开/关状态间切换。
 * 搜索基于 Elasticsearch 全文检索，支持 ik 中文分词和高亮。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Tag(name = "帖子模块", description = "发布、编辑、删除、查询、搜索帖子")
@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;
    private final SearchService searchService;

    public PostController(PostService postService, SearchService searchService) {
        this.postService = postService;
        this.searchService = searchService;
    }

    /**
     * 发布帖子
     */
    @Operation(summary = "发布帖子", description = "发布新帖子，支持富文本 HTML 内容")
    @PostMapping
    public Result<CreatePostVO> create(@Valid @RequestBody CreatePostDTO dto) {
        Long postId = postService.createPost(dto);
        return Result.success(new CreatePostVO(postId), "发布成功");
    }

    /**
     * 帖子详情
     */
    @Operation(summary = "帖子详情", description = "根据 ID 查询帖子详情，含作者信息、点赞/收藏/关注状态")
    @GetMapping("/{id}")
    public Result<PostDetailVO> detail(@PathVariable Long id) {
        PostDetailVO detail = postService.getPostDetail(id);
        return Result.success(detail);
    }

    /**
     * 编辑帖子（仅限本人）
     */
    @Operation(summary = "编辑帖子", description = "编辑帖子，只能修改自己的帖子")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdatePostDTO dto) {
        postService.updatePost(id, dto);
        return Result.success("修改成功");
    }

    /**
     * 软删除帖子（仅限本人）
     */
    @Operation(summary = "删除帖子", description = "软删除帖子，只能删除自己的帖子")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success("删除成功");
    }

    /**
     * 帖子点赞/取消点赞（toggle 模式）
     */
    @Operation(summary = "帖子点赞/取消", description = "对帖子进行点赞或取消点赞，toggle 模式下反复调用可切换状态")
    @PostMapping("/{id}/like")
    public Result<LikePostVO> like(@PathVariable Long id) {
        LikePostVO vo = postService.likePost(id);
        return Result.success(vo, vo.isLiked() ? "点赞成功" : "已取消点赞");
    }

    /**
     * 帖子收藏/取消收藏（toggle 模式）
     */
    @Operation(summary = "帖子收藏/取消", description = "收藏或取消收藏帖子，toggle 模式下反复调用可切换状态")
    @PostMapping("/{id}/favorite")
    public Result<FavoritePostVO> favorite(@PathVariable Long id) {
        FavoritePostVO vo = postService.favoritePost(id);
        return Result.success(vo, vo.isFavorited() ? "收藏成功" : "已取消收藏");
    }

    /**
     * 首页帖子列表（游标分页）
     */
    @Operation(summary = "帖子列表", description = "游标分页查询首页帖子列表，支持最新/热门排序")
    @GetMapping
    public Result<CursorPageVO<PostItemVO>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sort) {
        CursorPageVO<PostItemVO> page = postService.listPosts(cursor, size, sort);
        return Result.success(page);
    }

    /**
     * 搜索帖子
     */
    @Operation(summary = "搜索帖子", description = "通过 Elasticsearch 对帖子标题和正文进行全文搜索，支持 ik 中文分词和高亮")
    @GetMapping("/search")
    public Result<PageVO<PostItemVO>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sort) {
        PageVO<PostItemVO> page = searchService.search(q, current, size, sort);
        return Result.success(page);
    }
}
