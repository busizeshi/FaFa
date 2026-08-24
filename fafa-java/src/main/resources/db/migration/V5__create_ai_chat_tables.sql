CREATE TABLE IF NOT EXISTS `ai_chat_session` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT DEFAULT NULL COMMENT '当前宠物上下文',
  `title` VARCHAR(80) DEFAULT NULL COMMENT '会话标题',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/deleted',
  `last_message_at` DATETIME DEFAULT NULL COMMENT '最近消息时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  PRIMARY KEY (`id`),
  KEY `idx_chat_session_user_time` (`user_id`, `status`, `last_message_at`),
  KEY `idx_chat_session_user_pet` (`user_id`, `pet_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话会话';

CREATE TABLE IF NOT EXISTS `ai_chat_message` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `session_id` BIGINT NOT NULL COMMENT '会话ID',
  `user_id` BIGINT NOT NULL COMMENT '所属用户ID',
  `pet_id` BIGINT DEFAULT NULL COMMENT '本条消息宠物上下文',
  `role` VARCHAR(16) NOT NULL COMMENT 'user/assistant/tool/system',
  `content` TEXT NOT NULL COMMENT '消息正文',
  `client_message_id` VARCHAR(64) DEFAULT NULL COMMENT '客户端幂等键',
  `blocks_json` JSON DEFAULT NULL COMMENT '结构化回答卡片',
  `citations_json` JSON DEFAULT NULL COMMENT '数据证据引用',
  `tool_calls_json` JSON DEFAULT NULL COMMENT '工具调用摘要',
  `record_draft_json` JSON DEFAULT NULL COMMENT '自然语言记录草稿',
  `model` VARCHAR(64) DEFAULT NULL COMMENT '模型标识',
  `prompt_tokens` INT DEFAULT NULL COMMENT '输入 token 数',
  `completion_tokens` INT DEFAULT NULL COMMENT '输出 token 数',
  `assistant_status` VARCHAR(16) NOT NULL DEFAULT 'answered' COMMENT 'answered/failed',
  `trace_id` VARCHAR(64) DEFAULT NULL COMMENT '链路追踪ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chat_message_client` (`session_id`, `client_message_id`),
  KEY `idx_chat_message_session_time` (`session_id`, `id`),
  KEY `idx_chat_message_user_time` (`user_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI对话消息';
