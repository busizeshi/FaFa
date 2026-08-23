-- ============================================================
-- Phase 2：日常记录系统
-- 三类记录：状态观察（daily_status）/ 照护动作（feeding_record）/ 周期测量（weight_record）
--
-- 设计说明（相对《分阶段开发计划》6.4 的调整）：
-- 1. daily_status 不设逻辑删除列：一天一格 upsert 语义下，删除即当天回到
--    「未记录」状态，物理删除最贴合语义；且唯一键含 deleted 的方案在
--    「记录→删除→再记录→再删除」循环中会撞唯一键。脚本幂等（IF NOT EXISTS）。
-- 2. 枚举维度用 VARCHAR(16) 而非 MySQL ENUM：与 pet.type / pet.gender 的既有
--    约定一致，取值合法性由 Java 领域枚举校验，扩展枚举值无需变更表结构。
-- 3. feeding_record 增加 unit 字段（g/ml）：喂食量跨单位（克/毫升），
--    区分单位是后续 AI 喂养分析的数据正确性前提。
-- 4. 物种记录配置不建表：配置与代码强耦合（新增维度必然伴随枚举与前端
--    改造），由领域层 SpeciesRecordPolicy 提供，接口暴露给前端渲染。
-- ============================================================

-- 1. 每日状态记录表（状态观察：一天一格，upsert 语义）
CREATE TABLE IF NOT EXISTS `daily_status` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `record_date` DATE NOT NULL COMMENT '记录日期（一天一格）',
  `appetite` VARCHAR(16) DEFAULT NULL COMMENT '食欲: excellent/normal/decreased/refused',
  `energy` VARCHAR(16) DEFAULT NULL COMMENT '精神活动: active/normal/lethargic',
  `water_intake` VARCHAR(16) DEFAULT NULL COMMENT '饮水程度: normal/more/less/not_noticed',
  `defecation` VARCHAR(16) DEFAULT NULL COMMENT '排便: normal/soft/diarrhea/constipation/not_seen',
  `vomiting` VARCHAR(16) DEFAULT 'none' COMMENT '呕吐: none/once/multiple',
  `urination` VARCHAR(16) DEFAULT NULL COMMENT '排尿: normal/abnormal/not_seen',
  `appetite_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '进食克数（选填）',
  `water_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '饮水毫升数（选填）',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_date` (`pet_id`, `record_date`),
  KEY `idx_record_date` (`record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日状态记录表（状态观察，一天一格）';

-- 2. 喂食记录表（照护动作：事件流，一天多条）
CREATE TABLE IF NOT EXISTS `feeding_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `feeding_time` DATETIME NOT NULL COMMENT '喂食时间',
  `food_type` VARCHAR(16) NOT NULL COMMENT '食物类型: main_food/snack/can/other',
  `amount` DECIMAL(10,2) DEFAULT NULL COMMENT '喂食量数值',
  `unit` VARCHAR(8) NOT NULL DEFAULT 'g' COMMENT '单位: g/ml',
  `food_id` BIGINT DEFAULT NULL COMMENT '关联食品管理卡ID（Phase 7 预留）',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_time` (`pet_id`, `feeding_time`),
  KEY `idx_food` (`food_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='喂食记录表（照护动作，事件流）';

-- 3. 体重记录表（周期测量：独立入口，不入每日打卡）
CREATE TABLE IF NOT EXISTS `weight_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pet_id` BIGINT NOT NULL COMMENT '宠物ID',
  `record_date` DATE NOT NULL COMMENT '记录日期',
  `weight` DECIMAL(5,2) NOT NULL COMMENT '体重（kg），与 pet.weight 保持一致',
  `notes` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT DEFAULT NULL COMMENT '记录人ID（家庭协同预留）',
  `is_deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除标记',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_pet_date` (`pet_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='体重记录表（周期测量）';
