-- Phase 7：食品管理、包装识别、换粮过渡与喂食计划

CREATE TABLE IF NOT EXISTS `food_card` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '食品卡ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `name` VARCHAR(100) NOT NULL COMMENT '食品名称',
  `brand` VARCHAR(100) DEFAULT NULL COMMENT '品牌',
  `food_type` VARCHAR(32) NOT NULL COMMENT 'main_food/canned/snack/supplement/homemade/other',
  `specification` VARCHAR(100) DEFAULT NULL COMMENT '包装规格',
  `package_weight` DECIMAL(12,3) DEFAULT NULL COMMENT '包装重量或容量',
  `package_weight_unit` VARCHAR(8) DEFAULT NULL COMMENT 'g/ml',
  `price` DECIMAL(12,2) DEFAULT NULL COMMENT '价格',
  `currency` VARCHAR(8) NOT NULL DEFAULT 'CNY' COMMENT '货币',
  `opened_at` DATE DEFAULT NULL COMMENT '开封日期',
  `expires_at` DATE DEFAULT NULL COMMENT '保质期日期',
  `nutrition_basis` VARCHAR(24) NOT NULL DEFAULT 'unknown' COMMENT 'per_100g/per_100ml/serving/unknown',
  `protein_percent` DECIMAL(7,4) DEFAULT NULL COMMENT '蛋白质百分比',
  `fat_percent` DECIMAL(7,4) DEFAULT NULL COMMENT '脂肪百分比',
  `fiber_percent` DECIMAL(7,4) DEFAULT NULL COMMENT '纤维百分比',
  `moisture_percent` DECIMAL(7,4) DEFAULT NULL COMMENT '水分百分比',
  `ash_percent` DECIMAL(7,4) DEFAULT NULL COMMENT '灰分百分比',
  `calcium_percent` DECIMAL(7,4) DEFAULT NULL COMMENT '钙百分比',
  `phosphorus_percent` DECIMAL(7,4) DEFAULT NULL COMMENT '磷百分比',
  `energy_kcal` DECIMAL(12,4) DEFAULT NULL COMMENT '能量',
  `energy_unit` VARCHAR(24) DEFAULT NULL COMMENT 'kcal_per_100g/kcal_per_100ml/kcal_per_serving',
  `protein_sources` JSON DEFAULT NULL COMMENT '蛋白来源数组',
  `ingredients` JSON DEFAULT NULL COMMENT '配料表数组',
  `additive_flags` JSON DEFAULT NULL COMMENT '添加物提示标记数组',
  `life_stage_label` VARCHAR(100) DEFAULT NULL COMMENT '包装阶段标注',
  `source_media_id` BIGINT DEFAULT NULL COMMENT '来源媒体ID',
  `source_recognition_task_id` BIGINT DEFAULT NULL COMMENT '来源识别任务ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/archived/deleted',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_card_recognition_task` (`source_recognition_task_id`),
  KEY `idx_food_card_user_pet_status` (`user_id`, `pet_id`, `status`, `is_deleted`),
  KEY `idx_food_card_pet_updated` (`pet_id`, `updated_at`),
  KEY `idx_food_card_source_media` (`source_media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食品管理卡';

CREATE TABLE IF NOT EXISTS `food_recognition_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '食品识别任务ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `media_id` BIGINT NOT NULL COMMENT '包装图片媒体ID',
  `idempotency_key` VARCHAR(100) NOT NULL COMMENT '幂等键',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT 'pending/processing/completed/confirmed/rejected/failed/expired',
  `model_name` VARCHAR(100) DEFAULT NULL COMMENT '模型名称',
  `prompt_version` VARCHAR(100) DEFAULT NULL COMMENT '提示词版本',
  `draft_json` JSON DEFAULT NULL COMMENT '结构化识别草稿',
  `raw_response_json` JSON DEFAULT NULL COMMENT '受限保存的原始响应',
  `error_code` VARCHAR(64) DEFAULT NULL COMMENT '失败代码',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '失败信息',
  `food_card_id` BIGINT DEFAULT NULL COMMENT '确认后生成的食品卡ID',
  `quota_month` CHAR(7) DEFAULT NULL COMMENT '配额月份',
  `started_at` DATETIME DEFAULT NULL,
  `completed_at` DATETIME DEFAULT NULL,
  `expired_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_food_recognition_request` (`user_id`, `idempotency_key`),
  KEY `idx_food_recognition_media_status` (`user_id`, `media_id`, `status`),
  KEY `idx_food_recognition_pet_created` (`pet_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='食品包装识别任务';

CREATE TABLE IF NOT EXISTS `food_transition_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '换粮计划ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `old_food_id` BIGINT NOT NULL COMMENT '旧食品卡ID',
  `new_food_id` BIGINT NOT NULL COMMENT '新食品卡ID',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE NOT NULL COMMENT '结束日期',
  `daily_target_amount` DECIMAL(12,3) DEFAULT NULL COMMENT '每日目标总量',
  `unit` VARCHAR(8) DEFAULT NULL COMMENT 'g/ml',
  `ratio_steps` JSON NOT NULL COMMENT '每日新旧食品比例',
  `reaction_date` DATE DEFAULT NULL COMMENT '最近一次反应日期',
  `reaction_code` VARCHAR(24) DEFAULT NULL COMMENT '最近反应',
  `reaction_note` VARCHAR(500) DEFAULT NULL COMMENT '反应备注',
  `status` VARCHAR(16) NOT NULL DEFAULT 'draft' COMMENT 'draft/active/completed/cancelled',
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_transition_pet_date` (`pet_id`, `start_date`, `end_date`, `status`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='换粮过渡计划';

CREATE TABLE IF NOT EXISTS `feeding_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '喂食计划ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `name` VARCHAR(100) DEFAULT NULL COMMENT '计划名称',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '结束日期',
  `daily_target_calories` DECIMAL(12,4) DEFAULT NULL COMMENT '每日估算能量',
  `daily_target_amount` DECIMAL(12,3) DEFAULT NULL COMMENT '每日目标总量',
  `unit` VARCHAR(8) DEFAULT NULL COMMENT 'g/ml',
  `recommendation_source` VARCHAR(24) NOT NULL DEFAULT 'user_input' COMMENT 'user_input/rule/ai_assisted',
  `policy_version` VARCHAR(32) DEFAULT NULL COMMENT '估算策略版本',
  `version` INT NOT NULL DEFAULT 1 COMMENT '计划版本',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'draft/active/paused/completed/cancelled',
  `accepted_at` DATETIME DEFAULT NULL COMMENT '用户确认时间',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '计划备注',
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_feeding_plan_pet_status` (`pet_id`, `status`, `start_date`, `end_date`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='喂食计划';

CREATE TABLE IF NOT EXISTS `feeding_plan_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '喂食计划项ID',
  `feeding_plan_id` BIGINT NOT NULL COMMENT '喂食计划ID',
  `item_key` VARCHAR(64) NOT NULL COMMENT '计划内稳定键',
  `food_id` BIGINT NOT NULL COMMENT '食品卡ID',
  `meal_time` TIME NOT NULL COMMENT '计划时段',
  `amount` DECIMAL(12,3) NOT NULL COMMENT '每次目标量',
  `unit` VARCHAR(8) NOT NULL DEFAULT 'g' COMMENT 'g/ml',
  `percentage` DECIMAL(7,4) DEFAULT NULL COMMENT '每日比例',
  `cycle_type` VARCHAR(16) NOT NULL DEFAULT 'daily' COMMENT 'daily/weekly/custom',
  `cycle_value` VARCHAR(64) DEFAULT NULL COMMENT '周期配置',
  `nutrition_snapshot` JSON DEFAULT NULL COMMENT '计划创建时的营养快照',
  `sort_order` INT NOT NULL DEFAULT 0,
  `is_deleted` TINYINT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_feeding_plan_item_key` (`feeding_plan_id`, `item_key`, `is_deleted`),
  KEY `idx_feeding_plan_item_plan_time` (`feeding_plan_id`, `meal_time`, `is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='喂食计划项';

-- MySQL 8.0 不支持 ADD COLUMN IF NOT EXISTS / CREATE INDEX IF NOT EXISTS（该语法仅 MariaDB 支持），
-- 以下通过 information_schema 判断 + 动态 SQL 实现幂等，与上方 CREATE TABLE IF NOT EXISTS 保持一致。
-- feeding_record 补充喂食计划关联字段
SET @sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE `feeding_record` ADD COLUMN `feeding_plan_id` BIGINT DEFAULT NULL COMMENT ''来源喂食计划ID'' AFTER `food_id`', 'SELECT 1')
            FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feeding_record' AND COLUMN_NAME='feeding_plan_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE `feeding_record` ADD COLUMN `feeding_plan_item_id` BIGINT DEFAULT NULL COMMENT ''来源喂食计划项ID'' AFTER `feeding_plan_id`', 'SELECT 1')
            FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feeding_record' AND COLUMN_NAME='feeding_plan_item_id');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE `feeding_record` ADD COLUMN `planned_at` DATETIME DEFAULT NULL COMMENT ''计划时段'' AFTER `feeding_plan_item_id`', 'SELECT 1')
            FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feeding_record' AND COLUMN_NAME='planned_at');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*)=0, 'ALTER TABLE `feeding_record` ADD COLUMN `food_name_snapshot` VARCHAR(100) DEFAULT NULL COMMENT ''食品名称快照'' AFTER `planned_at`', 'SELECT 1')
            FROM information_schema.COLUMNS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feeding_record' AND COLUMN_NAME='food_name_snapshot');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- feeding_record 索引
SET @sql = (SELECT IF(COUNT(*)=0, 'CREATE INDEX `idx_feeding_record_plan` ON `feeding_record` (`feeding_plan_id`, `planned_at`, `is_deleted`)', 'SELECT 1')
            FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feeding_record' AND INDEX_NAME='idx_feeding_record_plan');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*)=0, 'CREATE UNIQUE INDEX `uk_feeding_record_plan_slot` ON `feeding_record` (`feeding_plan_id`, `feeding_plan_item_id`, `planned_at`, `is_deleted`)', 'SELECT 1')
            FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feeding_record' AND INDEX_NAME='uk_feeding_record_plan_slot');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*)=0, 'CREATE INDEX `idx_feeding_record_food_time` ON `feeding_record` (`pet_id`, `food_id`, `feeding_time`, `is_deleted`)', 'SELECT 1')
            FROM information_schema.STATISTICS WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='feeding_record' AND INDEX_NAME='idx_feeding_record_food_time');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
