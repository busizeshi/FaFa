package com.fafa.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * FaFa 业务配置项（前缀 fafa）
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
@ConfigurationProperties(prefix = "fafa")
public class FaFaProperties {

    /** 微信小程序配置 */
    private WeChat wechat = new WeChat();

    /** Python AI 服务配置 */
    private Python python = new Python();

    /** 服务间内部令牌（Java 与 Python 双方一致） */
    private String internalToken;

    /** 存储类型：minio（开发/测试）/ oss（生产） */
    private Storage storage = new Storage();

    /** AI 任务分发器：http（开发）/ rocketmq（生产） */
    private Ai ai = new Ai();

    /** 宠物数量限额配置 */
    private Pet pet = new Pet();

    /** 素材上传约束配置 */
    private Media media = new Media();

    /** 配额配置（存储空间 + AI 语义检索） */
    private Quota quota = new Quota();

    /** MinIO 配置 */
    private Minio minio = new Minio();

    /** 阿里云 OSS 配置 */
    private Oss oss = new Oss();

    @Data
    public static class WeChat {
        private String appid;
        private String secret;
    }

    @Data
    public static class Python {
        /** fafa-python 基地址，如 http://localhost:8000 */
        private String baseUrl;
    }

    @Data
    public static class Storage {
        /** minio / oss */
        private String type = "minio";
    }

    @Data
    public static class Ai {
        /** http / rocketmq */
        private String dispatcher = "http";
    }

    @Data
    public static class Pet {
        private Limit limit = new Limit();

        @Data
        public static class Limit {
            private int normal = 2;
            private int vip = 999;
        }
    }

    @Data
    public static class Media {
        /** 单个照片大小上限（字节） */
        private long photoMaxSizeBytes = 10485760L;
        /** 单个视频大小上限（字节） */
        private long videoMaxSizeBytes = 52428800L;
        /** 单批上传文件数上限 */
        private int batchMaxCount = 20;
    }

    @Data
    public static class Quota {
        private StorageQuota storage = new StorageQuota();
        private AiSearchQuota aiSearch = new AiSearchQuota();

        @Data
        public static class StorageQuota {
            /** 普通用户存储空间（字节） */
            private long normalBytes = 2147483648L;
            /** VIP 存储空间（字节） */
            private long vipBytes = 21474836480L;
        }

        @Data
        public static class AiSearchQuota {
            /** 普通用户每月 AI 语义检索次数（VIP 不限） */
            private int monthlyNormal = 10;
        }
    }

    @Data
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        /** 单桶 + 路径前缀区分模块（avatar/、pet/ 等） */
        private String bucketMedia = "fafa-media";
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName = "fafa-prod";
    }
}
