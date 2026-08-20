-- ============================================
-- FaFa 宠物生活助手 - 数据库结构脚本
-- ============================================
-- 数据库：MySQL 8.0+
-- 字符集：utf8mb4
-- 排序规则：utf8mb4_unicode_ci
-- 生成时间：2026-08-19
-- ============================================

-- 设置数据库
USE fafa;

-- 设置字符集
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- 设置时区
SET time_zone = '+08:00';

-- ============================================
-- 用户模块
-- ============================================

-- 用户表
DROP TABLE IF EXISTS `user`;
CREATE TABLE `user` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  `openid` VARCHAR(64) NOT NULL COMMENT '微信OpenID',
  `unionid` VARCHAR(64) NULL COMMENT '微信UnionID',
  `nickname` VARCHAR(100) NULL COMMENT '微信昵称',
  `avatar` VARCHAR(500) NULL COMMENT '微信头像URL',
  `phone` VARCHAR(20) NULL COMMENT '手机号',
  `gender` TINYINT NULL COMMENT '性别: 0-未知, 1-男, 2-女',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-正常',
  `register_date` DATE NOT NULL COMMENT '注册日期',
  `last_login_at` DATETIME NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_user_openid` (`openid`),
  KEY `idx_user_unionid` (`unionid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 用户设置表
DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `reminder_notification` TINYINT NOT NULL DEFAULT 1 COMMENT '提醒通知: 0-关闭, 1-开启',
  `report_notification` TINYINT NOT NULL DEFAULT 1 COMMENT '报告通知: 0-关闭, 1-开启',
  `allow_ai_analysis` TINYINT NOT NULL DEFAULT 1 COMMENT '允许AI分析: 0-关闭, 1-开启',
  `default_pet_id` BIGINT NULL COMMENT '默认选中的宠物ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_settings_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户设置表';

-- ============================================
-- 宠物模块
-- ============================================

-- 宠物表
DROP TABLE IF EXISTS `pet`;
CREATE TABLE `pet` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `name` VARCHAR(50) NOT NULL COMMENT '宠物昵称',
  `avatar` VARCHAR(500) NULL COMMENT '宠物头像URL',
  `species` VARCHAR(20) NOT NULL COMMENT '种类: cat-猫, dog-狗, other-其他',
  `breed` VARCHAR(50) NULL COMMENT '品种',
  `gender` VARCHAR(20) NULL COMMENT '性别: male-公, female-母, unknown-未知',
  `birth_date` DATE NULL COMMENT '出生日期',
  `adopt_date` DATE NULL COMMENT '到家日期',
  `weight` DECIMAL(5, 2) NULL COMMENT '当前体重(kg)',
  `is_neutered` TINYINT NULL COMMENT '是否绝育: 0-否, 1-是',
  `coat_color` VARCHAR(50) NULL COMMENT '毛色',
  `remarks` TEXT NULL COMMENT '备注说明',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-已离世, 1-正常',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序字段',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_pet_user_id` (`user_id`),
  KEY `idx_pet_user_sort` (`user_id`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物表';

-- 宠物档案历史表
DROP TABLE IF EXISTS `pet_profile_history`;
CREATE TABLE `pet_profile_history` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `field_name` VARCHAR(50) NOT NULL COMMENT '字段名',
  `old_value` TEXT NULL COMMENT '旧值',
  `new_value` TEXT NULL COMMENT '新值',
  `changed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '变更时间',
  KEY `idx_history_pet_id` (`pet_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物档案历史表';

-- ============================================
-- 记录模块
-- ============================================

-- 喂食记录表
DROP TABLE IF EXISTS `feed_record`;
CREATE TABLE `feed_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '记录人ID',
  `feed_time` DATETIME NOT NULL COMMENT '喂食时间',
  `food_name` VARCHAR(100) NOT NULL COMMENT '食物名称',
  `food_type` VARCHAR(20) NULL COMMENT '食物类型: main-主粮, snack-零食, wet-罐头, other-其他',
  `amount` VARCHAR(50) NULL COMMENT '喂食量(如: 35g, 1罐)',
  `unit` VARCHAR(10) NULL COMMENT '单位: g-克, ml-毫升, can-罐, piece-个',
  `brand` VARCHAR(100) NULL COMMENT '品牌',
  `remarks` TEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_feed_pet_id` (`pet_id`),
  KEY `idx_feed_pet_time` (`pet_id`, `feed_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='喂食记录表';

-- 体重记录表
DROP TABLE IF EXISTS `weight_record`;
CREATE TABLE `weight_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '记录人ID',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `weight` DECIMAL(5, 2) NOT NULL COMMENT '体重(kg)',
  `bcs_score` TINYINT NULL COMMENT '体况评分(1-9)',
  `remarks` TEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_weight_pet_id` (`pet_id`),
  KEY `idx_weight_pet_date` (`pet_id`, `record_date`),
  UNIQUE KEY `uk_weight_pet_date` (`pet_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体重记录表';

-- 饮水记录表
DROP TABLE IF EXISTS `water_record`;
CREATE TABLE `water_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '记录人ID',
  `record_time` DATETIME NOT NULL COMMENT '记录时间',
  `amount` INT NOT NULL COMMENT '饮水量(ml)',
  `remarks` TEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_water_pet_id` (`pet_id`),
  KEY `idx_water_pet_time` (`pet_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮水记录表';

-- 排便记录表
DROP TABLE IF EXISTS `excretion_record`;
CREATE TABLE `excretion_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '记录人ID',
  `record_time` DATETIME NOT NULL COMMENT '记录时间',
  `type` VARCHAR(20) NOT NULL COMMENT '类型: urine-排尿, feces-排便',
  `status` VARCHAR(20) NULL COMMENT '状态: normal-正常, soft-偏软, diarrhea-腹泻, constipation-便秘, bloody-带血',
  `color` VARCHAR(50) NULL COMMENT '颜色描述',
  `remarks` TEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_excretion_pet_id` (`pet_id`),
  KEY `idx_excretion_pet_time` (`pet_id`, `record_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排便记录表';

-- 事件记录表
DROP TABLE IF EXISTS `event_record`;
CREATE TABLE `event_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '记录人ID',
  `event_type` VARCHAR(50) NOT NULL COMMENT '事件类型: bath-洗澡, grooming-美容, vet-就医, vaccine-疫苗, deworming-驱虫, nail-剪指甲, play-玩耍, other-其他',
  `event_time` DATETIME NOT NULL COMMENT '事件时间',
  `title` VARCHAR(200) NOT NULL COMMENT '事件标题',
  `content` TEXT NULL COMMENT '事件内容',
  `tags` JSON NULL COMMENT '标签数组',
  `location` VARCHAR(200) NULL COMMENT '地点',
  `cost` DECIMAL(10, 2) NULL COMMENT '费用',
  `images` JSON NULL COMMENT '图片URL数组',
  `remarks` TEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_event_pet_id` (`pet_id`),
  KEY `idx_event_pet_time` (`pet_id`, `event_time`),
  KEY `idx_event_type` (`event_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='事件记录表';

-- ============================================
-- 成长档案模块
-- ============================================

-- 照片表
DROP TABLE IF EXISTS `photo`;
CREATE TABLE `photo` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '上传人ID',
  `url` VARCHAR(500) NOT NULL COMMENT '照片URL',
  `thumbnail_url` VARCHAR(500) NULL COMMENT '缩略图URL',
  `original_url` VARCHAR(500) NULL COMMENT '原图URL',
  `taken_at` DATETIME NULL COMMENT '拍摄时间',
  `upload_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间',
  `description` TEXT NULL COMMENT '照片说明',
  `tags` JSON NULL COMMENT '标签数组',
  `ai_tags` JSON NULL COMMENT 'AI识别的标签',
  `ai_description` TEXT NULL COMMENT 'AI生成的描述',
  `embedding_id` VARCHAR(100) NULL COMMENT '向量ID(关联Qdrant)',
  `width` INT NULL COMMENT '图片宽度',
  `height` INT NULL COMMENT '图片高度',
  `file_size` BIGINT NULL COMMENT '文件大小(字节)',
  `is_cover` TINYINT NOT NULL DEFAULT 0 COMMENT '是否封面: 0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_photo_pet_id` (`pet_id`),
  KEY `idx_photo_pet_taken` (`pet_id`, `taken_at`),
  KEY `idx_photo_embedding` (`embedding_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='照片表';

-- 日记表
DROP TABLE IF EXISTS `diary`;
CREATE TABLE `diary` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '作者ID',
  `diary_date` DATE NOT NULL COMMENT '日记日期',
  `title` VARCHAR(200) NULL COMMENT '日记标题',
  `content` TEXT NOT NULL COMMENT '日记内容',
  `mood` VARCHAR(20) NULL COMMENT '心情: happy-开心, calm-平静, worried-担心, sad-难过',
  `weather` VARCHAR(20) NULL COMMENT '天气',
  `images` JSON NULL COMMENT '图片URL数组',
  `tags` JSON NULL COMMENT '标签数组',
  `is_ai_assisted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否AI辅助: 0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_diary_pet_id` (`pet_id`),
  KEY `idx_diary_pet_date` (`pet_id`, `diary_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日记表';

-- ============================================
-- 提醒系统模块
-- ============================================

-- 提醒表
DROP TABLE IF EXISTS `reminder`;
CREATE TABLE `reminder` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '创建人ID',
  `title` VARCHAR(200) NOT NULL COMMENT '提醒标题',
  `reminder_type` VARCHAR(50) NOT NULL COMMENT '提醒类型: feed-喂食, vaccine-疫苗, deworming-驱虫, bath-洗澡, grooming-美容, vet-复诊, medicine-吃药, other-其他',
  `remind_time` DATETIME NOT NULL COMMENT '提醒时间',
  `repeat_type` VARCHAR(20) NOT NULL DEFAULT 'once' COMMENT '重复类型: once-一次, daily-每天, weekly-每周, monthly-每月, custom-自定义',
  `repeat_config` JSON NULL COMMENT '重复配置(如: {"days": [1,3,5]})',
  `advance_minutes` INT NOT NULL DEFAULT 0 COMMENT '提前提醒分钟数',
  `description` TEXT NULL COMMENT '提醒描述',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待办, completed-已完成, cancelled-已取消, expired-已过期',
  `completed_at` DATETIME NULL COMMENT '完成时间',
  `completion_note` TEXT NULL COMMENT '完成备注',
  `completion_images` JSON NULL COMMENT '完成时上传的图片',
  `is_notified` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已通知: 0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_reminder_pet_id` (`pet_id`),
  KEY `idx_reminder_pet_time` (`pet_id`, `remind_time`),
  KEY `idx_reminder_status` (`status`),
  KEY `idx_reminder_time_status` (`remind_time`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='提醒表';

-- ============================================
-- 报告模块
-- ============================================

-- 报告表
DROP TABLE IF EXISTS `report`;
CREATE TABLE `report` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `report_type` VARCHAR(20) NOT NULL COMMENT '报告类型: weekly-周报, monthly-月报',
  `period_start` DATE NOT NULL COMMENT '周期开始日期',
  `period_end` DATE NOT NULL COMMENT '周期结束日期',
  `title` VARCHAR(200) NOT NULL COMMENT '报告标题',
  `summary` TEXT NULL COMMENT 'AI总结',
  `content` JSON NOT NULL COMMENT '报告内容(JSON结构)',
  `cover_image` VARCHAR(500) NULL COMMENT '封面图URL',
  `share_image` VARCHAR(500) NULL COMMENT '分享海报URL',
  `status` VARCHAR(20) NOT NULL DEFAULT 'draft' COMMENT '状态: draft-草稿, published-已发布',
  `generated_at` DATETIME NOT NULL COMMENT '生成时间',
  `view_count` INT NOT NULL DEFAULT 0 COMMENT '查看次数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_report_pet_id` (`pet_id`),
  KEY `idx_report_pet_period` (`pet_id`, `period_start`, `period_end`),
  KEY `idx_report_type` (`report_type`),
  UNIQUE KEY `uk_report_pet_period_type` (`pet_id`, `period_start`, `period_end`, `report_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报告表';

-- ============================================
-- AI 模块
-- ============================================

-- AI对话会话表
DROP TABLE IF EXISTS `ai_conversation`;
CREATE TABLE `ai_conversation` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `pet_id` BIGINT NULL COMMENT '关联宠物ID(可为空)',
  `title` VARCHAR(200) NULL COMMENT '对话标题',
  `context_type` VARCHAR(50) NULL COMMENT '上下文类型: general-通用, data_query-数据查询, photo_search-照片搜索, diary_write-日记写作',
  `last_message_at` DATETIME NOT NULL COMMENT '最后消息时间',
  `message_count` INT NOT NULL DEFAULT 0 COMMENT '消息数量',
  `is_archived` TINYINT NOT NULL DEFAULT 0 COMMENT '是否归档: 0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_conversation_user_id` (`user_id`),
  KEY `idx_conversation_pet_id` (`pet_id`),
  KEY `idx_conversation_last_msg` (`user_id`, `last_message_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话表';

-- AI消息表
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `conversation_id` BIGINT NOT NULL COMMENT '会话ID',
  `role` VARCHAR(20) NOT NULL COMMENT '角色: user-用户, assistant-AI, system-系统',
  `content` TEXT NOT NULL COMMENT '消息内容',
  `content_type` VARCHAR(20) NOT NULL DEFAULT 'text' COMMENT '内容类型: text-文本, image-图片, data-数据卡片',
  `metadata` JSON NULL COMMENT '元数据(如: 引用的数据、图片URL等)',
  `token_count` INT NULL COMMENT 'Token消耗数',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_message_conversation` (`conversation_id`),
  KEY `idx_message_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI消息表';

-- AI发现表
DROP TABLE IF EXISTS `ai_discovery`;
CREATE TABLE `ai_discovery` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `discovery_type` VARCHAR(50) NOT NULL COMMENT '发现类型: weight_trend-体重趋势, diet_pattern-饮食规律, health_alert-健康提醒, milestone-成长里程碑',
  `title` VARCHAR(200) NOT NULL COMMENT '标题',
  `content` TEXT NOT NULL COMMENT '内容',
  `insight_data` JSON NULL COMMENT '洞察数据',
  `priority` TINYINT NOT NULL DEFAULT 1 COMMENT '优先级: 1-低, 2-中, 3-高',
  `is_read` TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读: 0-未读, 1-已读',
  `read_at` DATETIME NULL COMMENT '阅读时间',
  `generated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生成时间',
  `expires_at` DATETIME NULL COMMENT '过期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_discovery_pet_id` (`pet_id`),
  KEY `idx_discovery_user_id` (`user_id`),
  KEY `idx_discovery_generated` (`generated_at`),
  KEY `idx_discovery_unread` (`user_id`, `is_read`, `generated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI发现表';

-- ============================================
-- 系统模块
-- ============================================

-- 用户反馈表
DROP TABLE IF EXISTS `feedback`;
CREATE TABLE `feedback` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `type` VARCHAR(20) NOT NULL COMMENT '反馈类型: feature-功能建议, bug-Bug反馈, other-其他',
  `content` TEXT NOT NULL COMMENT '反馈内容',
  `images` JSON NULL COMMENT '截图URL数组',
  `contact` VARCHAR(100) NULL COMMENT '联系方式',
  `status` VARCHAR(20) NOT NULL DEFAULT 'pending' COMMENT '状态: pending-待处理, replied-已回复, closed-已关闭',
  `admin_reply` TEXT NULL COMMENT '管理员回复',
  `replied_at` DATETIME NULL COMMENT '回复时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_feedback_user_id` (`user_id`),
  KEY `idx_feedback_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈表';

-- 系统配置表
DROP TABLE IF EXISTS `system_config`;
CREATE TABLE `system_config` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `config_key` VARCHAR(100) NOT NULL COMMENT '配置键',
  `config_value` TEXT NOT NULL COMMENT '配置值',
  `config_type` VARCHAR(20) NOT NULL DEFAULT 'string' COMMENT '值类型: string, number, boolean, json',
  `description` VARCHAR(500) NULL COMMENT '配置说明',
  `is_public` TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开: 0-否, 1-是',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统配置表';

-- 操作日志表
DROP TABLE IF EXISTS `operation_log`;
CREATE TABLE `operation_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `user_id` BIGINT NULL COMMENT '用户ID',
  `operation` VARCHAR(100) NOT NULL COMMENT '操作类型',
  `resource_type` VARCHAR(50) NULL COMMENT '资源类型',
  `resource_id` BIGINT NULL COMMENT '资源ID',
  `details` JSON NULL COMMENT '操作详情',
  `ip` VARCHAR(50) NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) NULL COMMENT 'User Agent',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_log_user_id` (`user_id`),
  KEY `idx_log_operation` (`operation`),
  KEY `idx_log_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ============================================
-- 初始数据
-- ============================================

-- 系统配置初始数据
INSERT INTO `system_config` (`config_key`, `config_value`, `config_type`, `description`, `is_public`) VALUES
('app_name', 'FaFa', 'string', '应用名称', 1),
('app_version', '1.0.0', 'string', '应用版本', 1),
('ai_model', 'qwen3.7-flash', 'string', 'AI模型名称', 0),
('max_pets_per_user', '10', 'number', '每个用户最多宠物数', 0),
('max_photo_size_mb', '10', 'number', '照片最大尺寸(MB)', 0),
('report_generate_day', '1', 'number', '周报生成日(1-7, 1为周一)', 0);

-- ============================================
-- 脚本结束
-- ============================================
