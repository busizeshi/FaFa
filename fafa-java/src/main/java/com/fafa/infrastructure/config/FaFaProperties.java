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
    public static class Minio {
        private String endpoint;
        private String accessKey;
        private String secretKey;
        private String bucketMedia = "fafa-media";
        private String bucketAvatar = "fafa-avatar";
    }

    @Data
    public static class Oss {
        private String endpoint;
        private String accessKeyId;
        private String accessKeySecret;
        private String bucketName = "fafa-prod";
    }
}
