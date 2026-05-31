package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理工具控制器
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Tag(name = "管理模块", description = "系统管理工具接口")
@RestController
@RequestMapping("/admin")
public class AdminController {

    private final PostService postService;

    public AdminController(PostService postService) {
        this.postService = postService;
    }

    @Operation(summary = "同步帖子到 ES", description = "将数据库中所有未删除的帖子全量写入 Elasticsearch 索引")
    @PostMapping("/sync-es")
    public Result<Integer> syncEs() {
        log.info("管理员触发 ES 全量同步");
        int count = postService.syncAllPostsToEs();
        return Result.success(count, "ES 同步完成，共同步 " + count + " 条帖子");
    }
}
