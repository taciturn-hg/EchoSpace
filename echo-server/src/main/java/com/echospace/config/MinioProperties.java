package com.echospace.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MinIO 配置属性：绑定 application.yaml 中 minio.* 前缀的配置项
 *
 * @Author: taciturn-hg
 */
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {

    /** MinIO 服务端点 */
    private String endpoint;

    /** 访问密钥 */
    private String accessKey;

    /** 密钥 */
    private String secretKey;

    /** 存储桶名称 */
    private String bucketName;
}
