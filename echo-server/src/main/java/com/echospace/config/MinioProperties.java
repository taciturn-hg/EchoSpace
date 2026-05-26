package com.echospace.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * MinIO 配置属性：绑定 application.yaml 中 minio.* 前缀的配置项
 *
 * @Author: taciturn-hg
 */
@ConfigurationProperties(prefix = "minio")
@Validated
@Data
public class MinioProperties {

    /** MinIO 服务端点 */
    @NotBlank
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 密钥 */
    private String secretKey;

    /** 存储桶名称 */
    @NotBlank
    private String bucketName;
}
