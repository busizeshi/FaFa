-- 同一用户的有效宠物名称必须唯一。
-- 使用生成列让逻辑删除记录的 active_name 为 NULL，从而允许删除后复用名称。
-- MySQL 8.0 不支持 CREATE UNIQUE INDEX IF NOT EXISTS，使用 information_schema 保证幂等。

SET @sql = (SELECT IF(COUNT(*)=0,
    'ALTER TABLE `pet` ADD COLUMN `active_name` VARCHAR(32) GENERATED ALWAYS AS (IF(`is_deleted` = 0, `name`, NULL)) STORED',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet' AND COLUMN_NAME='active_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = (SELECT IF(COUNT(*)=0,
    'CREATE UNIQUE INDEX `uk_pet_user_active_name` ON `pet` (`user_id`, `active_name`)',
    'SELECT 1')
    FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet' AND INDEX_NAME='uk_pet_user_active_name');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
