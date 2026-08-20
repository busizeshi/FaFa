-- 为所有表添加软删除字段
-- 执行前请备份数据库

USE fafa;

-- 用户表
ALTER TABLE `user` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 宠物表
ALTER TABLE `pet` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 照片表
ALTER TABLE `photo` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 喂食记录表
ALTER TABLE `feed_record` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 饮水记录表
ALTER TABLE `water_record` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 排便记录表
ALTER TABLE `excretion_record` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 体重记录表
ALTER TABLE `weight_record` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 事件记录表
ALTER TABLE `event_record` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 提醒表
ALTER TABLE `reminder` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 用户设置表
ALTER TABLE `user_settings` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 反馈表
ALTER TABLE `feedback` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- AI对话表
ALTER TABLE `ai_conversation` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- AI消息表
ALTER TABLE `ai_message` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- AI发现表
ALTER TABLE `ai_discovery` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 日记表
ALTER TABLE `diary` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 报告表
ALTER TABLE `report` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);

-- 宠物档案历史表
ALTER TABLE `pet_profile_history` 
ADD COLUMN `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否删除: 0-否, 1-是' AFTER `updated_at`,
ADD INDEX `idx_is_deleted` (`is_deleted`);
