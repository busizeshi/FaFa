package com.fafa.infrastructure.oss;

import com.fafa.domain.common.ErrorCode;
import com.fafa.domain.exception.BusinessException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储实现（开发/测试环境）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "fafa.storage.type", havingValue = "minio")
public class MinioStorageService implements StorageService {

    private final MinioClient minioClient;

    @Value("${fafa.storage.minio.bucket}")
    private String bucket;

    public MinioStorageService(
            @Value("${fafa.storage.minio.endpoint}") String endpoint,
            @Value("${fafa.storage.minio.access-key}") String accessKey,
            @Value("${fafa.storage.minio.secret-key}") String secretKey) {
        this.minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    /**
     * 启动时确保 bucket 存在，避免首次上传失败
     */
    @PostConstruct
    public void ensureBucket() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 已创建: {}", bucket);
            }
        } catch (Exception e) {
            // 不阻断启动：MinIO 未就绪时记录错误，上传时再失败
            log.error("MinIO bucket 检查失败，请确认 MinIO 服务可用: bucket={}", bucket, e);
        }
    }

    @Override
    public String upload(String objectKey, InputStream stream, long size, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(stream, size, -1)
                    .contentType(contentType)
                    .build());
            log.info("文件上传成功: {}", objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("文件上传失败: objectKey={}", objectKey, e);
            throw new BusinessException(ErrorCode.PHOTO_UPLOAD_FAILED);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
            log.info("文件删除成功: {}", objectKey);
        } catch (Exception e) {
            // 先删库后删文件策略：文件删除失败仅记录，不阻断业务
            log.error("文件删除失败，待人工清理: objectKey={}", objectKey, e);
        }
    }

    @Override
    public String presignedGetUrl(String objectKey, int expireSeconds) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(bucket)
                    .object(objectKey)
                    .expiry(expireSeconds, TimeUnit.SECONDS)
                    .build());
        } catch (Exception e) {
            log.error("生成预签名URL失败: objectKey={}", objectKey, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
    }
}
