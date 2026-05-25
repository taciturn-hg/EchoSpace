package com.echospace.controller;

import com.echospace.common.Result;
import com.echospace.service.FileService;
import com.echospace.vo.UploadAvatarVO;
import com.echospace.vo.UploadImageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传控制器：头像上传、通用图片上传
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Tag(name = "文件上传", description = "头像上传、通用图片上传")
@RestController
@RequestMapping("/upload")
public class FileController {

    @Autowired
    private FileService fileService;

    /**
     * 上传头像
     *
     * @param file 图片文件（支持 JPG / PNG，前端限制 2MB）
     * @return 上传后的文件访问 URL
     */
    @Operation(summary = "上传头像", description = "上传用户头像图片，返回文件访问 URL")
    @PostMapping("/avatar")
    public Result<UploadAvatarVO> uploadAvatar(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        String url = fileService.uploadAvatar(file);
        log.info("头像上传成功 url={}", url);
        return Result.success(new UploadAvatarVO(url));
    }

    /**
     * 上传通用图片（帖子配图等）
     *
     * @param file 图片文件
     * @return 上传后的文件访问 URL
     */
    @Operation(summary = "上传图片", description = "上传通用图片（帖子配图等），返回文件访问 URL")
    @PostMapping("/image")
    public Result<UploadImageVO> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("请选择要上传的文件");
        }
        String url = fileService.uploadImage(file);
        log.info("图片上传成功 url={}", url);
        return Result.success(new UploadImageVO(url));
    }
}
