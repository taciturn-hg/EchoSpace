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

    /** MinIO 服务端点 */
    @NotBlank
    private String endpoint;

    /** 访问密钥 */
    @NotBlank
    private String accessKey;

    /** 密钥 */
    @NotBlank
    private String secretKey;

    /** 存储桶名称 */
    @NotBlank
    private String bucketName;
}
