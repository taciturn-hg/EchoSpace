package com.echospace.service.impl;

import com.echospace.common.BusinessException;
import com.echospace.config.MinioProperties;
import com.echospace.service.FileService;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
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

    private static final String[] ALLOWED_EXTENSIONS = {".jpg", ".jpeg", ".png"};
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

        String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";
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
        String url = endpoint + "/" + minioProperties.getBucketName() + "/" + objectName;
        log.info("MinIO 上传成功 url={}", url);
        return url;
    }

    private boolean isAllowedExtension(String extension) {
        for (String allowed : ALLOWED_EXTENSIONS) {
            if (allowed.equalsIgnoreCase(extension)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeEndpoint(String endpoint) {
        if (endpoint == null) {
            return "";
        }
        int endIndex = endpoint.length();
        while (endIndex > 0 && endpoint.charAt(endIndex - 1) == '/') {
            endIndex--;
        }
        return endpoint.substring(0, endIndex);
    }
}
