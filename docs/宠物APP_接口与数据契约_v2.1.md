# 伴宠 PawLife · 接口与数据契约 v2.1

> 2026-10-25｜配套[页面与交互规格](宠物APP_页面与交互设计规格_v2.1.md)。  
> 以下为**供UI与研发对齐的拟定契约**，不是已有服务说明，也不是可直接部署的OpenAPI实现。编码前需将请求/响应与授权规则固化为OpenAPI和自动化契约测试。

## 1. 架构边界与响应约定

沿用v2.0：Flutter客户端→Spring Boot模块化单体→MySQL；Redis用于缓存/限流；数据库Outbox+Worker处理媒体、审核、统计和索引；OSS保管私有原件与受控公开派生件；Qdrant仅在语义检索需要时引入。食品、计划、记录和社区先用关系模型，不因“相似度”就上复杂图数据库或独立推荐微服务。

所有路径前缀`/api/v1`。文中`{id}`等为路径参数；不得把客户端传来的householdId当授权依据。认证用现有方向Sa-Token，业务会话/刷新凭据放系统安全存储；签名媒体URL与token不得写入普通日志。

### 1.1 基本协议

- 成功：200读取/修改，201创建，202异步已受理，204删除关系；202不能在UI写“处理完成”。
- 创建/有副作用命令使用`Idempotency-Key`；键作用域为账号+接口+业务目标，保留窗口由后端配置。相同键不同payload返回409，重试必须复用原键。
- 可修改资源返回`revision`与`ETag`；PATCH/危险命令要求`If-Match`。缺失前置条件返回428，版本冲突409或412统一映射`REVISION_CONFLICT`。
- 金额用分等最小货币单位integer；营养、份量、体重用十进制字符串，例如`"25.0"`，避免浮点误差。显示位数由DTO/客户端格式器统一决定。
- 时间戳ISO 8601带偏移，数据库统一UTC；`localDate`与IANA`timezone`用于每日任务/统计。不能用日期字符串推断用户当前设备时区。
- 所有未知值使用null或明确UNKNOWN枚举；0只能表示已知为零；缺失字段与未授权字段区分，但不泄露受限内容是否存在。
- 列表`items,nextCursor,hasMore,snapshotAt`；默认20，上限50为产品初值。total可空，不伪造总数；筛选改变废弃旧cursor。
- 聚合页面返回`sections.{key}.status`，允许局部失败；`capabilities`帮助UI显示按钮，写接口依旧独立鉴权。
- 错误返回HTTP语义码+稳定业务code+用户可理解message+fieldErrors+requestId；日志只含追踪ID与脱敏上下文。

```json
{
  "data": {
    "items": [],
    "nextCursor": null,
    "hasMore": false,
    "capabilities": {"canCreate": true}
  },
  "meta": {
    "requestId": "req_demo_001",
    "serverTime": "2026-10-25T09:00:00+08:00",
    "schemaVersion": "1.0",
    "snapshotAt": "2026-10-25T09:00:00+08:00"
  }
}
```

```json
{
  "error": {
    "code": "REVISION_CONFLICT",
    "message": "计划已被其他照护者更新，请重新预览",
    "fieldErrors": [],
    "currentRevision": 4,
    "retryable": false,
    "requestId": "req_demo_002"
  }
}
```

### 1.2 公共DTO与私有DTO不可共用序列化

`PublicPetSnapshot`仅含不可反查私有petId的publicSubjectId、物种、观察时年龄区间、授权的体型/品种/绝育/活动字段、可选公共昵称头像。年龄来源`EXACT_DERIVED/ESTIMATED/UNKNOWN`；公开对象不含精确生日、家庭名、住址、联系方式、病历和私有媒体路径。

`PetNutritionProfile`是私人对象，含petId、profileRevision、体重及测量日、生命周期及来源、BCS/MCS来源、健康限制/未知值、回避原料、同意用途。客户端为当前用户请求MATCH时可以传petId，但服务端只能按权限读取；别人的背景只能读取已发布快照。

## 2. 核心实体与版本

| 实体 | 必要字段 | 约束与关联 |
|---|---|---|
| FoodProduct | id、brandId/name、name、species、marketRegion、status | 品牌相同不等于同配方；产品可以多个版本 |
| FoodVersion | id、productId可空、scope、versionLabel、foodForm、nutritionPurpose、lifeStageRaw、energy、nutrients、ingredientsRaw、source、verification、revision | 已被计划/记录引用的版本不可就地改；私人手填版本也可被引用 |
| FoodSKU | id、productId、barcode、netWeight、marketRegion、versionEvidence | 同条码可能多版本，不设条码→配方单值假设 |
| FoodCard | id、householdId、foodVersionId、state、privateNote、revision | 家庭私有；停用不删除历史 |
| FoodLot | id、cardId、skuId可空、batchNo、openedAt、expiresOn、remaining、priceMinor、currency | 库存与公开营养资料隔离 |
| Mixture/Version | id、versionId、householdId、petId可空、title、purpose、items、sourcePublicationVersionId、status、revision | items固定foodVersionId和quantity；公共副本需重新审核 |
| FeedingPlan/Version | id、versionId、petId、mixtureVersionId、effectiveFrom/to、timezone、schedule、revision、status | 同宠物主计划生效区间不得重叠；未来版本可排期 |
| TaskRule/Instance | ruleId、planVersionId、petId、slotKey、dueAt、status、assignee、revision | unique(ruleVersion,localDate,slotKey)；完成不自动等于已吃完 |
| FeedingEvent | id、petScope、happenedAt、recordedBy、performedBy、items、taskInstanceId、planVersionId、source、revision | taskInstanceId非空时唯一；共享事件不灌入每只个体摄入 |
| ObservationEpisode | id、privatePetId、targetType/id/version、startDate/endDate、ongoing、cofeeding、interventions、observations、evidenceRefs | 同一宠物/目标/重叠使用期由服务端合并或冲突提示 |
| Publication/Version | id、versionId、authorId、type、snapshot、visibility、status、activePublicVersion、moderation | 公共快照白名单；source refs仅服务端保留 |
| Review/Version | id、episodeId、authorId、publicSnapshot、dimensions、disclosure、status | 同episode一份主点评，追评列表；统计按宠物去重 |
| Reaction/Favorite | userId、targetType、targetId、reactionType | 唯一组合；点赞和收藏分开，收藏私有 |
| Comment | id、publicationId、rootId、replyToId、authorId、body、status | 两层呈现，审核状态；撤回内容不可继续新评论 |
| MediaAsset/Derivative | id、householdId、objectKey、mime、status、visibility、parentAssetId、revision | 公开派生不复用私有URL；追踪删除依赖 |
| ShareGrant | id、scope、fields、audience、expiresAt、revokedAt、snapshotVersion | 每次读鉴权；签名有效不覆盖撤回状态 |
| Outbox/Operation/Job | id、aggregateId、type、state、attempt、nextRetryAt、resultRef | 事务提交后可靠派发；用户通过operationId查真实结果 |

关系：公共FoodVersion→家庭FoodCard→MixtureVersion→PlanVersion→TaskInstance→FeedingEvent；ObservationEpisode可关联其中的实际记录；Publication只发布选定快照。**数据库引用关系不自动授予读取权限。**

### 2.1 生命阶段和品种不是前端硬编码规则

字典API返回species/breed/foodForm/nutritionPurpose/lifeStage及版本。年龄有bornOn或estimatedAgeRange+asOfDate；当时年龄按观察发生日期计算。品种混血/未知为正式值，不猜测。犬的成长阶段需考虑成年体型与专业规则，生日到某一天不直接自动换粮。食品标签原文、结构化映射及映射来源分别保存，无法映射保留UNKNOWN。

### 2.2 食品能量与营养结构

```json
{
  "foodVersionId": "fv_demo_dry_1",
  "species": ["CAT"],
  "foodForm": "DRY",
  "nutritionPurpose": "COMPLETE",
  "lifeStageRaw": "成年猫",
  "energy": {
    "value": "4000",
    "unit": "KCAL_PER_KG",
    "servingGrams": null,
    "method": "LABEL_DECLARED",
    "verification": "USER_CONFIRMED",
    "kcalPerGram": "4.0"
  },
  "nutrients": [
    {"key": "CRUDE_PROTEIN", "value": "32", "unit": "PERCENT", "comparator": "GTE", "basis": "AS_FED"},
    {"key": "MOISTURE", "value": "10", "unit": "PERCENT", "comparator": "LTE", "basis": "AS_FED"}
  ],
  "source": {"type": "PACKAGE_LABEL", "assetId": "asset_demo_label", "checkedAt": "2026-10-24T12:00:00+08:00"}
}
```

此为虚构原型数据，不是任何真实品牌或营养建议。energy原始单位保留；不从保证分析值反算伪精确热量；能量来源可以LABEL_DECLARED/MANUFACTURER_DOCUMENT/USER_PROVIDED/UNKNOWN。第三方声称全价的标签核对不是平台检测认证。

**单位计算**：kcal/kg÷1000=kcal/g；kcal/100g÷100=kcal/g；kcal/份在有明确每份g时才转换。按g计算`quantityG × kcalPerGram`；按份可直接乘每份kcal。毫升不当作克，没有密度不换算。每包装的份数不自动代表每份净重，须有一致证据。

**保证分析**：存比较符与口径；干物质示意公式`asFedPercent/(100-moisturePercent)×100`仅在可转换时返回，并附“按标签估算”及原上下界信息；保证值组合不是精确营养组成，不能将两个上下界机械推导成一个严谨区间或排序。水分缺失/100%/未知口径均不可换算。

## 3. 饮食计算、计划与投喂的详细契约

### 3.1 DIET_CALCULATE（无写入）

`POST /diet/calculations`请求：petId可选、profileRevision、items[{foodVersionId,quantity:{value,unit,servingDefinitionId?}}]、purpose、referenceDate。服务器从版本读取能量，不能相信客户端传的kcal。

```json
{
  "data": {
    "calculationId": "calc_demo_1",
    "calculationVersion": "diet-v1",
    "items": [
      {"foodVersionId": "fv_demo_dry_1", "grams": "40", "kcal": "160", "status": "KNOWN"},
      {"foodVersionId": "fv_demo_unknown_1", "grams": "90", "kcal": null, "status": "MISSING_ENERGY"}
    ],
    "summary": {
      "knownKcal": "160", "totalKcal": null,
      "energyCompleteness": "PARTIAL", "knownItemCount": 1, "itemCount": 2,
      "supplementEnergyRatio": null
    },
    "missingItems": [{"foodVersionId": "fv_demo_unknown_1", "fields": ["energy.value", "energy.unit"]}],
    "checks": [{"code": "ENERGY_INCOMPLETE", "level": "INFO", "message": "另有1项食品尚未提供能量"}]
  }
}
```

份量不能为负，0只用于显式“不使用此项”的草稿状态，正式餐次不保存0占位；UNKNOWN不是0。所有项可算时totalKcal=knownKcal；knownItemCount/itemCount是**条目覆盖**不是摄入全天覆盖。补充类占比只有总能量和用途均可判定才计算；未标用途另提示，不凑满100%。

### 3.2 PLAN_PREVIEW → PLAN_ACTIVATE

`POST /feeding-plans/{planId}/preview`请求草稿版本、目标宠物、目标生效时间、家庭时区、预期旧计划revision。返回previewToken、expiresAt、新旧差异、受影响未来实例、不可替换已完成实例、conflicts、checks。

`POST /feeding-plans/{planId}/activate`携previewToken、If-Match、Idempotency-Key；服务端再次确认权限、宠物/食品/规则版本、有效时间无重叠。单数据库事务写新计划状态、旧计划终止边界、未来任务调整指令和Outbox；Worker幂等补建实例。若任务异步生成，返回202与jobId、activationStatus=PREPARING；生成完成后ACTIVE，失败保留可恢复状态并明确原计划是否仍有效。

实现选择必须固定：本设计建议**先在事务中保留原有效计划，新版本PREPARING；任务准备成功后原子切换有效指针与取消未来旧实例**。切换窗口执行任务时重新检查生效指针和revision；不能出现新旧两套都可执行的空窗。小规模可在同事务预建有限窗口后直接201/200，其余滚动生成，关键是切换原子性。

当日已DONE的早餐保持原事件/版本；只替换未来待办。SNOOZED实例如何迁移需在preview返回明细，默认保留原实例并让用户选择取消或保留，不能悄悄把延后任务变成两餐。实例唯一键含计划规则版本和本地槽位；每日时间规则依IANA时区执行。

### 3.3 FEEDING_CREATE

```json
{
  "petScope": {"type": "INDIVIDUAL", "petIds": ["pet_demo_tuanzi"]},
  "taskInstanceId": "task_demo_20261025_breakfast",
  "expectedTaskRevision": 2,
  "planVersionId": "planv_demo_1",
  "happenedAt": "2026-10-25T08:05:00+08:00",
  "timezone": "Asia/Hong_Kong",
  "items": [{
    "foodVersionId": "fv_demo_dry_1",
    "offered": {"value": "25", "unit": "G"},
    "consumed": null,
    "consumptionStatus": "UNOBSERVED"
  }],
  "performedBy": null,
  "note": "",
  "source": "MANUAL"
}
```

返回eventId、recordedBy（服务端取会话）、offeredKnownKcal、consumedKnownKcal可空、task状态和revision、operationId。事务锁任务实例→确认未完成且有权限→插事件→标DONE并关联eventId→Outbox通知；unique(taskInstanceId)兜底。任务完成不是喂食事实的传感器证明。

独立新记录taskInstanceId可空；共享碗scope=SHARED只统计共享投喂，个体摄入为UNKNOWN。已记录实际额外投喂须新建不关联同一完成槽的事件，并可关联`relatedTaskId`作为说明，不能复用完成动作。

`PATCH /feeding-events/{eventId}/observation`：剩余量/比例/称重或估计方法、观察时间、修正说明；consumed≤offered；混合剩余无法分解则保留混合残量、不按比例捏造；修改需要revision。服务端统一重算总量与统计。

### 3.4 撤销与时间覆盖

删除/纠正事件必须说明是否影响任务完成状态；同一次事务返回任务新状态。撤销不证明物理行为未发生，UI文案不可说“已撤回喂食”。同一宠物日统计依happenedAt和家庭timezone；补录的createdAt另存。计划、已投喂、已观察摄入分别聚合；未知摄入日期为null。每日覆盖率需区分“有记录”“所有启用餐次有记录”“全天进食完整自述”，不能用完成率代表全部摄入。

## 4. 匹配与公开检索契约

### 4.1 MATCH请求与响应

`POST /matches/evaluate`：candidateType（FOOD/MIXTURE/PUBLICATION）、candidateVersionId、petId、profileRevision、mode（PET_SIMILARITY/FOOD_SIMILARITY/MIXTURE_SIMILARITY）、explicitFilters。响应不得包含未经公开的他人petId或私人健康限制。

```json
{
  "data": {
    "candidateVersionId": "pubv_demo_partial_1",
    "profileRevision": 3,
    "ruleVersion": "reference-v1",
    "generatedAt": "2026-10-25T09:00:00+08:00",
    "suitability": {
      "status": "NEED_INFO",
      "reasons": [{"code": "FOOD_ENERGY_MISSING", "text": "其中1项食品能量尚未核对"}],
      "missingFields": ["candidate.foods[1].energy"],
      "allowedActions": ["FAVORITE", "SAVE_PRIVATE_DRAFT", "EDIT_DATA"],
      "blockedActions": ["AUTO_PORTION", "AUTO_ACTIVATE"]
    },
    "similarity": {
      "label": "部分条件相近",
      "coverage": "0.90",
      "matchedDimensions": [{"key": "SPECIES", "text": "同为猫"}, {"key": "LIFE_STAGE", "text": "同为成年阶段"}, {"key": "WEIGHT_RANGE", "text": "体重区间接近"}, {"key": "NEUTER_STATUS", "text": "绝育状态相同"}],
      "differentDimensions": [{"key": "BREED", "text": "品种不同"}],
      "missingDimensions": ["ACTIVITY"],
      "displayScore": null
    },
    "evidence": {"type": "APP_RECORDS_LINKED", "daysWithRecords": 14, "periodDays": 21, "fullDayIntakeConfirmed": false}
  }
}
```

相似分只用于内部排序，不传可诱导误用的健康百分比。健康资料不完整、不适用或有已知冲突，返回能力限制。手动按既有医嘱安排与自动套用是不同能力；可记录真实发生的事，不代表推荐该行为。

### 4.2 内部排序的可解释初稿

1. ACL/公开状态/物种/用户明确条件/已知禁用规则先过滤。
2. 已公开的生命阶段、体型或体重区间、绝育、品种、活动分别算相似。初值权重35/30/15/10/10；缺失维度不当匹配。内部`score=sum(w*s)/sum(available_w)`同时计算`coverage=sum(available_w)/100`，coverage不足0.6不显示强匹配标签。物种是前置条件不再重复加分。样例权重没有临床意义。
3. 记录覆盖与信息完整性只衡量可解释程度；默认结果保障负面观察可检索、商业内容单独标识；按作者/家庭/食品去重并做多样化。点赞不作为营养适用的分值。
4. 输入宠物画像、食品版本、规则版本任一变化即失效。缓存键含授权范围/画像版本/查询hash/公开版本；禁止全站复用个人结果。

年龄区间、品牌变体、食材标签采用字典版本。食品相似和宠物相似两种score独立，不能相加当成健康推荐。结构化检索不足时允许语义召回，但返回前重新执行硬过滤和public-state校验。

### 4.3 公共查询与统计

`GET /public/search`参数：q、type、species、lifeStage、ageMinMonths/ageMaxMonths、weightMin/Max、breedId、neuterStatus、foodForm、nutritionPurpose、minObservationDays、evidenceType、disclosure、sort、cursor。个性化画像参数使用另一个已授权`contextId`，不在GET查询串写病史/过敏原。

响应含appliedFilters、rankingMode、snapshotAt、items、nextCursor；空结果不暗改筛选；全量计数昂贵时total=null。`rankingMode=LATEST`在个性化关闭时不读取个人画像，用户显式筛选可保留。

点评聚合按公开可用且未作废观察；同一宠物/目标版本当前取最新有效episode，排除同家庭重复和已撤回内容。初始比例展示门槛≥10独立宠物且≥5独立家庭；不足则`distribution=null,suppressionReason=SMALL_SAMPLE`。某维度未观察不进该维度分母；返回observedCount/unobservedCount/uniquePets/uniqueHouseholds/statSnapshotAt。对小队列防差分查询和去匿名化，必要时不返回精确家庭数。

## 5. 公开发布、审核与数据删除

### 5.1 发布请求

`POST /publications/drafts`输入sourceType/sourceVersionId、episodeId可空、title/body、disclosure、selectedPublicFields、selectedMediaIds。服务器检查源资料访问权与publish权限，提取白名单快照，并生成previewId/hash。**客户端传入的整个私人对象不能直接成为publicSnapshot。**

`POST /publications/{id}/submit`输入draftRevision、previewHash、audience=PUBLIC、确认的条款版本；重新验证资源未撤回、证据仍可用、图片处理完成、公开主体授权。返回202、publicationVersionId、PENDING_REVIEW。审核Worker只审固定版本；正文再改则旧审核不能批准新内容。

公开快照字段：title、body、publicAuthor、publicPetSnapshot、foodVersionPublicRefs、mixtureItems、observationPeriod、dimensions、evidenceSummary、commercialDisclosure、processedMediaRefs、sourceAttribution，以及可选cofeedingContext、confounderFlags。后两项仅公开用户选择的混喂/同时有其他干预等概括标签，不携具体药名、剂量或诊断。可另选publicBudgetRange{minMinor,maxMinor,currency,period}，仅来自用户独立自述且显式选择公开，period固定DAY，不读取账本。精确生日、家庭成员、私有费用、原医疗附件、私人联系方式和图片EXIF不在白名单。媒体需生成公开派生件、清EXIF、确认可见内容；清EXIF不能移除照片里拍到的门牌/病历姓名，预览须明确检查。

`POST /publications/{id}/withdraw`即时更新不可读状态并写Outbox；公开详情每次读先查状态，不能等搜索索引同步才撤回。公开媒体使用受控访问层或可快速撤销的分发策略；已经被用户截图/下载的副本无法召回。前端公开缓存需短期限，收到撤回信号清理。

### 5.2 版本引用与原始资料删除

保留`publication_source_ref`与`media_derivative_parent`依赖关系。用户删除一条私人证据时，先在确认页列出受影响的公开经验和证据徽标，再按政策撤回/移除徽标/重新审核。撤销公开授权即阻止后续公开读取；将清理任务下发到DB、搜索索引、向量库、对象存储、缓存与导出文件。任务返回分项状态，不承诺请求成功即所有备份已物理清除。

已复制到他人私有搭配的结构化信息和署名遵守发布前告知的复制规则：作者撤回后来源链接不可读，不能继续传播原图/私有背景；若为危险内容或侵权，可停止新复制并提示现有副本风险。需要随法律依据保留的审核/申诉日志须最小化、隔离访问、按明确期限处理，不能以审计名义永久保留全部原始媒体。

### 5.3 点赞、收藏、点评与评论

- 点赞`PUT /reactions/{targetType}/{targetId}/like`表示期望已点赞，DELETE表示期望未点赞；不要使用容易重试翻转两次的toggle API。返回我的状态和服务器计数；计数可最终一致，状态需可读回。
- 收藏同样PUT/DELETE期望状态；仅本人可读，默认不通知作者。
- 点评`POST /reviews/drafts`先查询/锁定ObservationEpisode；已存在返回可更新的reviewId。作者上传记录只证明应用内关联，绝不返回verifiedEffect=true。
- 评论`POST /publications/{id}/comments`返回PENDING_REVIEW/已受理；本人可见pending，其他用户不可见；通过才计公共数量。评论删除与内容下架时分页过滤统一。
- 审核/举报不默认异步成功；不确定时留待人工处理。公共入口功能开关关闭，读接口和写接口均遵守，而非前端隐藏按钮即可。

## 6. 接口目录：页面别名一一落地

下表每个大写别名均对应主文档中的`接口/状态`引用。路径是建议命名。公共响应、列表、版本、幂等和权限按第1节继承；同一路径多个动作按方法区分。范围：`私`=家庭/宠物ACL，`本`=账号本人，`公`=已公开投影，`管`=运营后台受限权限。

### 6.1 账号、家庭与宠物

| 接口别名 | 方法与路径 | 关键输入 → 响应；范围 |
|---|---|---|
| BOOTSTRAP | GET `/bootstrap` | appVersion/platform → featureFlags、legalVersions、uploadCapabilities、dictionaryVersions；公 |
| LEGAL_DOCS | GET `/legal-documents/{type}?version=` | 类型/版本 → title、body、effectiveAt、version；公 |
| AUTH | POST `/auth/register`、`/auth/login`、`/auth/challenges`、`/auth/challenges/{id}/verify`、`/auth/password-resets` | 邮箱/密码或challenge → 会话或challengeId/有效期；限流、验证前后分权限 |
| ONBOARD | POST `/onboarding/complete` | createPet或acceptInvite意图 → household/pet/currentContext；本，创建原子且幂等 |
| ME | GET/PATCH `/me` | 账号头像昵称 → 私人资料、家庭、权益摘要；本 |
| ACCOUNT_SECURITY | POST `/me/password-changes`、`/me/email-changes` | 身份确认+新信息 → 操作状态；本，邮箱变更需验证流程 |
| SESSIONS | GET `/me/sessions`；DELETE `/me/sessions/{id}`；POST `/auth/logout` | 会话/设备 → 最近活动、撤销结果；本 |
| HOUSEHOLDS | GET/POST `/households`；PATCH `/households/{id}`；POST `/households/{id}/leave` | 名称或退出确认 → 家庭/影响；私 |
| MEMBERS | GET `/households/{id}/members`；PATCH/DELETE `/households/{id}/members/{memberId}` | 角色、资源/宠物scope、期限、revision → permissions；私，最小授权 |
| INVITES | POST `/households/{id}/invites`；POST `/invites/preview`、`/invites/accept`；DELETE `/invites/{id}` | token或邀请设置 → 范围/到期/接受结果；预览最小信息，写需身份 |
| OWNERSHIP | POST `/households/{id}/ownership-transfers`、`/ownership-transfers/{id}/accept` | 目标成员、身份凭据 → 双方确认状态；Owner与接收者 |
| PETS | GET/POST `/households/{id}/pets`；PATCH `/pets/{id}`；PUT `/households/{id}/pet-order` | 宠物字段/排序ID数组 → profile/revision/capabilities；私 |
| PET_DETAIL | GET `/pets/{id}/overview` | petId → 基本档案、任务/回忆摘要、分区权限；私 |
| NUTRITION_PROFILE | GET/PATCH `/pets/{id}/nutrition-profile` | 私人营养相关字段/同意用途 → profileRevision、missingFields；私 |
| PET_LIFECYCLE | POST `/pets/{id}/archive`、`/restore`、`/memorialize` | 影响确认/未来日程选项 → 状态与任务变化；Owner |
| DICTIONARIES | GET `/dictionaries/{type}?version=` | 种类/版本 → items、version、locale、deprecated；公/配置 |
| HANDOFF | POST `/handoffs/preview`、`/handoffs`；GET `/handoffs/{id}` | 宠物范围/字段/日期 → 脱敏卡片/分享范围；私 |
| DELETION | POST `/deletion-requests`；GET `/deletion-requests/{id}`；POST `/deletion-requests/{id}/cancel` | target、身份确认、影响确认 → deadline、分项状态；本/Owner |

AUTH重置细化：POST `/auth/password-reset-challenges`发送；POST `/auth/password-reset-challenges/{id}/verify`取得一次性resetCredential；POST `/auth/password-resets`使用凭据与新密码完成。AUTH注册验证成功后再签发正式会话。是否支持密码泄露筛查/验证码长度由配置与实施安全评审确定，UI不能推断后台已具备。

### 6.2 今日、任务与日常健康

| 接口别名 | 方法与路径 | 关键输入 → 响应；范围 |
|---|---|---|
| TODAY | GET `/pets/{id}/today?date=`；GET `/households/{id}/today` | 日期/家庭时区 → tasks、nutrition、trends、memories、各区状态；私 |
| TASKS | GET `/tasks?petId=&date=&status=` | 过滤 → 实例列表；私 |
| TASK_DETAIL | GET `/tasks/{id}` | id → 任务明细、执行历史、revision；私 |
| TASK_COMPLETE | POST `/tasks/{id}/complete`；POST `/tasks/{id}/snooze`、`/skip`、`/reopen` | revision、执行信息/延后时间/原因 → event/task/operation；私 |
| TASK_RULES | POST/PATCH `/task-rules`、`/task-rules/{id}`；POST `/task-rules/{id}/pause` | 周期、时区、月末规则、执行人 → rule及未来预览；私 |
| NOTIFICATIONS | GET `/notifications`；POST `/notifications/read` | IDs/游标 → 站内通知与未读数；本 |
| NOTIFICATION_PREFS | GET/PATCH `/me/notification-preferences`；POST `/me/notification-tests` | 类别/静默时段 → 设置与投递状态；本 |
| CARE_OVERVIEW | GET `/pets/{id}/care-overview?section=` | 饮食/健康/记录 → 按需区块；私 |
| QUICK_ACTIONS | GET/PUT `/me/quick-actions` | 类型顺序 → 可用动作/权限；本 |
| EVENTS | GET/POST `/care-events`；PATCH/DELETE `/care-events/{id}` | 类型schema、时间、宠物范围、attributes → event/revision；私 |
| EVENT_DETAIL | GET `/care-events/{id}`；GET `/care-events/{id}/revisions` | id → 原字段、关联、历史；私 |
| HEALTH_OVERVIEW | GET `/pets/{id}/health-overview` | petId → 体重/体况/医疗/观察摘要；私 |
| METRICS | GET/POST `/pets/{id}/metrics`；PATCH/DELETE `/metrics/{id}` | 类型、数值/量表/来源/日期 → metric、trend；私 |
| ASSESSMENT_GUIDES | GET `/assessment-guides?species=&type=` | 物种/BCS或MCS → 版本、内容、许可信息、媒体；公 |
| ACTIVITY_SESSION | POST `/activity-sessions`；PATCH `/activity-sessions/{id}`；POST `/activity-sessions/{id}/finish` | 起止/暂停区间 → 草稿/正式事件；私 |
| SYMPTOM_GUIDANCE | POST `/symptom-guidance/evaluate` | 用户明确观察 → 信息级别、联系入口、ruleVersion；私 |
| PREVENTIVE_EVENTS | GET/POST `/pets/{id}/preventive-events` | 疫苗/驱虫、执行字段/可选提醒 → event、taskRule或分项状态；私 |
| MEDICATION_PLANS | GET/POST `/pets/{id}/medication-plans`；PATCH `/medication-plans/{id}`；POST `/medication-plans/{id}/pause` | 原医嘱、药名/规格/剂量/途径/周期 → 版本/任务；私 |
| MEDICATION_COMPLETE | POST `/medication-tasks/{id}/complete` | 人工核对凭据、revision → 执行事件/operation；私，联网核验 |
| VISITS | GET/POST `/pets/{id}/visits`；PATCH/DELETE `/visits/{id}` | 就诊/确认字段/账本和提醒选择 → visit及附属结果；私 |
| VISIT_DETAIL | GET `/visits/{id}` | id → 摘要、医嘱、附件、检验原口径、费用权限；私 |
| MEDICAL_FILES | GET `/pets/{id}/medical-files`；PATCH `/medical-files/{id}` | 分类/visit绑定 → 文件清单/校对状态；私 |
| EMERGENCY_CARD | GET/PATCH `/pets/{id}/emergency-card` | 联系人与授权资料 → 明确来源和时间的卡；私 |

日常attributes最小字典：water{mode,volumeMl?,observation?,method,shared}；elimination{type,observedCount?,condition?,attribution,photoRefs}；activity{startedAt,endedAt,pauseIntervals,intensity}；symptom{type/freeText,frequency?,duration?,subjectiveSeverity,contactedVet}；care{careType,performedAt}。类型与schemaVersion必须校验，未知字段不得直接成为统计指标。source只允许服务端认可的manual/device/import/ai_confirmed等来源，客户端不能自称兽医验证。

### 6.3 食品、搭配、计划与观察

| 接口别名 | 方法与路径 | 关键输入 → 响应；范围 |
|---|---|---|
| FOOD_SEARCH | GET `/public/foods?query=&species=` | 关键词/物种/用途 → 版本候选、核对来源；公 |
| BARCODE | GET `/public/foods/by-barcode/{barcode}` | 条码/地区 → candidates[]，不保证唯一；公 |
| FOOD_DETAIL | GET `/food-versions/{id}` | id/上下文 → 标签、用途、来源、版本；按public/private scope鉴权 |
| FOOD_CARDS | GET/POST `/households/{id}/food-cards`；PATCH `/food-cards/{id}` | 引用/私人新版本、在用状态 → card/revision；私 |
| FOOD_PROPOSALS | POST `/food-proposals`；GET `/food-proposals/{id}` | 标签证据、变更字段、候选目标 → 待核对/审核状态；本/管 |
| FOOD_COMPARE | POST `/foods/compare` | 2–3个versionId、basis → 同口径值/不可比原因；授权范围 |
| FOOD_LOTS | GET/POST `/food-cards/{id}/lots`；PATCH `/food-lots/{id}` | 规格、日期、剩余、价格 → 包装与校正记录；私 |
| MIXTURES | GET/POST `/households/{id}/mixtures`；POST `/mixtures/{id}/archive` | 标题/范围 → 列表或新草稿；私 |
| MIXTURE_DRAFT | GET/PATCH `/mixtures/{id}/draft`；POST `/mixtures/{id}/versions` | items、版本来源、revision → 草稿/不可变版本；私 |
| MIXTURE_DETAIL | GET `/mixtures/{id}?version=` | 版本 → 食品/份量/计算/来源/引用计划；私 |
| DIET_CALCULATE | POST `/diet/calculations` | 详第3.1节 → 逐项/已知/完整热量、checks；授权范围 |
| PLANS | GET/POST `/pets/{id}/feeding-plans`；POST `/feeding-plans/{id}/pause`、`/resume` | 草稿/时间 → 当前/未来/历史版本；私 |
| PLAN_DRAFT | GET/PATCH `/feeding-plans/{id}/draft` | 食品/餐次/周期/执行人 → 草稿、缺失字段；私 |
| PLAN_PREVIEW | POST `/feeding-plans/{id}/preview` | 详第3.2节 → previewToken/差异/冲突；私 |
| PLAN_ACTIVATE | POST `/feeding-plans/{id}/activate` | previewToken/If-Match → ACTIVE或PREPARING+jobId；私 |
| ENERGY_ESTIMATE | POST `/pets/{id}/energy-estimates` | 确认过的资料/专业人工目标 → eligibility/估算/来源，不激活；私 |
| TRANSITIONS | GET/POST `/pets/{id}/diet-transitions`；PATCH `/diet-transitions/{id}`；POST `/diet-transitions/{id}/pause` | 来源/目标版本、分段日期份量 → 主计划阶段与观察轴；私 |
| FEEDING_CREATE | POST `/feeding-events` | 详第3.3节 → event/task/operation；私 |
| FEEDING_OBSERVATION | PATCH `/feeding-events/{id}/observation` | 剩余/摄入观察、方法、revision → 修订值；私 |
| FEEDING_STATS | GET `/pets/{id}/feeding-stats?from=&to=` | 日期/口径 → 计划/投喂/摄入/覆盖/缺失；私 |
| OBSERVATION_EPISODES | GET/POST `/pets/{id}/observation-episodes`；PATCH `/observation-episodes/{id}` | 使用期、目标、混喂、观察及证据 → episode/revision；私 |

### 6.4 发现、社区与个人偏好

| 接口别名 | 方法与路径 | 关键输入 → 响应；范围 |
|---|---|---|
| DISCOVERY | GET `/public/discovery?type=&sort=&contextId=&cursor=` | 公开类型/排序/已授权上下文 → 结构化列表、实际排序模式；公/个性化鉴权 |
| PUBLIC_SEARCH | GET `/public/search` | 详第4.3节 → appliedFilters、items、cursor；公 |
| MATCH | POST `/matches/evaluate` | 详第4.1节 → suitability/similarity/evidence；个人上下文需授权 |
| PREFERENCES | GET/PATCH `/me/recommendation-preferences` | 个性化/不想看/显式偏好 → 当前有效设置；本 |
| PUBLICATION_DRAFT | POST `/publications/drafts`；GET/PATCH `/publications/{id}/draft`；POST `/publications/{id}/preview` | 内容/公开字段/来源 → draftRevision/previewHash；本且具发布权 |
| PUBLICATION_SUBMIT | POST `/publications/{id}/submit`；POST `/publications/{id}/withdraw` | 预览确认/撤回 → version/status/job；本 |
| PUBLICATION_DETAIL | GET `/public/publications/{id}` | id/activeVersion → 公共快照+可用操作；公 |
| MY_PUBLICATIONS | GET `/me/publications?type=&status=` | 状态/类型 → 草稿/公开/退回/贡献记录；本 |
| MODERATION_STATUS | GET `/me/moderation-items/{id}` | 资源ID → 状态、原因、可修改字段、申诉资格；本 |
| REVIEWS | GET `/public/reviews?targetType=&targetId=&version=`；POST `/reviews/drafts`；PATCH `/reviews/{id}/draft` | 目标/观察期 → 列表、分项统计、私人草稿；读公，写有权 |
| REVIEW_SUBMIT | POST `/reviews/{id}/preview`、`/submit`、`/follow-ups`、`/withdraw` | 固定版本/追评 → 审核状态与关联周期；本/授权宠物 |
| REACTIONS | PUT/DELETE `/reactions/{type}/{id}/like` | 目标 → likedByMe、count；登录账号 |
| FAVORITES | GET `/me/favorites`；PUT/DELETE `/me/favorites/{type}/{id}` | 目标版本 → 个人收藏状态；本 |
| COMMENTS | GET/POST `/publications/{id}/comments`；DELETE `/comments/{id}` | body/rootId/replyToId → 自己待审或公共评论；按可见性 |
| PUBLIC_PROFILE | GET `/public/users/{id}`；PATCH `/me/public-profile` | 公开名/头像/简介 → 白名单背景/内容；读公，写本 |
| REPORTS | POST `/abuse-reports`；GET `/me/abuse-reports/{id}` | 目标/原因/证据 → 举报工单；本，区别于健康报告 |
| BLOCKS | GET `/me/blocks`；PUT/DELETE `/me/blocks/{publicUserId}` | 公共用户 → 屏蔽关系；本 |
| APPEALS | POST `/moderation-appeals`；GET `/me/moderation-appeals/{id}` | 处理ID、理由、证据 → 申诉工单；本 |

另需复制命令：`POST /publications/{id}/copy-to-private`，输入sourceVersionId、petId可空、允许复制的items；返回私有mixtureDraftId、sourceAttribution、需要重新确认的字段。若源状态或权限变化返回410/403；只有明确执行该动作才计reference_copied，收藏不计复制/使用。

审核后台建议`GET /admin/moderation/queue`、`POST /admin/moderation/{id}/decision`、`POST /admin/food-proposals/{id}/resolve`、`GET/PATCH /admin/abuse-reports/{id}`；审核决定与快照hash、操作者、规则版本绑定。管理员无权默认浏览整个私人家庭数据库；公开审核所需材料为限目的授权副本。

### 6.5 媒体、回忆、账本、AI与设置

| 接口别名 | 方法与路径 | 关键输入 → 响应；范围 |
|---|---|---|
| MEMORY_TIMELINE | GET `/pets/{id}/memories?view=&cursor=`；GET `/households/{id}/memories` | 生活/健康/月份 → 授权事件/媒体/日记引用；私 |
| ALBUMS | GET/POST `/households/{id}/albums`；PATCH/DELETE `/albums/{id}`；PUT `/albums/{id}/items` | 名称/条目/解散或删素材选项 → album/影响；私 |
| MEDIA_LIST | GET `/media?petId=&albumId=&type=&cursor=` | 授权筛选 → 缩略图引用/处理状态；私 |
| UPLOAD | POST `/uploads`；POST `/uploads/{id}/refresh`；DELETE `/uploads/{id}` | 文件大小/类型/用途 → uploadId、限时凭证、限制；私，服务端再验实际内容 |
| MEDIA_COMMIT | POST `/uploads/{id}/commit` | 实际上传完成+元信息 → assetId/PROCESSING/jobId；私 |
| MEDIA_DETAIL | GET `/media/{id}` | id → 元信息/处理状态/权限，不直接永久URL；私/公共派生 |
| MEDIA_ACCESS | POST `/media/{id}/access` | 用途/variant → 短效访问令牌或URL/到期；每次鉴权 |
| MEDIA_UPDATE | PATCH/DELETE `/media/{id}`；POST `/media/batch-operations` | 标签、归属、revision或影响确认 → 结果/删除job；私 |
| MEDIA_SEARCH | POST `/media/search` | 文本/精准条件/语义模式 → 授权结果/降级原因；私 |
| STORAGE | GET `/me/storage-usage` | 无 → 原件/派生/配额/待清理量；本 |
| JOBS | GET `/jobs/{id}`；POST `/jobs/{id}/retry`、`/cancel`；GET `/operations/{id}` | job或操作ID → state/progress/result/error；发起者/授权成员 |
| OCR_JOB | POST `/ocr-jobs` | assetId、domain、可处理授权 → jobId/待核对字段；私 |
| JOURNALS | GET/POST `/pets/{id}/journals`；GET/PATCH/DELETE `/journals/{id}` | bodyDoc、引用、草稿/发布私密状态 → 日记/revision；私 |
| STORIES | POST `/story-jobs`；GET/PATCH `/stories/{id}` | 选定素材/模板/文案 → 固定模板私密故事；私 |
| EXPENSES | GET/POST `/expenses`；PATCH/DELETE `/expenses/{id}` | 金额/币种/分类/归属/关联 → expense/revision；费用ACL |
| EXPENSE_STATS | GET `/households/{id}/expense-stats?month=&petId=` | 范围 → 按币种分组金额/类别/覆盖；费用ACL |
| BUDGETS | GET/POST `/budgets`；PATCH `/budgets/{id}` | 周期/币种/阈值/订阅日期 → 预算与提醒；私 |
| CUSTOM_SCHEMAS | GET/POST `/custom-event-schemas`；POST `/custom-event-schemas/{id}/versions`；POST `/custom-event-schemas/{id}/archive` | 受控字段定义 → schema/version/预览；私 |
| AI_CHAT | POST `/ai/conversations`；POST `/ai/conversations/{id}/messages`；GET `/ai/runs/{id}/events`；POST `/ai/runs/{id}/cancel` | 用户文本/授权附件/上下文 → runId/SSE；私 |
| AI_DRAFT | GET/PATCH `/ai/action-drafts/{id}`；POST `/ai/action-drafts/{id}/confirmation-challenges` | 字段修改/用户核对请求 → draftHash/短效确认凭据；私 |
| AI_COMMIT | POST `/ai/action-drafts/{id}/commit` | confirmationToken、draftHash、幂等键 → 正式事件/operation；私 |
| AI_HISTORY | GET `/ai/conversations`；GET/DELETE `/ai/conversations/{id}` | 宠物/日期 → 历史与引用可见性；本/私 |
| AI_CONSENT | GET/PATCH `/me/ai-consents` | 资源类型、用途、版本 → 有效授权；本 |
| REPORT_JOBS | POST `/report-jobs`；GET `/reports?petId=` | 日期/模块/模板/选定媒体 → jobId/报告列表；私 |
| REPORT_DETAIL | GET `/reports/{id}` | id → 结构化报告、snapshotAt、templateVersion、引用；私 |
| EXPORT_JOBS | POST `/export-jobs` | 范围/format/脱敏字段 → jobId/到期下载入口；本/授权资源 |
| SHARES | POST `/shares/preview`、`/shares`；GET/DELETE `/shares/{id}`；POST `/share-access` | 范围/字段/受众/到期/token → 授权预览或内容；私或受限接收者 |
| PRIVACY_PREFS | GET/PATCH `/me/privacy-preferences` | 可见性/个性化/数据用途 → 当前设置/传播状态；本 |
| ENTITLEMENTS | GET `/me/entitlements` | 无 → 有效权益、用量、重置周期；本 |
| BILLING | GET `/billing/products`；POST `/billing/receipts/verify`、`/billing/restore` | 平台凭据 → 经服务器验证的购买状态；本 |
| SUPPORT | GET `/help/articles`、`/help/articles/{id}`；POST `/support-tickets`；GET `/me/support-tickets/{id}` | 问题/附件/诊断包同意 → 帮助或工单；公/本 |

PRIVACY_PREFS与PREFERENCES中的个性化开关必须指向同一服务端状态，不能出现发现页关了但设置页仍开启。`performedBy`等显示别名和`recordedBy`审计身份同样只维护一个事实来源。

## 7. 异步、流式、缓存和失败的界面协议

### 7.1 异步任务

job通用：`id,type,status,stage,progress?,createdAt,updatedAt,resultRef?,error?,retryable,canCancel`。状态`QUEUED→RUNNING→SUCCEEDED/FAILED/CANCELLED`；progress没有真实分母则null。推荐初始轮询间隔2/5/10秒，后台降低频率，服务端返回nextPollAfterMs；有可靠SSE/push则通知后重取权威状态。

OCR成功是`SUCCEEDED`产生待核对草稿，不是正式记录。媒体原件上传成功但缩略图未就绪时显示PROCESSING，不能重复上传原件。取消job不必然能回滚已完成的外部调用，UI说明“已请求取消”直到后端确认终态。

提交超时后客户端保留幂等键和operationId，`GET /operations/{id}`查状态；未取得id时可提供`GET /operations/by-key?key=`（当前账号鉴权，键不可包含敏感正文）查询原结果。只有明确失败且可重试才重新提交；不要生成新key掩盖不确定状态。

### 7.2 AI SSE

事件包均含eventId、runId、conversationId、sequence、schemaVersion。允许类型`message.delta`、`tool.status`、`ui.card`、`ui.chart`、`action.draft`、`message.done`、`error`。受控UI组件类型白名单，不接收任意HTML/JS/SQL；未知type忽略并提供文字回退。断线用Last-Event-ID续传；旧事件幂等去重，断线不得重新执行commit。

### 7.3 版本失效与本地缓存

- 私人数据按账号/家庭/宠物/ACL版本分区加密缓存；退出、撤权、切家庭清理不再可访问分区。公开缓存和私人缓存物理/逻辑分离。
- 手机离线无法立刻接收服务器撤权；只能通过短有效期、重新联网校验和最小缓存降低窗口，不能宣称离线设备可被瞬间远程清空。病历/应急缓存是否允许由用户和角色授权决定。
- sourceRevision、profileRevision、ruleVersion变化使匹配结果过期；公开结果读时验证activePublicVersion；搜索索引不是权限权威。
- 点赞可乐观更新后回滚；任务、医疗、激活、发布显示pending直至服务端确认。列表数量最终一致可有更新时间，但不能用缓存计数驱动安全状态。

### 7.4 业务错误码与UI处理

| HTTP/code | UI文案/呈现 | 动作 |
|---|---|---|
| 400 `INVALID_ARGUMENT` | 字段紧邻错误 | 聚焦首错，保留输入 |
| 401 `SESSION_EXPIRED` | “登录已过期” | 本地加密草稿保留，登录后回原页，敏感操作重确认 |
| 403 `ACCESS_REVOKED` | “访问权限已变化” | 清受限数据，返回可访问页 |
| 403 `PUBLISH_PERMISSION_REQUIRED` | “当前角色不能公开家庭资料” | 保存私人草稿，不提交审核 |
| 404 `RESOURCE_NOT_FOUND` | “内容不可用” | 不暴露其他家庭是否有该资源 |
| 409/412 `REVISION_CONFLICT` | 变更对照/重新预览 | 不自动覆盖 |
| 409 `TASK_ALREADY_COMPLETED` | 已由某人完成+时间 | 看已有记录，不能重复完成 |
| 409 `IDEMPOTENCY_PAYLOAD_MISMATCH` | “提交内容已变化” | 核对原操作状态后显式新操作 |
| 409 `PLAN_OVERLAP` | 冲突日期/计划 | 返回编辑，不产生双计划 |
| 409 `EPISODE_DUPLICATE` | 已有同周期观察 | 打开更新/追评 |
| 410 `PUBLICATION_WITHDRAWN` | “内容已撤回” | 清正文/媒体，返回列表 |
| 410 `SHARE_EXPIRED` | “分享已失效” | 不显示敏感标题，联系分享者由用户自行决定 |
| 422 `UNIT_NOT_CONVERTIBLE` | 缺少每份克重/密度 | 可按原单位记录，禁止伪换算 |
| 422 `PREVIEW_STALE` | “资料已变化，请再核对” | 刷新预览、保留编辑 |
| 422 `MEDICAL_REVIEW_REQUIRED` | “需要专业确认” | 保存资料，不自动算建议量 |
| 422 `KNOWN_CONTRAINDICATION` | 具体已知冲突 | 禁自动套用，允许纠错/收藏资料 |
| 422 `MEDIA_NOT_READY` | “图片仍在处理” | 保留草稿，完成后再提交 |
| 428 `REVISION_REQUIRED` | “请刷新后继续” | 重取revision，不盲提交 |
| 429 `RATE_LIMITED` | 显示可重试时间 | 按Retry-After，禁止无限重试 |
| 413 `FILE_TOO_LARGE` | 文件名及大小限制 | 替换/压缩/移除该文件 |
| 422 `STORAGE_QUOTA_EXCEEDED` | 超出空间及所需空间 | 清理/选更少/权益入口，其他功能可用 |
| 503 `DEPENDENCY_UNAVAILABLE` | 某区块暂不可用 | 局部重试、手工回退 |
| 客户端 `RESULT_UNKNOWN` | “结果待核实” | 查询operation，不生成新的重复动作 |

## 8. 服务端不可省略的实现约束

1. API聚合只为减少移动端请求，不能绕过各领域权限；页面中出现的canEdit不是授权凭据。
2. 时间表和食物量用确定性计算服务。AI负责解释/草稿，不计算权威聚合或决定营养安全；规则由有来源的配置版本和专业审核流程管理。
3. 发布、计划、任务完成、记录删除与Outbox落同事务；Worker重试用业务唯一键。通知发失败不回滚已完成喂食事实，任务照常在站内可见。
4. 公共食品更正先创建提案，审核合并时不重写已引用历史版本；任何合并须保留旧ID重定向/来源映射。
5. 病历和私人媒体不进入公共推荐索引；公共数据索引只接收白名单快照。标签/照片识别结果是建议，不自动认定宠物身份。
6. 社区审核在发布前后均可工作；恶意刷评和去重不得以隐私敏感数据无边界收集实现。记录关联不是医疗效果验证。
7. 小规模先查询实际关系表/必要索引；定期汇总统计、全文/向量检索和缓存按观测瓶颈增加。数据少时不以复杂机器学习填补可信资料缺口。
8. 性能预算为初始验收目标：关键读取p95约1秒内、普通写入p95约1.5秒内、长任务受理约1秒内；必须在明确网络/样本/设备条件下压测，不能在设计稿宣称已经达到。

## 9. 与页面绑定的契约验收

### 9.1 核心UI内容与字段映射

| UI内容 | 服务端字段/来源 | 缺失或失败时 |
|---|---|---|
| 当前宠物、年龄与估计标记 | Pet.name、bornOn/estimatedAgeRange、ageSource、asOfDate | 显示年龄未知，不假设成年 |
| 食品名称、地区、配方版本 | FoodProduct + FoodVersion + marketRegion | 来源不明显式标记，不把SKU净重当配方差异 |
| 食品用途与适用阶段 | nutritionPurpose、lifeStageRaw、mappingSource | UNKNOWN→待核对，不推断全价 |
| 能量大字与来源徽标 | energy.value/unit/method、verification、source.checkedAt | null→未提供；OCR草稿→待核对 |
| 保证分析表格 | nutrients[].key/value/unit/comparator/basis | 保留比较符；不支持单位不可比 |
| 搭配合计 | summary.knownKcal/totalKcal/energyCompleteness | 总值null，展示已知部分与缺项 |
| 计划预览 | diff、effectiveAt、affectedInstances、conflicts、previewToken | 冲突→重新预览；不能静默启用 |
| 已由谁投喂 | TaskInstance.doneBy/doneAt/linkedEventId | 未同步→待核实；不是无人喂过的证据 |
| 实际摄入 | FeedingEvent.consumed/consumptionStatus/method | UNOBSERVED→未知；不复用offered |
| 相似理由 | similarity.matched/different/missingDimensions、coverage | 不足→部分条件相近；displayScore=null |
| 基础检查与按钮能力 | suitability.status/reasons/allowedActions/blockedActions | 算法失败→暂不可用，保留资料查看 |
| 观察使用期与年龄 | observationPeriod、PublicPetSnapshot的观察时背景 | 不取当前私人宠物档案冒充旧背景 |
| 记录关联徽标 | evidence.type/daysWithRecords/periodDays/fullDayIntakeConfirmed | 源证据撤回→失效或重审，不显示已验证效果 |
| 结构化点评分布 | dimensions[].observedCount/unobservedCount、distribution | 小样本→null+样本较少，不展示0% |
| 我的赞/收藏 | likedByMe/favoritedByMe，独立关系接口 | 乐观操作失败回滚，不能只改本机计数 |
| 发布状态与下一动作 | publicationVersion.status、moderation.reason/action | pending不是published；退回指出具体字段 |
| 图片/病历可访问性 | MediaAsset.status、capabilities、短效access返回 | 禁止沿用永久URL；403清理，处理中可重试 |
| 图表横轴和缺口 | happenedAt/localDate、timezone、series[].value/quality | 无记录null、断点，不插0或AI补值 |
| AI确认卡 | draftId/hash、unresolvedFields、expiresAt、confirmationChallenge | 过期重新核对，未确认不写库 |
| 报告快照标签 | report.snapshotAt/templateVersion/sourceRevisions | 旧快照提示，不擅自更新已导出报告 |
| 页面动作可见性 | capabilities + 当前服务端授权 | UI用于解释；操作时后端仍须拒绝越权 |

### 9.2 实施验收范围

重点自动化验证：跨家庭/撤权的读写与媒体访问；任务并发唯一；幂等键复用；计划版本切换；所有单位与null统计；公开DTO字段白名单；撤回后全文索引/缓存仍不返回正文；重复观察去重；低样本抑制；个性化关闭；SSE断线不能重提commit；删除任务重试不遗漏派生资源。设计验收使用主文档UX-01至UX-48，服务端测试不以视觉原型通过替代。

接口仍需实施前确定的配置项：账号验证渠道/政策、媒体配额/长度、审核服务及工作时间、数据保留期、实际食品目录来源与许可、营养规则评审责任、付费方案。页面均已设计其未知/未开通/不可用状态，不能让这些待定项阻断私人记录闭环。


