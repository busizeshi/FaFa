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

    /** AI 对话运行约束 */
    private Chat chat = new Chat();

    /** 就医与用药约束 */
    private Medical medical = new Medical();

    /** 健康预警规则与扫描配置 */
    private HealthAlert healthAlert = new HealthAlert();

    /** 食品识别与评分配置 */
    private Food food = new Food();

    /** 喂食计划与分析配置 */
    private Feeding feeding = new Feeding();

    /** 宠物开支与统计配置 */
    private Expense expense = new Expense();

    /** 周期报告与内容创作配置 */
    private Report report = new Report();

    /** 家庭协同与应急信息卡配置 */
    private Collaboration collaboration = new Collaboration();

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
        /** 视频交给百炼处理时，预签名 URL 的有效期（秒） */
        private int mediaUrlExpireSeconds = 3600;
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
        private FoodRecognitionQuota foodRecognition = new FoodRecognitionQuota();
        private ReportContentQuota reportContent = new ReportContentQuota();

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

        @Data
        public static class FoodRecognitionQuota {
            /** 普通用户每月食品包装识别次数。 */
            private int monthlyNormal = 5;
            /** VIP 用户每月食品包装识别次数。 */
            private int monthlyVip = 50;
        }

        @Data
        public static class ReportContentQuota {
            private int monthlyNormal = 3;
            private int monthlyVip = 10;
        }
    }

    @Data
    public static class Food {
        private Recognition recognition = new Recognition();
        private Transition transition = new Transition();
        private Score score = new Score();

        @Data
        public static class Recognition {
            private int timeoutSeconds = 90;
            private int draftRetentionDays = 7;
            private long maxImageBytes = 10485760L;
            private String promptVersion = "food-recognition-v1";
        }

        @Data
        public static class Transition {
            private int minDays = 3;
            private int maxDays = 30;
        }

        @Data
        public static class Score {
            private String policyVersion = "v1";
            private int proteinSourcePartialScore = 12;
            private int additiveFlagPenalty = 5;
            private java.math.BigDecimal calciumPhosphorusMinRatio = new java.math.BigDecimal("1.0");
            private java.math.BigDecimal calciumPhosphorusMaxRatio = new java.math.BigDecimal("2.0");
        }
    }

    @Data
    public static class Feeding {
        private int maxRangeDays = 366;
        private int maxItemsPerPlan = 12;
        private int recommendationTimeoutSeconds = 45;
        private String policyVersion = "v1";
    }

    @Data
    public static class Expense {
        private String defaultCurrency = "CNY";
        private java.math.BigDecimal maxAmount = new java.math.BigDecimal("1000000.00");
        private int maxRangeDays = 3660;
        private boolean autoSyncMedical = true;
        private String calculationVersion = "v1";
    }

    @Data
    public static class Report {
        private int maxRangeDays = 3660;
        private int maxMediaCount = 12;
        private int generationTimeoutSeconds = 90;
        private String snapshotVersion = "report-facts-v1";
        private String promptVersion = "report-content-v1";
        private String calculationVersion = "v1";
        private String scheduleScanCron = "0 */10 * * * *";
    }

    @Data
    public static class Collaboration {
        /** 协同功能总开关；关闭时仍允许宠物主人使用旧功能。 */
        private boolean enabled;
        private int normalMaxMembers;
        private int vipMaxMembers = 5;
        private int caregiverMaxDays = 30;
        private int invitationExpireMinutes = 30;
        private int inviteRateLimitPerHour = 20;
        private int acceptRateLimitPerHour = 30;
        private int auditRetentionDays = 730;
        private int reminderMaxRetries = 3;
        private String reminderDeliveryCron = "0 */5 * * * *";
    }

    @Data
    public static class Chat {
        private int maxMessageLength = 8000;
        private int historyMessageLimit = 20;
        private int agentTimeoutSeconds = 45;
        private int toolTimeoutSeconds = 8;
        private int maxToolCalls = 5;
        private double recordDraftConfidenceThreshold = 0.80D;
    }

    @Data
    public static class Medical {
        private int visitMaxRangeDays = 366;
        private int summaryMaxRangeDays = 90;
        private int summaryTimeoutSeconds = 45;
        private int medicationFutureToleranceMinutes = 5;
        private int maxAdministrationTimes = 6;
    }

    @Data
    public static class HealthAlert {
        private boolean enabled = true;
        private String scanCron = "0 */30 * * * *";
        private int dailyWindowDays = 7;
        private int increasedWaterDays = 3;
        private int appetiteEnergyDeclineDays = 2;
        private int diarrheaDays = 2;
        private int missedMedicationCount = 2;
        private int redRepeatHours = 24;
        private int retentionDays = 180;
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
