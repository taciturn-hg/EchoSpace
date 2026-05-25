package com.echospace.service.impl;

import com.echospace.common.BusinessException;
import com.echospace.config.MinioProperties;
import com.echospace.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * MinIO 文件存储实现：仅在 storage.type=minio 时生效
 *
 * @Author: taciturn-hg
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "storage.type", havingValue = "minio")
public class MinioFileServiceImpl implements FileService {

    @Autowired
    private MinioClient minioClient;

    @Autowired
    private MinioProperties minioProperties;

    @Override
    public String uploadAvatar(MultipartFile file) {
        return upload(file, "avatars");
    }

    @Override
    public String uploadImage(MultipartFile file) {
        return upload(file, "images");
    }

    private String upload(MultipartFile file, String dir) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String objectName = dir + "/" + UUID.randomUUID() + extension;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 上传失败 bucket={} object={}", minioProperties.getBucketName(), objectName, e);
            throw new BusinessException("文件上传失败，请稍后重试");
        }

        String url = minioProperties.getEndpoint() + "/" + minioProperties.getBucketName() + "/" + objectName;
        log.info("MinIO 上传成功 url={}", url);
        return url;
    }
}
