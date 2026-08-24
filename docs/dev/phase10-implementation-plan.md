# Phase 10 实施文档：家庭协同与上线打磨

> 依据：《FaFa产品功能设计-最终版.md》第十三章“家庭协同”、第十四章“应急信息卡”、第十八章版本规划；《分阶段开发计划.md》Phase 10；《.agent/development-guidelines.md》；Phase 0～9 已落地代码、数据表和接口契约。
>
> 计划日期：2026-08-23
>
> 状态：**核心代码已实施，待真实环境联调**
>
> 前置假设：Phase 0～9 的核心功能已经完成，Java 是业务事实、权限和持久化的唯一写入方；小程序是用户操作入口；Python Agent 继续保持只读，不参与成员管理、邀请、授权或应急卡写入。

Phase 10 的目标不是把 FaFa 做成社交平台，而是把“一只宠物的数据空间”从单用户使用升级为私密、可撤销、可审计的家庭协作空间，并完成一次面向真实用户的上线加固。核心原则是：资源归属不变、访问权限集中判断、每次操作可追溯、临时授权自动失效、敏感数据最小暴露。

---

## 1. 阶段目标与完成定义

### 1.1 用户目标

| 能力 | 用户结果 | 不做的事情 |
|---|---|---|
| 宠物空间成员 | 主人可以邀请家人共同查看和记录同一只宠物 | 不开放公开动态、评论、点赞、陌生人发现 |
| 角色权限 | 主人、成员、临时照护者看到不同功能和操作范围 | 不把前端隐藏按钮当作授权手段 |
| 临时照护 | 出差时给朋友或宠物店一个有明确失效时间的权限 | 不提供永久共享链接，不允许转授权 |
| 操作留痕 | 每条新增/修改/完成动作能看到操作人 | 不把协作者 userId 覆盖到资源 owner 字段 |
| 共享提醒 | 成员可以看到共同宠物的提醒，符合条件的成员接收通知 | 不重复推送，不因为一个成员失败阻断其他成员 |
| 应急信息卡 | 主人维护血型、过敏史、慢性病、紧急联系人和常去医院，快速查看并保存图片 | 不自动诊断、不自动补全医疗事实、不生成公开 URL |
| 上线打磨 | 具备迁移、回滚、监控、限流、备份、灰度和应急开关 | 不在本阶段接入支付、社交发布、复杂工作流 |

### 1.2 阶段完成定义

Phase 10 只有同时满足以下条件，才可以把阶段状态改为“已完成”：

1. 主人可以创建、查看、撤销成员和邀请；成员无法越权管理成员或修改宠物主人资料。
2. 邀请 token 只保存摘要，具有过期时间、一次性消费和并发消费保护。
3. 临时照护者到期后，所有读写接口均拒绝访问，不依赖前端页面状态。
4. Phase 2～9 的主要宠物数据写入链路都经过统一权限服务，并正确记录 `operator_id` 或等价操作人字段。
5. 共享提醒按“宠物 + 接收人 + 推送窗口”幂等，不因重复扫描造成重复通知。
6. 应急信息卡只展示已保存事实，用户可以在小程序端生成图片保存到本地。
7. V10 Flyway 在已有 Phase 9 数据库上成功执行，旧数据的 owner 访问行为不改变。
8. 安全、并发、越权、迁移、回滚、监控和灰度验收全部通过；Java、Python 和小程序检查均通过。

---

## 2. 范围、边界与明确决策

### 2.1 本阶段范围

- 宠物空间成员模型和权限策略。
- 主人邀请成员、成员接受邀请、主人撤销成员、临时照护自动到期。
- 统一宠物访问检查，替换只允许 `pet.user_id` 主人的旧检查路径。
- 记录写入操作人追踪和访问审计日志。
- 共享提醒可见性和按成员推送。
- 应急信息资料维护、事实卡 JSON 接口和小程序图片保存。
- 生产上线前的配置、限流、监控、备份、迁移、回滚、灰度和应急开关。

### 2.2 不在本阶段范围

- 公开社交、关注、点赞、评论、搜索其他用户。
- 支付、会员购买、发票和第三方支付回调。
- 外部平台自动发布小红书、抖音或朋友圈内容。
- 家庭成员之间的聊天和文件协同。
- 复杂的组织架构、团队层级和跨宠物批量授权。
- AI 自动判断成员关系或自动修改权限。
- 将应急卡发布为永久公网链接。

### 2.3 关键决策

1. `pet.user_id` 继续表示宠物空间主人和资源归属人，所有历史 `user_id` 过滤口径不改变。
2. `pet_member` 只保存主人之外的协作者；主人权限由 `pet.user_id` 派生，避免出现两个“主人”来源。
3. 成员身份由 Java 当前登录会话得到，客户端不能提交 `userId` 作为授权依据。
4. `operator_id` 表示本次实际操作人；系统定时任务使用明确的系统操作来源，不伪装成宠物主人。
5. 权限判断放在 Java application/domain policy 中，Controller 只传输请求，不自行拼装角色判断。
6. 邀请码只作为一次性入场凭证，接受后立即失效；成员后续访问依赖登录会话和 `pet_member`，不依赖邀请链接。
7. 应急信息卡默认在当前登录用户有权限时展示，图片由小程序本地渲染并保存，不新增对象存储文件和永久下载地址。

---

## 3. 现有系统基线与改造影响

### 3.1 已有可复用能力

| 现有能力 | Phase 10 复用方式 |
|---|---|
| `pet.user_id` 和 `PetRepository` | 作为主人归属和成员权限根节点 |
| Sa-Token 登录态 | 获取当前操作人，不从请求体信任 userId |
| `InternalTokenInterceptor` | 保持 Python→Java 内部只读接口的服务间认证 |
| `operator_id` | daily_status、feeding_record、weight_record、event_record、medication_record 已有预留或字段，改为写入真实操作人 |
| Phase 3 reminder / completion | 共享提醒读取和按成员推送的业务基础 |
| Phase 6 medical / health | 应急卡只读取用户已保存的医疗事实，不重新解释诊断原文 |
| Phase 8 expense | 成员默认只读费用，避免家庭成员误删或修改账单 |
| Phase 9 report/content | 成员可按权限查看已生成报告；临时照护者不进入报告和内容创作 |
| Redis | 邀请限流、短期 token 辅助状态、通知去重和功能开关缓存 |
| 现有 `Result` / 错误码 / springdoc | 保持统一响应结构和接口文档风格 |

### 3.2 必须集中改造的风险点

当前多个 application service 仍调用 `petApplicationService.checkPetOwnership(...)`，它只允许 `pet.user_id` 主人访问。Phase 10 不能只在小程序上隐藏按钮，必须把这些调用逐步替换为统一权限服务，否则“成员可查看/记录”的需求会出现接口层和页面层不一致。

改造时必须区分两种身份：

- **资源 owner**：宠物主人，沿用 `pet.user_id`，用于历史数据归属、存储配额、逻辑删除和主人级管理。
- **current operator**：当前登录用户，用于 `operator_id`、审计日志、提醒完成记录和权限判断。

---

## 4. 领域模型与权限模型

### 4.1 核心领域对象

建议新增以下 domain 模块：

```text
com.fafa.domain.model.collaboration
├── PetMember.java
├── PetMemberRole.java          // MEMBER / CAREGIVER
├── PetMemberStatus.java        // ACTIVE / REVOKED / EXPIRED
├── PetInvitation.java
├── InvitationRole.java         // MEMBER / CAREGIVER
├── InvitationStatus.java       // PENDING / ACCEPTED / EXPIRED / REVOKED
├── PetPermission.java          // READ / RECORD / COMPLETE_REMINDER / MANAGE_MEMBER / ...
└── PetAccessDecision.java

com.fafa.domain.model.emergency
├── PetEmergencyProfile.java
└── EmergencyContact.java
```

领域对象必须包含行为，而不是只有 setter：

- `PetMember.activate(...)`：校验成员不能是主人、临时照护必须有有效失效时间。
- `PetMember.revoke(...)`：只能从 active 进入 revoked，记录撤销时间。
- `PetMember.expire(...)`：由访问检查或定时任务将过期 caregiver 标记为 expired。
- `PetInvitation.accept(...)`：校验 pending、未过期、未达到使用次数，并产生成员激活结果。
- `PetInvitation.revoke(...)`：pending 邀请才可以撤销。
- `PetEmergencyProfile.update(...)`：校验字段长度、电话格式、联系方式完整性和版本号。

### 4.2 角色定义

| 角色 | 存储方式 | 说明 |
|---|---|---|
| `OWNER` | 由 `pet.user_id` 派生 | 宠物空间唯一主人，不能被邀请覆盖 |
| `MEMBER` | `pet_member` | 家庭成员，可查看宠物数据并新增日常照护记录 |
| `CAREGIVER` | `pet_member`，必须有 `expires_at` | 临时照护者，只能在有效时间内访问限定照护功能 |
| `SYSTEM` | 不作为登录角色 | 定时任务、通知扫描、数据维护使用，不能被客户端选择 |

### 4.3 权限矩阵

权限名称采用稳定 code，集中维护，Controller 不写字符串判断。

| 资源/动作 | OWNER | MEMBER | CAREGIVER | SYSTEM |
|---|---:|---:|---:|---:|
| 查看宠物基础资料 | ✅ | ✅ | ✅（有效期内） | 受限内部任务 |
| 编辑宠物基础资料/删除宠物 | ✅ | ❌ | ❌ | ❌ |
| 查看每日状态、体重、喂食、事件、媒体 | ✅ | ✅ | ✅ | 受任务范围限制 |
| 新增日常状态、体重、喂食、手工事件、上传媒体 | ✅ | ✅ | ✅ | ❌ |
| 编辑/删除自己创建的手工记录 | ✅ | ✅ | ✅（有效期内） | ❌ |
| 编辑/删除他人记录 | ✅ | ❌ | ❌ | ❌ |
| 完成共享提醒 | ✅ | ✅ | ✅ | ❌ |
| 查看医疗、用药、健康预警 | ✅ | ✅ | ❌ | 规则扫描专用 |
| 新增/编辑/删除医疗、用药 | ✅ | ❌ | ❌ | 规则系统专用 |
| 查看费用统计和账单 | ✅ | ✅ | ❌ | 报告事实读取 |
| 修改/删除费用 | ✅ | ❌ | ❌ | 医疗同步专用 |
| 查看食品卡、喂食计划、报告 | ✅ | ✅ | ❌ | 只读生成/扫描 |
| 查看已生成报告/内容 | ✅ | ✅ | ❌ | 只读生成/扫描 |
| 生成/编辑报告或内容草稿 | ✅ | ❌ | ❌ | 周期任务可生成 |
| 管理成员和邀请 | ✅ | ❌ | ❌ | ❌ |
| 查看成员操作审计 | ✅ | ❌ | ❌ | ❌ |
| 编辑应急信息卡 | ✅ | ❌ | ❌ | ❌ |
| 查看应急信息卡 | ✅ | ✅ | ✅（只读且有效期内） | ❌ |

默认策略优先保证安全：成员可以“看和记”，但不能管理空间、修改敏感医疗事实、删除他人的历史记录；临时照护者只操作当下照护闭环。

### 4.4 统一访问策略

新增 application service：`PetAccessApplicationService`。

建议接口：

```java
PetAccessDecision check(Long operatorId, Long petId, PetPermission permission);
void require(Long operatorId, Long petId, PetPermission permission);
PetAccessDecision currentAccess(Long operatorId, Long petId);
```

实现顺序：

1. 根据 `petId` 查询有效宠物，失败统一返回资源不存在或无权限，不泄漏资源存在性。
2. `operatorId == pet.userId` 时返回 OWNER。
3. 查询 active `pet_member`，如果 `expires_at <= now`，返回过期并异步/同步标记 expired。
4. 使用角色策略判断 permission，拒绝时写审计事件并返回 `FORBIDDEN`。
5. 返回决策对象给 application service，application service 将 `operatorId` 传入领域创建/编辑方法。

禁止：

- 在 Controller 中读取 `pet.user_id` 后手写 if/else。
- 只检查 `pet_member` 而忘记主人。
- 只在列表接口检查权限，详情、写入、删除接口不检查。
- 以邀请 token 作为后续接口的长期授权。

---

## 5. V10 数据库设计

### 5.1 迁移文件

新增：

```text
fafa-java/src/main/resources/db/migration/V10__create_collaboration_and_emergency_tables.sql
```

沿用项目约定：不建立跨业务表物理外键；由 Java application service 校验宠物归属和成员权限；审计表追加写入，业务事实表不因成员撤销而删除。

### 5.2 `pet_member`

```sql
CREATE TABLE IF NOT EXISTS `pet_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '宠物成员ID',
  `pet_id` BIGINT NOT NULL COMMENT '宠物空间ID',
  `user_id` BIGINT NOT NULL COMMENT '协作者用户ID，主人由 pet.user_id 派生',
  `role` VARCHAR(16) NOT NULL COMMENT 'member/caregiver',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT 'active/revoked/expired',
  `granted_by` BIGINT NOT NULL COMMENT '授权人用户ID',
  `joined_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expires_at` DATETIME DEFAULT NULL COMMENT '临时照护到期时间，member 为空',
  `revoked_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_member_user` (`pet_id`, `user_id`),
  KEY `idx_pet_member_active` (`pet_id`, `status`, `expires_at`),
  KEY `idx_pet_member_user` (`user_id`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物空间协作者';
```

设计说明：同一个用户和宠物只保留一行成员关系；撤销后重新邀请使用同一关系恢复或更新，不通过删除重建制造并发竞态。主人不插入此表。

### 5.3 `pet_invitation`

```sql
CREATE TABLE IF NOT EXISTS `pet_invitation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
  `pet_id` BIGINT NOT NULL,
  `created_by` BIGINT NOT NULL COMMENT '主人用户ID',
  `role` VARCHAR(16) NOT NULL COMMENT 'member/caregiver',
  `token_hash` CHAR(64) NOT NULL COMMENT '邀请 token SHA-256 摘要，不保存明文',
  `expires_at` DATETIME NOT NULL,
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物空间邀请凭证';
```

安全约束：token 使用密码学安全随机数生成，明文仅在创建响应返回一次；接受时使用原子条件更新，避免两个用户并发接受同一邀请。

### 5.4 `pet_emergency_profile`

```sql
CREATE TABLE IF NOT EXISTS `pet_emergency_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '应急资料ID',
  `pet_id` BIGINT NOT NULL,
  `blood_type` VARCHAR(32) DEFAULT NULL COMMENT '血型，未知时为空',
  `allergies` VARCHAR(1000) DEFAULT NULL COMMENT '用户确认的过敏史',
  `chronic_conditions` VARCHAR(1000) DEFAULT NULL COMMENT '用户确认的慢性病记录',
  `emergency_contact_name` VARCHAR(64) DEFAULT NULL,
  `emergency_contact_phone` VARCHAR(32) DEFAULT NULL,
  `primary_hospital_name` VARCHAR(200) DEFAULT NULL,
  `primary_hospital_phone` VARCHAR(32) DEFAULT NULL,
  `primary_hospital_address` VARCHAR(500) DEFAULT NULL,
  `notes` VARCHAR(1000) DEFAULT NULL,
  `version` INT NOT NULL DEFAULT 0,
  `updated_by` BIGINT NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pet_emergency_profile_pet` (`pet_id`),
  KEY `idx_pet_emergency_updated` (`pet_id`, `updated_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物应急资料';
```

应急资料不自动从医疗文本推断。若字段没有用户保存值，接口返回空值和“未填写”状态，不在卡片上补充“正常”或模型猜测。

实现补充：caregiver 邀请会在 `pet_invitation.member_expires_at` 保存接受后的成员到期时间；共享提醒投递表增加 `retry_count`，失败投递按配置有限重试。

### 5.5 `access_audit_log`

```sql
CREATE TABLE IF NOT EXISTS `access_audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '审计日志ID',
  `request_id` VARCHAR(64) DEFAULT NULL,
  `pet_id` BIGINT DEFAULT NULL,
  `user_id` BIGINT DEFAULT NULL COMMENT '当前操作人',
  `operator_id` BIGINT DEFAULT NULL COMMENT '实际执行人，系统任务可为空',
  `operation` VARCHAR(64) NOT NULL COMMENT 'read/create/update/delete/invite/accept/revoke/complete',
  `resource_type` VARCHAR(64) NOT NULL,
  `resource_id` VARCHAR(64) DEFAULT NULL,
  `result` VARCHAR(16) NOT NULL COMMENT 'success/denied/failed',
  `reason` VARCHAR(128) DEFAULT NULL,
  `metadata_json` JSON DEFAULT NULL COMMENT '不保存 token、诊断原文和完整请求体',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_access_audit_pet_time` (`pet_id`, `created_at`),
  KEY `idx_access_audit_operator_time` (`operator_id`, `created_at`),
  KEY `idx_access_audit_request` (`request_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='宠物空间访问与操作审计';
```

审计表追加写入，不提供普通成员查询；主人只能查询自己宠物空间的脱敏审计记录。

### 5.6 `reminder_notification_delivery`

```sql
CREATE TABLE IF NOT EXISTS `reminder_notification_delivery` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `reminder_id` BIGINT NOT NULL,
  `pet_id` BIGINT NOT NULL,
  `recipient_user_id` BIGINT NOT NULL,
  `delivery_window` VARCHAR(32) NOT NULL COMMENT '同一提醒的逻辑推送窗口',
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT 'pending/sent/failed/skipped',
  `provider_message_id` VARCHAR(128) DEFAULT NULL,
  `error_message` VARCHAR(500) DEFAULT NULL,
  `pushed_at` DATETIME DEFAULT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_reminder_delivery_recipient_window` (`reminder_id`, `recipient_user_id`, `delivery_window`),
  KEY `idx_reminder_delivery_scan` (`status`, `created_at`),
  KEY `idx_reminder_delivery_pet` (`pet_id`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='共享提醒成员推送幂等记录';
```

当前 `reminder.last_push_time` 只能表达“宠物提醒是否推过”，Phase 10 用此表表达“每个成员是否推过”，两者并存一阶段，待全部迁移完成后再评估是否删除旧字段。

### 5.7 操作人字段补齐

不修改历史 `user_id` owner 语义，按以下矩阵补齐或复用：

| 表 | 操作人字段处理 | 说明 |
|---|---|---|
| daily_status / feeding_record / weight_record / event_record | 复用已有 `operator_id` | 创建/更新时写当前登录用户 |
| medication_record | 复用已有 `operator_id` | 提醒完成时写实际完成用户 |
| reminder_completion_history | 复用 `user_id` 作为完成人 | 现有字段语义已经是完成人 |
| medical_visit / medication_plan | V10 增加 `operator_id` | `user_id` 继续作为 owner/档案归属 |
| media_asset | V10 增加 `operator_id` | `user_id` 继续作为上传配额和资源 owner |
| food_card / food_transition_plan / feeding_plan | V10 增加 `operator_id` | AI 草稿确认时写确认人 |
| expense_record | V10 增加 `operator_id` | 医疗自动同步使用系统来源，手工账单写当前用户 |
| pet_report / content_draft | V10 增加 `generated_by` 或 `operator_id` | 区分手动生成、成员生成、系统周期生成 |

V10 迁移对已有行使用 `NULL`，历史数据不伪造操作人；读取接口显示“历史记录，操作人未记录”。

---

## 6. 邀请、接受和撤销流程

### 6.1 主人创建邀请

1. 当前登录用户访问宠物邀请接口。
2. `PetAccessApplicationService.require(userId, petId, MANAGE_MEMBER)` 校验主人权限。
3. 校验角色、有效期和当前 active 成员数量；普通用户和会员上限统一走配置化 quota policy。
4. 生成至少 128 bit 随机 token，数据库只保存 SHA-256 摘要。
5. 写入 pending 邀请和成功审计日志。
6. 返回 `invitationId`、原始 token、过期时间和小程序跳转参数；之后不再返回原始 token。

邀请默认配置进入 `fafa.collaboration`：

```yaml
fafa:
  collaboration:
    enabled: false
    normal-max-members: 0
    vip-max-members: 5
    caregiver-max-days: 30
    invitation-expire-minutes: 30
    invite-rate-limit-per-hour: 20
    accept-rate-limit-per-hour: 30
    audit-retention-days: 730
```

默认关闭 `enabled`，完成灰度准备后才打开；普通用户权益按产品设计默认不支持，服务端必须拒绝而不是只在 UI 隐藏。

### 6.2 成员接受邀请

1. 用户登录后打开邀请链接，客户端提交 `invitationId + token`。
2. Java 对 token 做摘要，与数据库摘要比较；失败统一返回邀请无效，不区分“已使用/不存在/他人已接受”。
3. 在事务中锁定邀请记录和当前用户的成员关系，校验后执行原子更新：

```sql
UPDATE pet_invitation
SET used_count = used_count + 1,
    status = 'accepted',
    accepted_by = ?,
    accepted_at = CURRENT_TIMESTAMP
WHERE id = ?
  AND token_hash = ?
  AND status = 'pending'
  AND expires_at > CURRENT_TIMESTAMP
  AND used_count < max_uses;
```

上面的更新必须放在“成员关系创建或恢复成功”之后，并与成员写入处于同一个事务；执行前先完成主人自接受、已有 active 关系和角色/到期时间校验。若项目采用先占用邀请再写成员的实现，必须提供同事务回滚，不能留下已 accepted 但没有成员关系的邀请。

4. 若当前用户是主人，拒绝接受自己的邀请；若当前用户已有 active 成员关系，使用稳定的“邀请无效/已处理”响应，不再次消费邀请。
5. 若成员关系已 revoked，只有主人重新发起邀请后才允许恢复；接受新邀请时在同一事务内恢复或更新关系。
6. 只有成员关系写入成功后，才将邀请标记为 accepted 并写成功审计日志；任一步失败都回滚，避免 `used_count` 与成员状态不一致。

### 6.3 主人撤销成员

- 只能由 owner 执行。
- 将成员状态改为 `revoked`，写 `revoked_at`，不删除历史记录和审计日志。
- 同时使该成员未完成的 pending 邀请失效。
- 已经发出的微信通知不追溯撤回；后续读取和新通知立即拒绝。
- 若成员正在提交写操作，业务事务必须在保存前再次校验 access decision，避免“撤销后仍写入”。

### 6.4 临时照护到期

- `CAREGIVER` 必须设置 `expires_at`，服务端限制最长天数。
- 每次访问都判断 `expires_at`，不能只依赖定时任务更新状态。
- 定时任务批量把过期成员从 active 改为 expired，写系统审计事件。
- 过期后邀请记录和历史操作仍可被主人查看，但不再提供数据访问。

---

## 7. Java 分层实施设计

### 7.1 Domain 层

```text
fafa-java/src/main/java/com/fafa/domain/
├── model/collaboration/
│   ├── PetMember.java
│   ├── PetMemberRole.java
│   ├── PetMemberStatus.java
│   ├── PetInvitation.java
│   ├── InvitationRole.java
│   ├── InvitationStatus.java
│   ├── PetPermission.java
│   └── PetAccessDecision.java
├── model/emergency/
│   ├── PetEmergencyProfile.java
│   └── EmergencyContact.java
└── repository/
    ├── PetMemberRepository.java
    ├── PetInvitationRepository.java
    ├── PetEmergencyProfileRepository.java
    ├── AccessAuditLogRepository.java
    └── ReminderNotificationDeliveryRepository.java
```

Domain 层不依赖 Spring、MyBatis、Sa-Token 或 HTTP。`PetAccessDecision` 是值对象，包含 `petId`、`operatorId`、role、有效期和 permission 判断结果。

### 7.2 Application 层

```text
fafa-java/src/main/java/com/fafa/application/
├── service/PetAccessApplicationService.java
├── service/PetMemberApplicationService.java
├── service/PetInvitationApplicationService.java
├── service/EmergencyCardApplicationService.java
├── service/AccessAuditApplicationService.java
├── service/SharedReminderApplicationService.java
└── dto/
    ├── collaboration/
    └── emergency/
```

职责边界：

- `PetAccessApplicationService`：统一权限决策和越权错误。
- `PetInvitationApplicationService`：创建、接受、撤销邀请，处理幂等和事务。
- `PetMemberApplicationService`：成员列表、角色/期限展示、撤销和上限校验。
- `EmergencyCardApplicationService`：维护应急资料，聚合宠物和用户确认的医疗事实，输出卡片 DTO。
- `SharedReminderApplicationService`：根据成员权限生成接收人集合，创建推送幂等记录。
- `AccessAuditApplicationService`：记录授权、拒绝、成员变更和敏感数据访问，不把完整请求体写入日志。

### 7.3 Infrastructure 层

```text
fafa-java/src/main/java/com/fafa/infrastructure/
├── persistence/po/
│   ├── PetMemberPO.java
│   ├── PetInvitationPO.java
│   ├── PetEmergencyProfilePO.java
│   ├── AccessAuditLogPO.java
│   └── ReminderNotificationDeliveryPO.java
├── persistence/mapper/
├── persistence/repository/
├── scheduler/
│   ├── PetInvitationExpiryScheduler.java
│   ├── SharedReminderNotificationScheduler.java
│   └── CollaborationMaintenanceScheduler.java
└── security/
    ├── InvitationTokenService.java
    └── RateLimitService.java
```

token 摘要、限流和通知去重可以复用现有 Redis 客户端，但不得把 Redis key 拼接逻辑散落在 Controller 或领域对象中。

### 7.4 Interfaces 层

```text
fafa-java/src/main/java/com/fafa/interfaces/
├── controller/PetMemberController.java
├── controller/PetInvitationController.java
├── controller/EmergencyCardController.java
├── controller/AccessAuditController.java
└── dto/
    ├── collaboration/
    └── emergency/
```

Controller 只做请求绑定、基础格式校验、调用 application service 和统一返回，不接收 `userId` 作为授权参数。

---

## 8. 公共接口设计

所有接口默认地址为 `http://localhost:8080`，使用登录后的 `Authorization`。接口返回项目统一 `Result { code, message, data }`。

### 8.1 查询当前用户对宠物的访问权限

- 接口定义：返回当前用户的 role、有效期和页面能力，用于小程序渲染。
- 地址：`GET /api/pets/{petId}/access`
- 响应：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "petId": 1,
    "role": "member",
    "status": "active",
    "expiresAt": null,
    "permissions": ["READ_PROFILE", "READ_RECORD", "CREATE_RECORD", "COMPLETE_REMINDER"],
    "serverTime": "2026-08-23T15:00:00"
  }
}
```

### 8.2 主人创建邀请

- 接口定义：为宠物空间创建一次性邀请。
- 地址：`POST /api/pets/{petId}/invitations`
- 请求 JSON：

```json
{
  "role": "caregiver",
  "expiresInDays": 7
}
```

- 响应 JSON：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "invitationId": 1201,
    "petId": 1,
    "role": "caregiver",
    "token": "one-time-token-returned-only-once",
    "expiresAt": "2026-08-30T15:00:00",
    "sharePath": "/pages/invitation/accept?invitationId=1201&token=one-time-token-returned-only-once"
  }
}
```

### 8.3 接受邀请

- 接口定义：当前登录用户使用一次性 token 加入宠物空间。
- 地址：`POST /api/pet-invitations/accept`
- 请求 JSON：

```json
{
  "invitationId": 1201,
  "token": "one-time-token-returned-only-once"
}
```

- 响应 JSON：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "petId": 1,
    "role": "caregiver",
    "status": "active",
    "expiresAt": "2026-08-30T15:00:00"
  }
}
```

### 8.4 成员列表与撤销

- 成员列表：`GET /api/pets/{petId}/members`
- 撤销成员：`DELETE /api/pets/{petId}/members/{memberId}`
- 撤销邀请：`DELETE /api/pets/{petId}/invitations/{invitationId}`
- 列表响应 JSON：

```json
{
  "code": 0,
  "message": "成功",
  "data": [
    {"memberId": 501, "userId": 8, "nickname": "家人", "role": "member", "status": "active", "expiresAt": null},
    {"memberId": 502, "userId": 9, "nickname": "临时照护", "role": "caregiver", "status": "active", "expiresAt": "2026-08-30T15:00:00"}
  ]
}
```

### 8.5 应急资料维护与卡片读取

- 查询资料：`GET /api/pets/{petId}/emergency-profile`
- 更新资料：`PUT /api/pets/{petId}/emergency-profile`
- 读取应急卡：`GET /api/pets/{petId}/emergency-card`
- 更新请求 JSON：

```json
{
  "bloodType": "未知",
  "allergies": "对某类药物过敏，以病历为准",
  "chronicConditions": "无已确认记录",
  "emergencyContactName": "张三",
  "emergencyContactPhone": "13800000000",
  "primaryHospitalName": "XX 宠物医院",
  "primaryHospitalPhone": "010-12345678",
  "primaryHospitalAddress": "XX 区 XX 路",
  "notes": "紧急情况先联系主人"
}
```

- 应急卡响应 JSON：

```json
{
  "code": 0,
  "message": "成功",
  "data": {
    "pet": {"id": 1, "name": "豆包", "type": "cat", "breed": "英短"},
    "profile": {"bloodType": "未知", "allergies": "对某类药物过敏，以病历为准", "chronicConditions": "无已确认记录"},
    "emergencyContact": {"name": "张三", "phone": "13800000000"},
    "primaryHospital": {"name": "XX 宠物医院", "phone": "010-12345678", "address": "XX 区 XX 路"},
    "sourceStatus": "user_confirmed",
    "updatedAt": "2026-08-23T15:00:00"
  }
}
```

小程序使用该 JSON 在本地卡片页面渲染并调用系统保存图片能力；后端不生成公网图片地址。

### 8.6 审计记录

- 地址：`GET /api/pets/{petId}/audit-logs?page=1&size=20`
- 仅 owner 可访问。
- 返回字段只包含操作人昵称、操作类型、资源类型、结果、时间和脱敏原因，不返回 token、诊断原文、完整备注和内部请求头。

### 8.7 内部接口调整

现有 `/internal/ai/**` 继续由 `X-Internal-Token` 保护。Python Agent 调用 Java 查询宠物数据时，Java 使用 `userId` 对应的 `PetAccessApplicationService` 判断 `READ_*` 权限；Python 不获得成员列表写权限，不新增任何成员管理工具。

---

## 9. Phase 2～9 跨模块授权改造清单

### 9.1 记录与成长档案

- `DailyRecordApplicationService`：读取使用 `READ_RECORD`，新增/编辑使用 `CREATE_RECORD` 或 `EDIT_OWN_RECORD`。
- `WeightRecordApplicationService`：写入当前 `operator_id`；成员只能编辑自己的记录。
- `FeedingRecordApplicationService`：喂食记录和计划完成允许 caregiver；删除他人记录只允许 owner。
- `EventRecordApplicationService`：手工事件使用真实操作人；提醒生成事件不可被成员修改。
- `ReminderApplicationService`：查看和完成提醒按成员权限，提醒定义编辑和删除仅 owner。

### 9.2 素材、食品和喂养

- `MediaApplicationService`：上传者写 `operator_id`，存储配额仍按 owner 计量；caregiver 不可删除或改变宠物归属。
- `FoodCardApplicationService`：成员只读；食品卡确认、归档、删除仅 owner。
- `FoodRecognitionApplicationService`：识别请求要检查 `CREATE_RECORD` 或明确食品权限，识别草稿确认仅 owner/member，caregiver 禁止。
- `FeedingPlanApplicationService` / `FoodTransitionApplicationService`：查看可共享，计划创建和修改仅 owner/member。

### 9.3 医疗、费用和报告

- `MedicalVisitApplicationService`、`MedicationApplicationService`：敏感医疗写操作仅 owner；成员读取需审计，caregiver 禁止。
- `HealthAlertApplicationService`：成员可查看中性预警，caregiver 不可查看健康预警详情。
- `ExpenseApplicationService`：成员可以查看费用统计和账单，只有 owner 可修改/删除；系统医疗同步记录使用系统操作来源。
- `ReportApplicationService`：owner/member 可读取已生成报告；caregiver 不可读取报告，避免暴露医疗/费用汇总。
- `ContentDraftApplicationService`：owner 可生成和编辑，member 只能查看已生成内容，caregiver 不可读取或使用内容创作配额。

### 9.4 统一重构策略

分批替换服务中的 owner-only 检查，避免一次改动覆盖过大：

1. 先改只读路径，确认成员能访问且跨宠物仍拒绝。
2. 再改日常记录写入路径，补齐 operator 字段和“编辑自己的记录”规则。
3. 再改医疗、费用、食品、报告等敏感模块，默认收紧权限。
4. 最后改 Agent 内部读取、定时任务和回调路径，明确 user/operator/system 三种来源。

每批改造都必须有跨角色测试，禁止用全局替换简单替换 `checkPetOwnership`。

---

## 10. 共享提醒与通知设计

### 10.1 接收人计算

提醒扫描时：

1. 查询 reminder 所属宠物和 owner。
2. 查询 active 成员，过滤 `expires_at <= now` 的 caregiver。
3. 根据提醒类型和角色权限生成 recipient 列表。
4. 为每个 recipient 创建 `reminder_notification_delivery` 幂等记录。
5. 只把 pending 记录交给现有微信订阅消息发送适配器。

### 10.2 幂等和失败策略

- `delivery_window` 使用提醒业务窗口，例如 `2026-08-24-morning`，不使用随机 UUID。
- 同一 `reminder_id + recipient_user_id + delivery_window` 只允许一条记录。
- 发送成功更新 `sent` 和 provider message id。
- 发送失败更新 `failed`，记录安全错误摘要；下一轮只重试符合重试次数和时间窗口的记录。
- 一个成员的订阅消息失败不阻断其他成员，也不阻断首页提醒数据展示。
- 撤销或过期成员不会产生新通知，但历史 sent 记录保留。

### 10.3 可观测指标

至少记录：

- `fafa_reminder_delivery_pending_total`
- `fafa_reminder_delivery_sent_total`
- `fafa_reminder_delivery_failed_total`
- `fafa_reminder_delivery_duplicate_total`
- `fafa_collaboration_member_expired_total`

日志只记录 petId、recipientUserId、deliveryWindow、结果和 traceId，不记录微信 token 或完整模板数据。

---

## 11. 应急信息卡设计

### 11.1 事实来源

应急卡只组合以下已保存数据：

- 宠物档案：name、type、breed、gender、birthDate、当前体重。
- 用户确认的 `pet_emergency_profile`：血型、过敏史、慢性病、紧急联系人、常去医院、备注。
- 可选的最近医疗资料摘要：只显示就诊日期、医院和用户保存的简短信息，不自动抽取诊断结论。

### 11.2 生成和编辑规则

- 只有 owner 可以编辑应急资料。
- 保存使用乐观锁 `version`，防止两个页面覆盖更新。
- 空字段明确返回 `null` 或“未填写”，不得由 AI 或常识补全。
- 电话、文本长度、敏感内容和 HTML 注入字符在 Java 校验。
- 成员和 caregiver 只能读取，不可通过应急卡接口修改。
- 小程序保存图片前显示“资料由用户维护，请在使用前确认有效”的提示。

### 11.3 小程序页面

新增：

```text
pages/emergency-card/index       // 卡片预览、保存图片、拨打电话
pages/emergency-card/edit        // owner 编辑资料
pages/collaboration/index        // 成员、邀请、撤销、权限说明
pages/invitation/accept          // 登录后接受一次性邀请
```

页面只根据 `/access` 返回的 permissions 控制展示，同时所有接口继续由服务端鉴权。

---

## 12. 上线打磨与生产治理

### 12.1 配置与功能开关

新增 `FaFaProperties.Collaboration`：

```java
private boolean enabled;
private int normalMaxMembers;
private int vipMaxMembers;
private int caregiverMaxDays;
private int invitationExpireMinutes;
private int inviteRateLimitPerHour;
private int acceptRateLimitPerHour;
private int auditRetentionDays;
private String reminderDeliveryCron;
```

环境配置要求：

- dev：可以开启，使用短过期时间和测试微信模板。
- test：默认开启，固定测试用户和可回收邀请。
- prod：先关闭，灰度用户或白名单开启；迁移和监控确认后再逐步放量。
- 所有 token、微信 secret、数据库密码、对象存储密钥继续通过环境变量注入。

### 12.2 安全加固

- 邀请 token 使用安全随机数、摘要存储、单次消费和最短必要有效期。
- 创建邀请和接受邀请按用户/IP/设备维度限流，限流 key 进入 Redis 统一服务。
- 所有成员 API 进行 petId + permission 双校验。
- 失败响应避免区分“资源不存在”和“无权访问”的内部细节。
- 审计日志禁止记录 raw token、Authorization、X-Internal-Token、完整医疗文本和完整用户备注。
- 访问应急卡、医疗记录、费用统计和成员审计都记录敏感访问审计。
- Python Agent 只读，不能通过自然语言触发邀请、授权、撤销、应急资料修改或成员写入。
- 对 API body 做长度限制，防止邀请备注、审计查询和应急文本导致异常内存占用。

### 12.3 可观测性

保持现有 `traceId` 贯穿：

- 登录用户请求 → 成员权限决策 → 业务写入 → 审计日志。
- 定时任务 → 通知记录 → 微信适配器。
- 内部 Agent 查询 → Java 权限判断 → 返回结果。

新增结构化日志字段：`petId`、`operatorId`、`role`、`permission`、`decision`、`requestId`。不得输出原始邀请 token。

新增健康检查与指标：

- V10 表是否可读写。
- Redis 限流/幂等 key 是否可用。
- 微信订阅消息适配器失败率。
- 当前 active 成员数量、过期成员数量、待发送通知数量。
- 权限拒绝率和邀请接受成功率。

### 12.4 数据库迁移与回滚

采用 expand-contract：

1. 先执行 V10 建表和可空操作人字段，不改变旧接口行为。
2. 发布兼容版本，读路径优先兼容 owner-only 和成员新路径。
3. 完成数据回填/索引确认后开启协同功能开关。
4. 验收后再逐步收紧新权限，旧 owner 行为必须保持可用。
5. 回滚应用版本时保留 V10 表，不删除成员、邀请和审计数据；功能开关关闭即可止损。

禁止对生产数据库执行 `DROP TABLE`、大范围 `DELETE` 或不可逆字段重命名作为回滚手段。

### 12.5 备份和保留

- 发布前备份数据库并验证可恢复性。
- 审计日志按配置保留，过期数据通过批处理归档/删除，不阻塞主请求。
- 成员撤销不删除业务事实。
- 邀请过期记录可按保留策略清理，但不能影响成员操作审计。
- 应急资料与宠物档案同级保护，不生成额外公开副本。

---

## 13. 实施分阶段计划

### Phase 10.0：权限与上线边界冻结

交付：角色矩阵、资源 owner/operator 规则、邀请 TTL、成员上限、应急卡字段、功能开关、错误码和审计字段清单。

检查：产品确认成员是否能看医疗/费用、caregiver 是否能查看报告、普通用户协同权益是否仍关闭。

### Phase 10.1：领域模型、V10 迁移与仓储

交付：五张 V10 表、PO、Mapper、Repository、领域枚举、状态行为和迁移测试。

重点：token 摘要、成员唯一关系、应急资料乐观锁、通知 recipient 幂等键、审计追加写。

### Phase 10.2：统一访问策略和邀请闭环

交付：`PetAccessApplicationService`、成员列表、创建邀请、接受邀请、撤销成员、撤销邀请、成员上限和限流。

重点：并发接受同一邀请、邀请过期、重复接受、跨宠物 token、主人自接受和撤销竞态。

### Phase 10.3：Phase 2～9 写入链路操作人改造

交付：记录、提醒、素材、食品、医疗、费用、报告和内容接口接入统一访问策略；`operator_id` 写入真实当前用户。

重点：owner 旧流程回归、member 新流程、caregiver 限制、跨用户和跨宠物越权。

### Phase 10.4：共享提醒和审计

交付：成员接收人计算、通知幂等表、重试、过期成员清理、主人审计查询。

重点：重复扫描不重复推送、一个成员失败不影响其他成员、撤销后不再产生新通知。

### Phase 10.5：应急信息卡和小程序页面

交付：应急资料 CRUD、应急卡 JSON、owner 编辑、成员只读、小程序预览/保存图片、邀请接受页和成员管理页。

重点：空数据、版本冲突、电话拨打、权限隐藏与服务端拒绝一致。

### Phase 10.6：上线打磨

交付：配置分环境、功能开关、限流、指标、日志脱敏、健康检查、备份恢复演练、发布和回滚脚本/清单。

重点：灰度关闭功能后旧 Phase 0～9 流程仍可用；关闭后邀请和成员 API 不产生新数据写入。

### Phase 10.7：联调、灰度与阶段验收

交付：Java 全量测试、Python 回归、小程序检查、真实 MySQL/Redis/微信通知联调、灰度报告和最终交接文档。

重点：用测试账号验证 owner/member/caregiver/system 四种身份，完成至少一次回滚演练。

当前实际状态：Java 核心代码、V10 迁移、接口文档、MySQL 表文档、交接文档和小程序最小页面已经落地；真实 MySQL/Flyway、Redis 限流、微信订阅消息和四角色越权矩阵尚未在本轮执行，因此阶段状态保持“待联调”。

---

## 14. 测试与验收设计

### 14.1 Domain 单元测试

- `PetMemberRole`、`PetPermission` code 映射和未知值拒绝。
- owner 派生权限不依赖 `pet_member` 行。
- caregiver 缺少 `expires_at`、超过最大天数、已过期激活均拒绝。
- member revoke/expire 状态机不可逆回 active，除非通过新的邀请流程。
- invitation pending/accepted/expired/revoked 状态流转。
- 应急资料字段长度、电话格式、version 冲突。

### 14.2 Repository/迁移测试

- V10 在已有 V9 数据库成功执行。
- `pet_member(pet_id,user_id)` 唯一键生效。
- token_hash 唯一键和查询索引生效。
- 应急资料 upsert 不产生第二行。
- notification delivery 相同窗口重复插入幂等。
- audit log 追加写不被业务逻辑删除。

### 14.3 权限矩阵测试

至少覆盖以下组合：

| 场景 | owner | member | caregiver | 其他用户 |
|---|---:|---:|---:|---:|
| 查看宠物资料 | 200 | 200 | 200 | 404/403 |
| 创建喂食记录 | 200 | 200 | 200 | 404/403 |
| 修改医疗档案 | 200 | 403 | 403 | 404/403 |
| 删除费用 | 200 | 403 | 403 | 404/403 |
| 管理成员 | 200 | 403 | 403 | 404/403 |
| 查看审计 | 200 | 403 | 403 | 404/403 |
| 编辑应急资料 | 200 | 403 | 403 | 404/403 |
| 查看应急卡 | 200 | 200 | 200 | 404/403 |
| caregiver 到期后任意接口 | 403 | - | 403 | - |

### 14.4 并发与安全测试

- 100 个并发请求接受同一个一次性邀请，只能成功一个。
- 同时撤销成员和提交喂食记录，撤销完成后不能产生新写入。
- 伪造 `userId`、`role`、`operatorId`、`petId` 均不能越权。
- 枚举 invitationId 不能得到 token、宠物名或成员信息。
- 邀请接口达到限流阈值后返回稳定错误码，不写入半成品邀请。
- 审计日志和应用日志扫描 token、Authorization、内部令牌和医疗原文泄漏。

### 14.5 共享提醒测试

- owner/member/caregiver 按权限收到通知。
- caregiver 到期不再收到通知。
- 同一 delivery window 重复扫描只有一次 sent。
- 微信适配器单个收件人失败不影响其他人。
- retry 次数和失败状态可观察、可恢复。

### 14.6 应急卡和小程序测试

- 无 profile 时显示未填写，不显示模型猜测。
- owner 编辑后 member/caregiver 立即可读到新版本。
- 两个编辑页面同时保存，旧 version 返回冲突并要求刷新。
- 资料存在特殊字符、超长文本、换行和手机号异常时前后端均拒绝或规范化。
- 小程序离线/网络失败时不把未保存内容误显示为已保存。
- 保存图片前确认免责声明，图片不包含 token、userId 或隐藏敏感字段。

### 14.7 性能目标

- 权限查询 P95 ≤ 100ms（Redis/数据库正常）。
- 成员列表 P95 ≤ 200ms，默认分页上限 50。
- 审计列表 P95 ≤ 300ms，默认分页上限 50。
- 通知扫描单批不超过配置 batch size，单次失败可重试。
- 迁移在测试数据量下无全表长事务；生产先评估索引和锁。

---

## 15. 错误码与配置契约

建议新增 `72xxx` 协同与应急模块错误码：

| 错误码 | 含义 |
|---:|---|
| 72001 | 宠物空间协同功能未开放 |
| 72002 | 成员数量已达上限 |
| 72003 | 成员不存在 |
| 72004 | 无权管理成员 |
| 72005 | 邀请不存在或已失效 |
| 72006 | 邀请已被使用 |
| 72007 | 邀请 token 无效 |
| 72008 | 邀请频率超限 |
| 72009 | 成员权限不足 |
| 72010 | 临时照护权限已过期 |
| 72011 | 应急资料不存在 |
| 72012 | 应急资料版本冲突 |
| 72013 | 应急资料字段无效 |
| 72014 | 审计查询无权限 |
| 72015 | 共享提醒推送失败 |

错误信息不要暴露“邀请 token 已存在”“目标用户是否属于某宠物”等枚举信息；前端只根据稳定错误码展示可操作提示。

---

## 16. 发布顺序、灰度和回滚

### 16.1 发布前

1. 冻结 API、角色矩阵、V10 SQL 和配置默认值。
2. 备份生产数据库并验证恢复点。
3. 在预发布环境执行 V10，运行迁移检查和历史数据回归。
4. 安装新版本但保持 `fafa.collaboration.enabled=false`。
5. 验证 Phase 0～9 的 owner 流程、Python 内部接口、报告、内容和媒体链路。
6. 检查监控、日志脱敏、限流和通知失败告警。

### 16.2 灰度

1. 仅对白名单用户打开协同开关。
2. 观察 24 小时：权限拒绝率、邀请成功率、重复通知率、接口延迟和异常日志。
3. 逐步放大会员用户范围，再决定是否开放其他权益。
4. 应急信息卡可独立开关，避免协同问题阻断应急卡使用。

### 16.3 回滚

- 轻微问题：关闭 feature flag，保留数据和审计。
- 权限问题：立即关闭协同和应急卡写入口，只保留 owner 旧流程，导出拒绝审计。
- 通知问题：关闭共享推送 scheduler，保留首页提醒和待发送记录。
- 数据迁移问题：停止发布，不删除 V10 表；根据备份恢复策略处理，禁止手工删除成员关系。
- 回滚后必须重新执行 owner/member/caregiver 最小回归集。

---

## 17. 文档同步清单

Phase 10 实施时必须同步：

1. `docs/接口文档.md`：增加成员、邀请、接受、撤销、权限、应急卡和审计接口。
2. `docs/MySQL数据表记录.md`：增加 `pet_member`、`pet_invitation`、`pet_emergency_profile`、`access_audit_log`、`reminder_notification_delivery`。
3. `docs/Codex开发交接文档.md`：记录 owner/operator 规则、权限矩阵、token 安全、灰度和回滚。
4. `docs/分阶段开发计划.md`：将 Phase 10 更新为“核心代码完成，待联调”或“已完成”。
5. `docs/dev/phase10-implementation-plan.md`：实施后更新状态、实际配置、测试结果和未完成项。
6. `application-dev.yml`、`application-test.yml`、`application-prod.yml`：同步协同开关、成员上限、邀请 TTL、限流、通知扫描和审计保留配置。
7. Knife4j/springdoc 分组：增加 collaboration、emergency、audit 接口分组。

---

## 18. 开发规范自检

### Architecture Delineation

成员、邀请和应急资料作为独立领域边界，Repository 接口位于 domain，MyBatis 和 Redis 适配位于 infrastructure，权限编排和事务位于 application，Controller 只负责传输。保留 `pet.user_id` 作为 owner，使用统一 `PetAccessApplicationService` 和 `operator_id` 解决协作者访问，避免在每个模块复制角色判断或引入通用工作流框架。

### Hard-coding

- 角色、状态、权限和操作类型使用枚举/code。
- 邀请有效期、成员上限、照护最长天数、限流、审计保留、通知扫描和 feature flag 进入 `FaFaProperties`。
- Redis key、审计字段白名单、分页上限和通知 retry 由配置/常量集中维护。
- 小程序不复制权限业务规则，只消费服务端 permissions。

### Coupling / Cohesion

- `PetAccessApplicationService` 只负责权限决策；邀请服务只负责凭证和成员生命周期；提醒服务只负责接收人和投递幂等；应急服务只负责用户确认资料和卡片输出。
- Python 保持只读，不获得成员管理写权限；通知适配器不直接修改成员关系。
- V10 迁移只新增表和可空字段，旧 owner 流程保持兼容，便于灰度和回滚。

### Type Safety

- Java 使用 `PetMemberRole`、`PetPermission`、`InvitationStatus`、`LocalDateTime` 和 version 字段。
- API DTO 对 role、TTL、文本长度、手机号、分页和 token 做校验。
- Python 不接收新写入契约；内部查询继续使用既有 Pydantic/JSON 契约。
- 应急卡使用明确 DTO，区分用户确认字段和系统生成时间，不使用 `Map<String,Object>` 承担核心权限数据。

---

## 19. 实施后自测命令

```text
1. 检查 git status，确认不覆盖 Phase 0～9 未提交改动。
2. 执行 V10 Flyway，并核对五张新增表、索引和可空 operator_id。
3. 运行 Java compile/test，重点检查权限策略、邀请并发和状态机。
4. 运行 Python 全量回归和静态检查，确认 Agent 没有新增写入工具。
5. 运行小程序页面 JavaScript、JSON 和接口契约检查。
6. 使用 owner/member/caregiver/其他用户四套账号进行越权矩阵测试。
7. 并发接受同一邀请，确认只能成功一次。
8. 撤销成员并立即提交喂食/提醒完成，确认不会产生越权写入。
9. 触发共享提醒多次，确认同一接收人同一窗口只有一次推送。
10. 检查日志、审计和监控中没有 raw token、Authorization、内部 token 和医疗原文泄漏。
11. 在灰度关闭 feature flag，确认 Phase 0～9 owner 流程仍可用。
12. 完成数据库恢复和应用回滚演练，记录实际耗时和负责人。
```

Phase 10 的最终交付标准不是“能邀请一个人”，而是建立一套可验证的私密协作边界：谁拥有宠物、谁可以看、谁可以记、谁可以修改、权限什么时候失效、每次操作由谁完成，都必须由服务端明确判断并留下可审计证据。

## 20. 本轮实施结果

- 已落地：统一访问策略、member/caregiver 生命周期、一次性邀请摘要、成员限流、审计追加写、应急资料 version 乐观锁、应急卡 JSON、小程序协同与应急卡页面。
- 已落地：日常状态、喂食、体重、事件、提醒读取/完成、媒体读取/上传、医疗读取、健康预警读取接入权限；操作人字段已在主要日常记录和媒体上传链路写入。
- 已落地：共享提醒按接收人和业务窗口幂等，失败状态按 `fafa.collaboration.reminder-max-retries` 有限重试；真实微信发送仍复用现有日志适配点，待订阅消息凭证接入后联调。
- 校验结果：`mvn test` 通过，140 个测试全部通过；新增协同、应急资料、提醒投递领域测试 7 个。
- 未完成：真实数据库迁移、Redis 故障/限流联调、微信通知适配器、并发压测、Python 与小程序运行时回归、回滚演练。
