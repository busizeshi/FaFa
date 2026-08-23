-- ============================================================
-- Phase 3：提醒系统
-- 表：reminder（提醒）、reminder_completion_history（完成历史）、
--     event_record（事件记录，成长档案写入侧先行落地）
-- 设计决策见 docs/dev/phase3-implementation-plan.md：
--   1. 行 = 提醒定义 + 当前周期状态（无持久 completed 态，完成历史独立表承载）
--   2. 枚举列用 VARCHAR + Java 领域枚举校验（与 Phase 2 约定一致，不用 MySQL ENUM）
--   3. 新增 last_push_time（推送同日去重）、完成历史冗余标题/类型快照
-- ============================================================

-- 1. 提醒表（周期任务定义 + 当前状态）
CREATE TABLE IF NOT EXISTS `reminder` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `type` VARCHAR(16) NOT NULL COMMENT '提醒类型: vaccine/deworming/bath/nail_trim/checkup/medication/birthday/daily_record/custom',
  `title` VARCHAR(200) NOT NULL COMMENT '提醒标题',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '提醒描述',
  `interval_type` VARCHAR(16) NOT NULL COMMENT '周期类型: once/days/weekly/monthly/months/yearly',
  `interval_value` INT DEFAULT NULL COMMENT '周期值: days/months=N(≥1); weekly=ISO星期(1周一~7周日); monthly=每月几号(1-31); once/yearly不使用',
  `next_remind_time` DATETIME NOT NULL COMMENT '下次提醒时间（当前周期基准，完成以其为基准推算下期）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending生效中/paused已暂停/finished已结束(一次性提醒完成后)',
  `last_completion_time` DATETIME DEFAULT NULL COMMENT '最近一次完成时间',
  `last_completion_notes` VARCHAR(500) DEFAULT NULL COMMENT '最近一次完成备注',
  `last_push_time` DATETIME DEFAULT NULL COMMENT '最近一次推送时间（同一提醒每日至多推送一次）',
  `medication_id` BIGINT DEFAULT NULL COMMENT '关联用药ID（Phase 6 用药管理预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_status` (`user_id`, `status`),
  KEY `idx_pet` (`pet_id`),
  KEY `idx_scan` (`status`, `next_remind_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒表（周期任务定义+当前状态）';

-- 2. 提醒完成历史表（追加只写，不修改不删除）
CREATE TABLE IF NOT EXISTS `reminder_completion_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `reminder_id` BIGINT NOT NULL COMMENT '提醒ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '完成人ID（家庭协同预留，现阶段=宠物主人）',
  `reminder_title` VARCHAR(200) NOT NULL COMMENT '提醒标题快照（提醒删除后历史仍可独立展示）',
  `reminder_type` VARCHAR(16) NOT NULL COMMENT '提醒类型快照',
  `completion_time` DATETIME NOT NULL COMMENT '完成时间（可晚于计划提醒时间）',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '完成备注',
  `photo_urls` JSON DEFAULT NULL COMMENT '完成照片URL列表（如疫苗本照片）',
  `event_record_id` BIGINT DEFAULT NULL COMMENT '关联生成的事件记录ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_reminder` (`reminder_id`, `completion_time`),
  KEY `idx_user_time` (`user_id`, `completion_time`),
  KEY `idx_pet_time` (`pet_id`, `completion_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒完成历史表（追加只写）';

-- 3. 事件记录表（成长档案数据底座，Phase 3 仅提醒完成写入侧；读接口/时间轴 Phase 4+ 扩展）
CREATE TABLE IF NOT EXISTS `event_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `source` VARCHAR(16) NOT NULL DEFAULT 'manual' COMMENT '来源: manual手动创建(Phase 4+)/reminder提醒完成生成',
  `source_id` BIGINT DEFAULT NULL COMMENT '来源对象ID（source=reminder 时为 reminder_id）',
  `event_type` VARCHAR(32) NOT NULL COMMENT '事件类型: vaccine/deworming/bath/nail_trim/checkup/custom',
  `title` VARCHAR(200) NOT NULL COMMENT '事件标题',
  `content` VARCHAR(2000) DEFAULT NULL COMMENT '事件详情',
  `event_date` DATE NOT NULL COMMENT '事件日期',
  `material_urls` JSON DEFAULT NULL COMMENT '素材URL列表（成长档案扩展）',
  `tags` JSON DEFAULT NULL COMMENT '标签列表（成长档案扩展，Phase 4 打通 user_tag）',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_date` (`pet_id`, `event_date`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件记录表（成长档案，Phase 3 提醒完成写入侧先行）';
