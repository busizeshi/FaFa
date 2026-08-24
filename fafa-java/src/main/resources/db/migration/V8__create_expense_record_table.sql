-- Phase 8：宠物日常开支与费用统计

CREATE TABLE IF NOT EXISTS `expense_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '开支记录ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT NOT NULL COMMENT '所属宠物ID',
  `category` VARCHAR(24) NOT NULL COMMENT 'food/medical/supplies/grooming/insurance/other',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '金额，当前仅支持正数人民币',
  `currency` CHAR(3) NOT NULL DEFAULT 'CNY' COMMENT '货币代码',
  `expense_date` DATE NOT NULL COMMENT '发生日期',
  `title` VARCHAR(100) NOT NULL COMMENT '开支标题',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `receipt_media_id` BIGINT DEFAULT NULL COMMENT '小票素材ID',
  `food_card_id` BIGINT DEFAULT NULL COMMENT '关联食品卡ID',
  `food_name_snapshot` VARCHAR(100) DEFAULT NULL COMMENT '食品名称快照',
  `medical_visit_id` BIGINT DEFAULT NULL COMMENT '来源就医记录ID',
  `source_type` VARCHAR(24) NOT NULL DEFAULT 'manual' COMMENT 'manual/medical_visit',
  `system_generated` TINYINT NOT NULL DEFAULT 0 COMMENT '是否系统生成',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expense_medical_visit` (`medical_visit_id`),
  KEY `idx_expense_user_pet_date` (`user_id`, `pet_id`, `expense_date`, `is_deleted`),
  KEY `idx_expense_user_category_date` (`user_id`, `category`, `expense_date`, `is_deleted`),
  KEY `idx_expense_food_card` (`food_card_id`, `expense_date`),
  KEY `idx_expense_receipt_media` (`receipt_media_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物开支记录';
