package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.dto.CreateCommentDTO;
import com.echospace.service.CommentService;
import com.echospace.vo.CommentVO;
import com.echospace.vo.CreateCommentVO;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.LikeCommentVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.ReplyVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 评论模块控制器
 * <p>
 * 提供评论发表、评论列表（游标分页）、二级回复分页查询、评论删除 4 个 REST 端点。
 * 删除操作校验评论所有权，非本人操作返回 403。
 * 删除一级评论时级联软删除其下所有二级回复。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Tag(name = "评论模块", description = "发表评论/回复、评论列表、删除评论")
@RestController
public class CommentController {

    @Autowired
    private CommentService commentService;

    /**
     * 发表一级评论或二级回复
     */
    @Operation(summary = "发表评论/回复", description = "对帖子发表一级评论（parentId=0）或对评论进行二级回复（parentId=非0）")
    @PostMapping("/posts/{postId}/comments")
    public Result<CreateCommentVO> create(@PathVariable Long postId,
                                          @Valid @RequestBody CreateCommentDTO dto) {
        CreateCommentVO vo = commentService.createComment(postId, dto);
        return Result.success(vo, "评论成功");
    }

    /**
     * 评论列表（游标分页，含预加载二级回复）
     */
    @Operation(summary = "评论列表", description = "游标分页查询帖子的一级评论，每条附带预加载的二级回复")
    @GetMapping("/posts/{postId}/comments")
    public Result<CursorPageVO<CommentVO>> list(
            @PathVariable Long postId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "3") int replySize) {
        CursorPageVO<CommentVO> page = commentService.listComments(postId, cursor, size, replySize);
        return Result.success(page);
    }

    /**
     * 查询更多二级回复（页码分页）
     */
    @Operation(summary = "更多二级回复", description = "分页查询指定一级评论下的更多二级回复")
    @GetMapping("/comments/{commentId}/replies")
    public Result<PageVO<ReplyVO>> listReplies(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        PageVO<ReplyVO> page = commentService.listReplies(commentId, current, size);
        return Result.success(page);
    }

    /**
     * 评论点赞/取消（toggle 模式），对应 API 4.5
     * <p>已点赞则取消，未点赞则点赞。仅返回 liked 状态，前端本地 ±1 更新 UI。</p>
     *
     * @param id 评论 ID
     * @return 点赞状态
     */
    @Operation(summary = "评论点赞/取消", description = "toggle 模式：已点赞则取消，未点赞则点赞，返回操作后的点赞状态")
    @PostMapping("/comments/{id}/like")
    public Result<LikeCommentVO> like(@PathVariable Long id) {
        log.info("评论点赞请求 commentId={}", id);
        LikeCommentVO vo = commentService.likeComment(id);
        return Result.success(vo, vo.isLiked() ? "点赞成功" : "已取消点赞");
    }

    /**
     * 删除评论（软删除，仅限本人）
     */
    @Operation(summary = "删除评论", description = "软删除评论，只能删除自己的评论。删除一级评论时其下二级回复一并软删除")
    @DeleteMapping("/comments/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        commentService.deleteComment(id);
        return Result.success("删除成功");
    }
}
