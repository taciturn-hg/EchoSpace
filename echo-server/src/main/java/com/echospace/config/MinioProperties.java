package com.echospace.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * MinIO 配置属性：仅在 storage.type=minio 时由 MinioConfig 注册并校验
 *
 * @Author: taciturn-hg
 */
@Data
public class MinioProperties {

    /** MinIO 服务端点（SDK 直连用） */
    @NotBlank(message = "MinIO 服务端点不能为空")
    private String endpoint;

    /**
     * 对外访问基础 URL（返回给前端的文件链接前缀），为空时回退到 endpoint。
     * 典型场景：MinIO 经 nginx/CDN 反代，SDK 直连 endpoint 与对外域名不同。
     */
    private String publicBaseUrl;

    /** 访问密钥 */
    @NotBlank(message = "MinIO 访问密钥不能为空")
    private String accessKey;

    /** 密钥 */
    @NotBlank(message = "MinIO 密钥不能为空")
    private String secretKey;

    /** 存储桶名称 */
    @NotBlank(message = "MinIO 存储桶名称不能为空")
    private String bucketName;
}
