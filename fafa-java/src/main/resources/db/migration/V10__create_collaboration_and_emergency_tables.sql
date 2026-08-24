-- Phase 10：家庭协同、临时照护、应急信息卡、访问审计和共享提醒投递
-- 设计原则：pet.user_id 继续表示资源 owner，operator_id 只表示本次实际操作人。

CREATE TABLE IF NOT EXISTS `pet_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '宠物协作者ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物空间ID',
  `user_id` BIGINT NOT NULL COMMENT '协作者用户ID，主人由 pet.user_id 派生',
  `role` VARCHAR(16) NOT NULL COMMENT 'member/caregiver',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/revoked/expired',
  `granted_by` BIGINT NOT NULL COMMENT '授权人用户ID',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME DEFAULT NULL COMMENT '临时照护到期时间',
  `revoked_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_member_user` (`pet_id`, `user_id`),
  KEY `idx_pet_member_active` (`pet_id`, `status`, `expires_at`),
  KEY `idx_pet_member_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物空间协作者';

CREATE TABLE IF NOT EXISTS `pet_invitation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
  `pet_id` BIGINT NOT NULL,
  `created_by` BIGINT NOT NULL COMMENT '主人用户ID',
  `role` VARCHAR(16) NOT NULL COMMENT 'member/caregiver',
  `token_hash` CHAR(64) NOT NULL COMMENT '邀请token SHA-256摘要，不保存明文',
  `expires_at` DATETIME NOT NULL,
  `member_expires_at` DATETIME DEFAULT NULL COMMENT '临时照护成员权限到期时间',
  `max_uses` INT NOT NULL DEFAULT 1,
  `used_count` INT NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/accepted/expired/revoked',
  `accepted_by` BIGINT DEFAULT NULL,
  `accepted_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_invitation_token` (`token_hash`),
  KEY `idx_pet_invitation_status_expire` (`pet_id`, `status`, `expires_at`),
  KEY `idx_pet_invitation_creator` (`created_by`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物空间一次性邀请';

CREATE TABLE IF NOT EXISTS `pet_emergency_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '应急资料ID',
  `pet_id` BIGINT NOT NULL,
  `blood_type` VARCHAR(32) DEFAULT NULL,
  `allergies` VARCHAR(1000) DEFAULT NULL,
  `chronic_conditions` VARCHAR(1000) DEFAULT NULL,
  `emergency_contact_name` VARCHAR(64) DEFAULT NULL,
  `emergency_contact_phone` VARCHAR(32) DEFAULT NULL,
  `primary_hospital_name` VARCHAR(200) DEFAULT NULL,
  `primary_hospital_phone` VARCHAR(32) DEFAULT NULL,
  `primary_hospital_address` VARCHAR(500) DEFAULT NULL,
  `notes` VARCHAR(1000) DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 1,
  `updated_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_emergency_profile_pet` (`pet_id`),
  KEY `idx_pet_emergency_updated` (`pet_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物应急资料';

CREATE TABLE IF NOT EXISTS `access_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
  `request_id` VARCHAR(64) DEFAULT NULL,
  `pet_id` BIGINT DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL COMMENT '当前请求用户',
  `operator_id` BIGINT DEFAULT NULL COMMENT '实际执行人，系统任务可为空',
  `operation` VARCHAR(64) NOT NULL,
  `resource_type` VARCHAR(64) NOT NULL,
  `resource_id` VARCHAR(64) DEFAULT NULL,
  `result` VARCHAR(16) NOT NULL COMMENT 'success/denied/failed',
  `reason` VARCHAR(128) DEFAULT NULL,
  `metadata_json` JSON DEFAULT NULL COMMENT '禁止保存token、请求头和完整医疗原文',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_access_audit_pet_time` (`pet_id`, `created_at`),
  KEY `idx_access_audit_operator_time` (`operator_id`, `created_at`),
  KEY `idx_access_audit_request` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物空间访问与操作审计';

CREATE TABLE IF NOT EXISTS `reminder_notification_delivery` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reminder_id` BIGINT NOT NULL,
  `pet_id` BIGINT NOT NULL,
  `recipient_user_id` BIGINT NOT NULL,
  `delivery_window` VARCHAR(32) NOT NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/sent/failed/skipped',
  `provider_message_id` VARCHAR(128) DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `retry_count` INT NOT NULL DEFAULT 0,
  `pushed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reminder_delivery_recipient_window` (`reminder_id`, `recipient_user_id`, `delivery_window`),
  KEY `idx_reminder_delivery_scan` (`status`, `created_at`),
  KEY `idx_reminder_delivery_pet` (`pet_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='共享提醒成员投递幂等记录';

ALTER TABLE `medical_visit`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '实际操作人' AFTER `pet_id`;
ALTER TABLE `medication_plan`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '实际操作人' AFTER `pet_id`;
ALTER TABLE `media_asset`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '实际上传/操作人' AFTER `pet_id`;
ALTER TABLE `food_card`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '确认或维护人' AFTER `pet_id`;
ALTER TABLE `food_transition_plan`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '实际操作人' AFTER `pet_id`;
ALTER TABLE `feeding_plan`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '实际操作人' AFTER `pet_id`;
ALTER TABLE `expense_record`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '实际操作人' AFTER `pet_id`;
ALTER TABLE `pet_report`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '手动生成或确认人，系统生成可为空' AFTER `pet_id`;
ALTER TABLE `content_draft`
  ADD COLUMN IF NOT EXISTS `operator_id` BIGINT DEFAULT NULL COMMENT '手动生成或编辑人' AFTER `pet_id`;

CREATE INDEX IF NOT EXISTS `idx_media_asset_operator` ON `media_asset` (`operator_id`, `created_at`);
CREATE INDEX IF NOT EXISTS `idx_accessible_operator_medical_visit` ON `medical_visit` (`operator_id`, `updated_at`);
CREATE INDEX IF NOT EXISTS `idx_accessible_operator_expense` ON `expense_record` (`operator_id`, `updated_at`);
