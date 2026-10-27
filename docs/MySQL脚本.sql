-- ==================================================================
-- FaFa MySQL 全量 DDL 脚本（单库架构：唯一业务库 fafa）
-- 版本：v1.0（2026-10-26）
-- 适用环境：192.168.1.254 / MySQL 8.0（docker 容器 fafa-mysql）
--
-- 执行策略（务必先读）：
--   §1 库与账号        ：【必须手工执行】，一次性；用 root 连接执行
--   §2 业务表 DDL      ：【正常情况不要手工执行】！表结构由 fafa-biz 启动时
--                        的 Flyway 自动迁移创建；本节是 V2 迁移的存档副本，
--                        用于评审、比对与应急重建（应急方法见文末 §3）。
--
-- 数据字典约定（架构规范 §7 / 手册 §6.3）：
--   1. 时间列一律 DATETIME(6)、无默认值，由应用层统一填充 UTC；
--   2. 密码/验证码只存哈希（bcrypt），禁止明文或可逆加密；
--   3. 枚举用 VARCHAR 存可读值；金额用最小货币单位整数；测量值用 DECIMAL；
--   4. biz/admin 共库：admin 业务表统一 admin_ 前缀、迁移历史用
--      flyway_schema_history_admin（biz 用 flyway_schema_history）。
-- ==================================================================


-- ##################################################################
-- §1 库与账号（必须手工执行）
-- ##################################################################

-- ---- 1.1 唯一业务库 ----
CREATE DATABASE IF NOT EXISTS fafa
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- ---- 1.2 统一业务账号（biz / admin 共用） ----
-- 密码与《FaFa_中间件环境与账号信息_v1.0.md》§4.1 保持一致；
-- 对应环境变量 MYSQL_USERNAME / MYSQL_PASSWORD
CREATE USER IF NOT EXISTS 'fafa'@'%' IDENTIFIED BY 'fafa_123456';
ALTER USER 'fafa'@'%' IDENTIFIED BY 'fafa_123456';
GRANT ALL PRIVILEGES ON fafa.* TO 'fafa'@'%';
FLUSH PRIVILEGES;

-- ---- 1.3 可选清理：旧多库架构遗留账号（建议执行） ----
DROP USER IF EXISTS 'fafa_biz'@'%';
DROP USER IF EXISTS 'fafa_admin'@'%';
DROP USER IF EXISTS 'fafa_wp02_admin'@'%';
DROP USER IF EXISTS 'fafa_wp02_biz'@'%';
DROP USER IF EXISTS 'fafa_wp04_suite'@'%';
FLUSH PRIVILEGES;

-- ---- 1.4 执行验证 ----
-- SHOW DATABASES;               应出现 fafa（业务库）与 nacos_config / powerjob（系统元数据，勿动）
-- SHOW GRANTS FOR 'fafa'@'%';   应仅有 fafa.* 授权


-- ##################################################################
-- §2 业务表 DDL（Flyway V2 存档副本——正常情况不要手工执行！）
-- 权威来源：fafa-biz/src/main/resources/db/migration/V2__create_identity_and_platform.sql
-- 本节与 V2 保持同步；修改表结构必须先改 V3 迁移，再回填本文件。
-- ##################################################################

-- ---- 2.1 宠主账号表（identity 上下文） ----
CREATE TABLE user_account
(
    id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    email         VARCHAR(255) NOT NULL COMMENT '登录邮箱（应用侧统一小写归一化）',
    password_hash VARCHAR(100) NOT NULL COMMENT '密码哈希（bcrypt，DelegatingPasswordEncoder 格式）',
    display_name  VARCHAR(64)  NULL COMMENT '显示昵称（注册后可改，M08 我的资料）',
    status        VARCHAR(32)  NOT NULL DEFAULT 'PENDING_VERIFICATION' COMMENT '状态：PENDING_VERIFICATION 未验证 / ACTIVE 正常 / SUSPENDED 停用',
    created_at    DATETIME(6)  NOT NULL COMMENT '创建时间（UTC，应用层填充）',
    updated_at    DATETIME(6)  NOT NULL COMMENT '更新时间（UTC，应用层填充）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_email (email)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='宠主账号';

-- ---- 2.2 邮箱验证挑战表（注册验证 / 找回密码第一步） ----
-- 验证码只存哈希；挑战可作废（超尝试次数 / 已消费 / 重发新挑战）
CREATE TABLE email_challenge
(
    id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键（对外暴露 challengeId）',
    purpose       VARCHAR(32)  NOT NULL COMMENT '用途：REGISTER_VERIFY 注册验证 / PASSWORD_RESET 找回密码',
    email         VARCHAR(255) NOT NULL COMMENT '目标邮箱（小写）',
    code_hash     VARCHAR(100) NOT NULL COMMENT '验证码哈希（bcrypt）',
    expires_at    DATETIME(6)  NOT NULL COMMENT '过期时间（UTC，默认 10 分钟）',
    attempt_count INT          NOT NULL DEFAULT 0 COMMENT '已尝试校验次数',
    max_attempts  INT          NOT NULL DEFAULT 5 COMMENT '最大尝试次数，超限作废',
    consumed_at   DATETIME(6)  NULL COMMENT '消费时间（验证成功 / 作废）',
    created_at    DATETIME(6)  NOT NULL COMMENT '创建时间（UTC，应用层填充）',
    updated_at    DATETIME(6)  NOT NULL COMMENT '更新时间（UTC，应用层填充）',
    PRIMARY KEY (id),
    KEY idx_challenge_email_purpose (email, purpose, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='邮箱验证挑战';

-- ---- 2.3 协议同意记录（用户协议 / 隐私政策，追加式） ----
CREATE TABLE legal_consent
(
    id               BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    user_id          BIGINT UNSIGNED NOT NULL COMMENT '用户 ID',
    document_type    VARCHAR(32) NOT NULL COMMENT '文档类型：USER_AGREEMENT / PRIVACY_POLICY',
    document_version VARCHAR(16) NOT NULL COMMENT '同意的版本号（客户端内置文本版本）',
    consented_at     DATETIME(6) NOT NULL COMMENT '同意时间（UTC，应用层填充）',
    created_at       DATETIME(6) NOT NULL COMMENT '创建时间（UTC，应用层填充）',
    updated_at       DATETIME(6) NOT NULL COMMENT '更新时间（UTC，应用层填充）',
    PRIMARY KEY (id),
    KEY idx_consent_user (user_id, document_type)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='协议同意记录（追加式，不更新历史）';

-- ---- 2.4 邮件发送日志（审计用：只记结果与错误，不记验证码明文） ----
CREATE TABLE email_send_log
(
    id           BIGINT UNSIGNED AUTO_INCREMENT COMMENT '主键',
    to_email     VARCHAR(255) NOT NULL COMMENT '收件邮箱',
    purpose      VARCHAR(32)  NOT NULL COMMENT '用途：REGISTER_VERIFY / PASSWORD_RESET',
    status       VARCHAR(16)  NOT NULL COMMENT 'SENT 成功 / FAILED 失败',
    error_msg    VARCHAR(500) NULL COMMENT '失败原因（脱敏摘要）',
    challenge_id BIGINT UNSIGNED NULL COMMENT '关联的挑战 ID',
    created_at   DATETIME(6)  NOT NULL COMMENT '创建时间（UTC，应用层填充）',
    updated_at   DATETIME(6)  NOT NULL COMMENT '更新时间（UTC，应用层填充）',
    PRIMARY KEY (id),
    KEY idx_send_log_email (to_email, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='邮件发送日志';

-- ---- 2.5 Outbox 事件表（业务数据和事件同事务提交，发布器异步投递） ----
CREATE TABLE outbox_event
(
    id               BIGINT UNSIGNED AUTO_INCREMENT COMMENT '自增主键',
    event_id         VARCHAR(64) NOT NULL COMMENT '全局唯一事件 ID（UUID）',
    event_type       VARCHAR(64) NOT NULL COMMENT '事件类型，如 user.registered',
    schema_version   INT         NOT NULL DEFAULT 1 COMMENT '消息 schema 版本',
    aggregate_type   VARCHAR(32) NOT NULL COMMENT '聚合类型，如 user',
    aggregate_id     VARCHAR(64) NOT NULL COMMENT '聚合 ID',
    job_id           VARCHAR(32) NULL COMMENT '关联任务 ID（任务消息时必填）',
    payload          JSON        NOT NULL COMMENT '事件载荷（禁止敏感原文/大字段）',
    trace_id         VARCHAR(64) NULL COMMENT '链路追踪 ID',
    publish_attempts INT         NOT NULL DEFAULT 0 COMMENT '已尝试发布次数',
    published_at     DATETIME(6) NULL COMMENT '确认发布时间（收到 confirm 后写入）',
    created_at       DATETIME(6) NOT NULL COMMENT '创建时间（UTC，应用层填充）',
    updated_at       DATETIME(6) NOT NULL COMMENT '更新时间（UTC，应用层填充）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_outbox_event_id (event_id),
    KEY idx_outbox_unpublished (published_at, id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='Outbox 事件（同事务持久化，发布器异步投递）';

-- ---- 2.6 异步任务表：GET /api/v1/operations/{jobId} 的状态事实 ----
CREATE TABLE operation_job
(
    id            BIGINT UNSIGNED AUTO_INCREMENT COMMENT '自增主键',
    job_id        VARCHAR(32) NOT NULL COMMENT '对外任务 ID（UUID）',
    job_type      VARCHAR(64) NOT NULL COMMENT '任务类型，如 media.process / report.generate',
    status        VARCHAR(16) NOT NULL DEFAULT 'QUEUED' COMMENT 'QUEUED / RUNNING / SUCCEEDED / FAILED / CANCELLED',
    progress      INT         NOT NULL DEFAULT 0 COMMENT '进度 0-100',
    error_code    VARCHAR(32) NULL COMMENT '稳定错误码',
    retryable     TINYINT(1)  NOT NULL DEFAULT 0 COMMENT '失败是否可重试',
    owner_user_id BIGINT UNSIGNED NULL COMMENT '所属用户（用户查询隔离）',
    started_at    DATETIME(6) NULL COMMENT '开始时间（UTC）',
    finished_at   DATETIME(6) NULL COMMENT '结束时间（UTC）',
    created_at    DATETIME(6) NOT NULL COMMENT '创建时间（UTC，应用层填充）',
    updated_at    DATETIME(6) NOT NULL COMMENT '更新时间（UTC，应用层填充）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_job_job_id (job_id),
    KEY idx_job_owner_status (owner_user_id, status, created_at)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='异步任务状态';

-- ---- 2.7 幂等记录表：按 (用户, 操作域, Idempotency-Key) 去重 ----
CREATE TABLE idempotency_record
(
    id              BIGINT UNSIGNED AUTO_INCREMENT COMMENT '自增主键',
    user_id         BIGINT UNSIGNED NOT NULL COMMENT '用户 ID（未登录场景用 0）',
    scope           VARCHAR(64) NOT NULL COMMENT '操作域，如 auth.register',
    idempotency_key VARCHAR(64) NOT NULL COMMENT '客户端幂等键',
    response_code   VARCHAR(16) NULL COMMENT '首次处理结果码（快照）',
    created_at      DATETIME(6) NOT NULL COMMENT '创建时间（UTC，应用层填充）',
    updated_at      DATETIME(6) NOT NULL COMMENT '更新时间（UTC，应用层填充）',
    PRIMARY KEY (id),
    UNIQUE KEY uk_idempotency (user_id, scope, idempotency_key)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='幂等记录';


-- ##################################################################
-- §3 应急重建说明（仅当 §2 被手工执行时才需要）
-- ##################################################################
-- 手工执行过 §2 后，Flyway 会在启动时再次执行 V2 导致"表已存在"失败。
-- 处理方法（二选一）：
--   方法 A（推荐）：启动 fafa-biz 时设置环境变量 SPRING_FLYWAY_BASELINE_VERSION=2
--     （biz 侧 baseline-on-migrate 已开启，Flyway 会对非空 schema 打 baseline=2，
--      跳过 V1/V2，之后从 V3 正常接管；应急结束移除该变量）；
--   方法 B：TRUNCATE 全部 §2 表后让 Flyway 从零迁移（会丢数据，仅空库可用）。
-- ==================================================================
