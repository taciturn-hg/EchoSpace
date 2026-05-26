package com.echospace.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件存储服务接口：由 MinIO 或 OSS 实现，通过 storage.type 配置切换
 *
 * @Author: taciturn-hg
 */
public interface FileService {

    /**
     * 上传头像图片
     *
     * @param file 上传的图片文件
     * @return 上传后的文件访问 URL
     */
    String uploadAvatar(MultipartFile file);

    /**
     * 上传通用图片（帖子配图等）
     *
     * @param file 上传的图片文件
     * @return 上传后的文件访问 URL
     */
    String uploadImage(MultipartFile file);

    /**
     * 删除已上传的文件（用于更新失败回滚等场景）
     *
     * @param url 上传时返回的文件访问 URL
     */
    void deleteFile(String url);
}
