package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.service.PostService;
import com.echospace.service.UserService;
import com.echospace.vo.CursorPageVO;
import com.echospace.vo.FollowItemVO;
import com.echospace.vo.FollowVO;
import com.echospace.vo.PageVO;
import com.echospace.vo.PostItemVO;
import com.echospace.vo.PublicUserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开可访问的用户信息控制器（无需登录即可调用）
 * <p>
 * 与 {@link UserController}（{@code /users/me}）的职责划分：
 * UserController 负责当前登录用户的自我管理（资料设置、账号设置、修改密码），需要认证；
 * 本控制器负责公开用户信息的查询（个人主页、用户帖子列表），无需登录。
 * </p>
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Tag(name = "用户公开信息", description = "无需登录：查询用户信息、用户帖子列表")
@RestController
@RequestMapping("/users")
public class UserPublicController {

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    /**
     * 获取指定用户的公开信息（个人主页），对应 API 2.1
     * <p>返回用户基本信息、发帖数、粉丝数、关注数；已登录时额外标记是否已关注。</p>
     *
     * @param id 用户 ID
     * @return 公开用户信息
     */
    @Operation(summary = "获取用户信息（个人主页）", description = "查询指定用户的公开信息，包含发帖数/粉丝数/关注数/是否已关注")
    @GetMapping("/{id:[0-9]+}")
    public Result<PublicUserVO> getProfile(@PathVariable Long id) {
        log.info("获取用户公开信息请求 targetUserId={}", id);
        PublicUserVO profile = userService.getPublicProfile(id);
        return Result.success(profile);
    }

    /**
     * 关注或取消关注指定用户（toggle 模式），对应 API 2.8
     * <p>已关注则取消，未关注则关注。需认证，不允许关注自己。</p>
     *
     * @param id 被关注的用户 ID
     * @return 关注状态
     */
    @Operation(summary = "关注/取消关注用户", description = "toggle 模式：已关注则取消，未关注则关注，不允许关注自己")
    @PostMapping("/{id:[0-9]+}/follow")
    public Result<FollowVO> follow(@PathVariable Long id) {
        log.info("关注/取消关注请求 targetUserId={}", id);
        FollowVO vo = userService.followUser(id);
        return Result.success(vo, vo.isFollowed() ? "关注成功" : "已取消关注");
    }

    /**
     * 分页查询指定用户的粉丝列表，对应 API 2.9
     * <p>按关注时间倒序排列，返回粉丝基本信息。</p>
     *
     * @param id      用户 ID
     * @param current 页码，默认 1
     * @param size    每页条数，默认 10
     * @return 粉丝列表分页结果
     */
    @Operation(summary = "粉丝列表", description = "分页查询指定用户的粉丝列表，按关注时间倒序")
    @GetMapping("/{id:[0-9]+}/followers")
    public Result<PageVO<FollowItemVO>> listFollowers(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        log.info("查询粉丝列表请求 targetUserId={}, current={}, size={}", id, current, size);
        PageVO<FollowItemVO> page = userService.listFollowers(id, current, size);
        return Result.success(page);
    }

    /**
     * 分页查询指定用户关注的人的列表，对应 API 2.10
     * <p>按关注时间倒序排列，返回被关注用户基本信息。</p>
     *
     * @param id      用户 ID
     * @param current 页码，默认 1
     * @param size    每页条数，默认 10
     * @return 关注列表分页结果
     */
    @Operation(summary = "关注列表", description = "分页查询指定用户关注的人的列表，按关注时间倒序")
    @GetMapping("/{id:[0-9]+}/following")
    public Result<PageVO<FollowItemVO>> listFollowing(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size) {
        log.info("查询关注列表请求 targetUserId={}, current={}, size={}", id, current, size);
        PageVO<FollowItemVO> page = userService.listFollowing(id, current, size);
        return Result.success(page);
    }

    /**
     * 查询指定用户发布的帖子列表（游标分页），对应 API 2.7
     * <p>支持最新（created_at）和热门（like_count）两种排序，用于个人主页帖子区域的无限加载。</p>
     *
     * @param id     用户 ID
     * @param cursor 游标，首页传 null
     * @param size   每页条数，默认 10
     * @param sort   排序方式：created_at（最新）/ like_count（最热），默认 created_at
     * @return 游标分页的帖子列表
     */
    @Operation(summary = "查询用户帖子列表", description = "游标分页查询指定用户发布的帖子，支持最新/热门排序")
    @GetMapping("/{id:[0-9]+}/posts")
    public Result<CursorPageVO<PostItemVO>> listUserPosts(
            @PathVariable Long id,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "created_at") String sort) {
        log.info("查询用户帖子列表请求 targetUserId={}, cursor={}, size={}, sort={}", id, cursor, size, sort);
        CursorPageVO<PostItemVO> page = postService.listUserPosts(id, cursor, size, sort);
        return Result.success(page);
    }
}
