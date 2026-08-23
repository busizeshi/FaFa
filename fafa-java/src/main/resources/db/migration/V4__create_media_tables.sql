-- ============================================================
-- Phase 4：素材管理与成长档案
-- 表：media_asset（媒体素材）、user_tag（用户标签，早期已建，本阶段启用）
-- 设计决策见 docs/dev/phase4-implementation-plan.md：
--   1. media_asset.tags 与 event_record.tags 为标签名快照（JSON），标签定义删除不影响快照
--   2. 宠物归属 = pet_id 当前归属 + recognition_confirmed 确认标志（识别回填待确认，确认/修正后置 1）
--   3. AI 分析状态回调驱动：photo 落库 pending → 回调 completed/failed；video 落库 skipped 不进 AI 链路
--   4. user_tag 为字典数据物理删除（无 is_deleted），usage_count 表示历史使用热度（取消打标不回退）
--   5. 存储配额复用 Phase 0 的 user_quota_usage（quota_type='STORAGE_USED'，单位字节），本脚本不涉及
-- ============================================================

-- 1. 媒体素材表（照片/视频）
CREATE TABLE IF NOT EXISTS `media_asset` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `user_id` BIGINT NOT NULL COMMENT '上传用户ID',
  `pet_id` BIGINT DEFAULT NULL COMMENT '归属宠物ID（用户指定或AI识别判定，待用户确认）',
  `media_type` VARCHAR(16) NOT NULL COMMENT '媒体类型: photo图片/video视频',
  `file_url` VARCHAR(512) NOT NULL COMMENT '对象存储URL',
  `file_name` VARCHAR(255) DEFAULT NULL COMMENT '原始文件名',
  `file_size` BIGINT NOT NULL COMMENT '文件大小（字节，存储配额计量）',
  `mime_type` VARCHAR(64) DEFAULT NULL COMMENT 'MIME类型',
  `tags` JSON DEFAULT NULL COMMENT '标签名数组（快照语义，标签定义删除不影响已有素材）',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'AI分析状态: pending待分析/processing分析中/completed已完成/failed失败/skipped跳过（视频不适用AI分析）',
  `ai_description` VARCHAR(1000) DEFAULT NULL COMMENT 'AI视觉理解描述（qwen-vl生成）',
  `ai_tags` JSON DEFAULT NULL COMMENT 'AI生成标签（qwen-vl生成）',
  `recognition_score` DECIMAL(5,4) DEFAULT NULL COMMENT '宠物识别相似度（cosine 0~1；用户手动指定归属时为NULL）',
  `recognition_confirmed` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '归属是否已确认（用户上传时指定/AI识别后确认识别/修正归属时置1）',
  `taken_at` DATETIME NOT NULL COMMENT '拍摄/发生时间（用户指定，缺省为上传时刻）',
  `error_message` VARCHAR(500) DEFAULT NULL COMMENT '分析失败原因（status=failed时记录）',
  `is_deleted` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_time` (`user_id`, `taken_at`),
  KEY `idx_pet_time` (`pet_id`, `taken_at`),
  KEY `idx_user_confirm` (`user_id`, `recognition_confirmed`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='媒体素材表（照片/视频，Phase 4 素材管理）';

-- 2. 用户标签表（早期建库产物，Phase 4 正式启用；已存在时本语句幂等跳过）
CREATE TABLE IF NOT EXISTS `user_tag` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `tag_name` VARCHAR(50) NOT NULL COMMENT '标签名称',
  `category` VARCHAR(20) DEFAULT NULL COMMENT '分类: pet-宠物名, location-地点, action-动作, object-物品, mood-心情, other-其他',
  `usage_count` INT NOT NULL DEFAULT 0 COMMENT '使用次数（打标时+1，取消打标不回退，表示历史使用热度）',
  `last_used_at` DATETIME DEFAULT NULL COMMENT '最后使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tag_name` (`user_id`, `tag_name`),
  KEY `idx_user_tag_user_id` (`user_id`),
  KEY `idx_user_tag_usage` (`user_id`, `usage_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户标签表';
