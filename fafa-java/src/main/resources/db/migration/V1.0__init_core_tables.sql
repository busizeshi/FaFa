-- FaFa 核心基础表
-- 命名规范：表/字段 snake_case，逻辑删除字段 is_deleted

CREATE TABLE `user`
(
    `id`              BIGINT       NOT NULL COMMENT '主键（雪花ID）',
    `openid`          VARCHAR(64)  NOT NULL COMMENT '微信openid',
    `nickname`        VARCHAR(64)      NULL COMMENT '昵称',
    `avatar_url`      VARCHAR(512)     NULL COMMENT '头像URL',
    `phone`           VARCHAR(20)      NULL COMMENT '手机号',
    `gender`          TINYINT          NULL COMMENT '性别 0未知 1男 2女',
    `city`            VARCHAR(64)      NULL COMMENT '城市',
    `member_level`    TINYINT      NOT NULL DEFAULT 0 COMMENT '会员等级 0普通 1会员',
    `member_expire_at` DATETIME         NULL COMMENT '会员到期时间',
    `created_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_openid` (`openid`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='用户表';

CREATE TABLE `pet`
(
    `id`              BIGINT        NOT NULL COMMENT '主键（雪花ID）',
    `user_id`         BIGINT        NOT NULL COMMENT '归属用户ID',
    `name`            VARCHAR(32)   NOT NULL COMMENT '宠物名称',
    `type`            TINYINT       NOT NULL DEFAULT 1 COMMENT '宠物类型 1猫 2狗',
    `gender`          VARCHAR(16)   NOT NULL DEFAULT 'unknown' COMMENT '性别 male/female/unknown',
    `birth_date`      DATE              NULL COMMENT '出生日期（由用户所选年龄外推）',
    `adopt_date`      DATE              NULL COMMENT '到家日期',
    `breed`           VARCHAR(64)       NULL COMMENT '品种',
    `weight`          DECIMAL(6, 2)     NULL COMMENT '当前体重(kg)',
    `is_neutered`     TINYINT       NOT NULL DEFAULT 0 COMMENT '是否绝育',
    `coat_color`      VARCHAR(64)       NULL COMMENT '毛色',
    `remarks`         VARCHAR(512)      NULL COMMENT '备注',
    `avatar`          VARCHAR(512)      NULL COMMENT '头像URL',
    `front_view_url`  VARCHAR(512)      NULL COMMENT '三视图-正面照URL',
    `side_view_url`   VARCHAR(512)      NULL COMMENT '三视图-侧面照URL',
    `top_view_url`    VARCHAR(512)      NULL COMMENT '三视图-俯拍照URL',
    `sort_order`      INT           NOT NULL DEFAULT 0 COMMENT '展示排序',
    `created_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updated_at`      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`      TINYINT       NOT NULL DEFAULT 0 COMMENT '逻辑删除 0未删 1已删',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`, `is_deleted`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='宠物表';
