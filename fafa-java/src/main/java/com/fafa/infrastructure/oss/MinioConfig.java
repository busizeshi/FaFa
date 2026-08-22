package com.fafa.infrastructure.oss;

import com.fafa.infrastructure.config.FaFaProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端装配（fafa.storage.type=minio 时生效）
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(FaFaProperties properties) {
        FaFaProperties.Minio minio = properties.getMinio();
        log.info("装配 MinIO 存储: endpoint={}", minio.getEndpoint());
        return MinioClient.builder()
                .endpoint(minio.getEndpoint())
                .credentials(minio.getAccessKey(), minio.getSecretKey())
                .build();
    }

    /** 启动时确保业务 bucket 存在，失败不阻断启动（记日志人工处理） */
    public static void ensureBucket(MinioClient client, String bucket) {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("创建 MinIO bucket: {}", bucket);
            }
        } catch (Exception ex) {
            log.error("MinIO bucket 初始化失败: bucket={}", bucket, ex);
        }
    }
}
