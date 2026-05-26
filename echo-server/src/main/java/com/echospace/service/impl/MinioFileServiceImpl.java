package com.echospace.service.impl;

import com.echospace.common.BusinessException;
import com.echospace.config.MinioProperties;
import com.echospace.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
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

    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            ".jpg", "image/jpeg",
            ".jpeg", "image/jpeg",
            ".png", "image/png"
    );
    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024;

    private String upload(MultipartFile file, String dir) {
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        if (!isAllowedExtension(extension)) {
            throw new BusinessException("文件扩展名不正确");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessException("文件大小不能超过2MB");
        }
        String objectName = dir + "/" + UUID.randomUUID() + extension;

        String contentType = resolveContentType(extension);
        try (var in = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.getBucketName())
                            .object(objectName)
                            .stream(in, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
        } catch (Exception e) {
            log.error("MinIO 上传失败 bucket={} object={}", minioProperties.getBucketName(), objectName, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR,"文件上传失败，请稍后重试");
        }

        String endpoint = normalizeEndpoint(minioProperties.getEndpoint());
        String publicBaseUrl = minioProperties.getPublicBaseUrl();
        String baseUrl = (publicBaseUrl != null && !publicBaseUrl.isBlank())
                ? normalizeEndpoint(publicBaseUrl)
                : endpoint;
        String url = baseUrl + "/" + minioProperties.getBucketName() + "/" + objectName;
        log.info("MinIO 上传成功 url={}", url);
        return url;
    }

    @Override
    public void deleteFile(String url) {
        String bucketName = minioProperties.getBucketName();
        int idx = url.indexOf("/" + bucketName + "/");
        if (idx == -1) {
            log.warn("MinIO 删除失败：URL 不匹配当前存储桶 url={} bucket={}", url, bucketName);
            throw new BusinessException("文件地址不合法，无法删除");
        }
        String objectName = url.substring(idx + bucketName.length() + 2);
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build()
            );
            log.info("MinIO 删除成功 bucket={} object={}", bucketName, objectName);
        } catch (Exception e) {
            log.error("MinIO 删除失败 bucket={} object={}", bucketName, objectName, e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件删除失败，请稍后重试");
        }
    }

    private boolean isAllowedExtension(String extension) {
        return ALLOWED_CONTENT_TYPES.containsKey(extension.toLowerCase());
    }

    private String resolveContentType(String extension) {
        return ALLOWED_CONTENT_TYPES.getOrDefault(extension.toLowerCase(), "application/octet-stream");
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null) {
            throw new BusinessException("MinIO endpoint 配置不能为空");
        }
        String normalized = endpoint.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException("MinIO endpoint 配置不能为空白");
        }
        int endIndex = normalized.length();
        while (endIndex > 0 && normalized.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return normalized.substring(0, endIndex);
    }
}
