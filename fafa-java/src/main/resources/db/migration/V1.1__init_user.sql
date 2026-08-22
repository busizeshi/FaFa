-- 用户表（微信一键登录）
CREATE TABLE IF NOT EXISTS `user`
(
    `id`         BIGINT       NOT NULL COMMENT '雪花ID',
    `open_id`    VARCHAR(64)  NOT NULL COMMENT '微信openid',
    `nickname`   VARCHAR(64)  NULL COMMENT '昵称',
    `avatar_url` VARCHAR(512) NULL COMMENT '头像URL',
    `created_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at` DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted` TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0未删除 1已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_open_id` (`open_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';
