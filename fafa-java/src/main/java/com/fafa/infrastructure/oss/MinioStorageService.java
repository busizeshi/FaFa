package com.fafa.infrastructure.oss;

import cn.hutool.core.util.IdUtil;
import com.fafa.infrastructure.config.FaFaProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 存储实现（开发/测试环境）
 *
 * 对象路径规范：{folder}/{yyyyMM}/{uuid}.{ext}
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private static final DateTimeFormatter MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final MinioClient minioClient;
    private final FaFaProperties properties;

    @PostConstruct
    public void init() {
        MinioConfig.ensureBucket(minioClient, properties.getMinio().getBucketMedia());
        MinioConfig.ensureBucket(minioClient, properties.getMinio().getBucketAvatar());
    }

    @Override
    public String upload(MultipartFile file, String folder) {
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".") 
            ? originalFilename.substring(originalFilename.lastIndexOf(".") + 1) 
            : "jpg";
        
        String objectKey = buildObjectKey(folder, extension);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getMinio().getBucketMedia())
                    .object(objectKey)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            
            String endpoint = properties.getMinio().getEndpoint();
            String bucket = properties.getMinio().getBucketMedia();
            return endpoint + "/" + bucket + "/" + objectKey;
            
        } catch (Exception ex) {
            log.error("MinIO 上传失败: objectKey={}", objectKey, ex);
            throw new RuntimeException("File upload failed", ex);
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(properties.getMinio().getBucketMedia())
                    .object(objectKey)
                    .build());
        } catch (Exception ex) {
            log.error("MinIO 删除失败: fileUrl={}", fileUrl, ex);
        }
    }

    @Override
    public String generatePresignedUrl(String fileUrl, int expireSeconds) {
        try {
            String objectKey = extractObjectKey(fileUrl);
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(io.minio.http.Method.GET)
                    .bucket(properties.getMinio().getBucketMedia())
                    .object(objectKey)
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception ex) {
            log.error("MinIO 预签名失败: fileUrl={}", fileUrl, ex);
            throw new RuntimeException("Generate presigned URL failed", ex);
        }
    }

    private String buildObjectKey(String folder, String extension) {
        String month = LocalDate.now().format(MONTH_FORMATTER);
        return folder + "/" + month + "/" + IdUtil.fastSimpleUUID() + "." + extension;
    }

    private String extractObjectKey(String fileUrl) {
        String bucket = properties.getMinio().getBucketMedia();
        int bucketIndex = fileUrl.indexOf(bucket);
        if (bucketIndex == -1) {
            throw new IllegalArgumentException("Invalid MinIO URL: " + fileUrl);
        }
        return fileUrl.substring(bucketIndex + bucket.length() + 1);
    }
}
