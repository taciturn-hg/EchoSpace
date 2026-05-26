package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.dto.CreatePostDTO;
import com.echospace.dto.UpdatePostDTO;
import com.echospace.service.PostService;
import com.echospace.vo.CreatePostVO;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.PostDetailVO;
import com.echospace.vo.PostItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@Tag(name = "帖子模块", description = "发布、编辑、删除、查询帖子")
@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    private PostService postService;

    @Operation(summary = "发布帖子", description = "发布新帖子，支持富文本 HTML 内容")
    @PostMapping
    public Result<CreatePostVO> create(@Valid @RequestBody CreatePostDTO dto) {
        Long postId = postService.createPost(dto);
        return Result.success(new CreatePostVO(postId), "发布成功");
    }

    @Operation(summary = "帖子详情", description = "根据 ID 查询帖子详情，含作者信息、点赞/收藏/关注状态")
    @GetMapping("/{id}")
    public Result<PostDetailVO> detail(@PathVariable Long id) {
        PostDetailVO detail = postService.getPostDetail(id);
        return Result.success(detail);
    }

    @Operation(summary = "编辑帖子", description = "编辑帖子，只能修改自己的帖子")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @Valid @RequestBody UpdatePostDTO dto) {
        postService.updatePost(id, dto);
        return Result.success("修改成功");
    }

    @Operation(summary = "删除帖子", description = "软删除帖子，只能删除自己的帖子")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        postService.deletePost(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "帖子列表", description = "游标分页查询首页帖子列表，支持最新/热门排序")
    @GetMapping
    public Result<CursorPageVO<PostItemVO>> list(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sort) {
        CursorPageVO<PostItemVO> page = postService.listPosts(cursor, size, sort);
        return Result.success(page);
    }
}
