package com.fafa.infrastructure.oss;

import com.fafa.infrastructure.config.FaFaProperties;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
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

    /**
     * 为 bucket 设置匿名只读策略（公共读）。
     *
     * 头像 / 照片属于展示型内容，小程序 <image> 与 DashScope 拉图均通过直链访问，
     * 桶默认私有会返回 AccessDenied。对象 key 含 UUID 不可枚举，公共读 + 随机 key
     * 是展示型媒体的标准做法（生产 OSS 同理，需在控制台将 bucket 设为 public-read）。
     */
    public static void ensurePublicRead(MinioClient client, String bucket) {
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": {"AWS": ["*"]},
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/*"]
                    }
                  ]
                }
                """.formatted(bucket);
        try {
            client.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(policy).build());
        } catch (Exception ex) {
            log.error("MinIO bucket 公共读策略设置失败: bucket={}", bucket, ex);
        }
    }
}
