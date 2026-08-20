-- 添加用户注销相关字段
-- 用于支持注销冷静期功能

-- 1. 添加注销请求时间字段
ALTER TABLE `user` ADD COLUMN `deletion_requested_at` DATETIME NULL COMMENT '注销请求时间' AFTER `last_login_at`;

-- 2. 更新用户状态枚举，添加"待注销"状态
-- 0: 禁用
-- 1: 正常
-- 2: 待注销
ALTER TABLE `user` MODIFY COLUMN `status` TINYINT NOT NULL DEFAULT 1 COMMENT '用户状态: 0-禁用 1-正常 2-待注销';

-- 3. 为查询优化添加索引
CREATE INDEX `idx_status_deletion_time` ON `user` (`status`, `deletion_requested_at`);
