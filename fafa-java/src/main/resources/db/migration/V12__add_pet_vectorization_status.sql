-- 宠物三视图向量化状态列。
-- 派发 Python 向量化前先落库 PENDING，Python 回调后更新为 COMPLETED/FAILED；
-- 存量宠物默认 completed（视为已完成向量化），避免旧数据被误判为待处理。
-- 沿用 V11 的 information_schema 守卫模式保证幂等。

SET @sql = (SELECT IF(COUNT(*)=0,
    'ALTER TABLE `pet` ADD COLUMN `vectorization_status` VARCHAR(20) NOT NULL DEFAULT ''completed'' COMMENT ''三视图向量化状态: pending/processing/completed/failed''',
    'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='pet' AND COLUMN_NAME='vectorization_status');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
