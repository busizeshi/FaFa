# FaFa 项目 Codex 开发交接文档

> **交接对象**：Codex（或任何接续开发的 AI / 工程师）
> **交接时间**：2026-08-23
> **当前进度**：Phase 0～5 核心代码已实现；Phase 4/5/6 待真实中间件与数据端到端联调；**Phase 7 Java/Python 核心代码、V7 迁移、食品与饮食小程序最小入口已实现，进入联调验收**。
> **必读文档（按优先级）**：
> 1. [.agent/development-guidelines.md](../.agent/development-guidelines.md) — 开发规范（DDD 分层、命名、异常、日志、注释、测试）
> 2. [docs/dev/phase4-implementation-plan.md](./dev/phase4-implementation-plan.md) — Phase 4 实施文档（当前开发依据）
> 3. [docs/技术选型与项目框架搭建.md](./技术选型与项目框架搭建.md) — 技术架构基线
> 4. [docs/服务器与中间件部署信息.md](./服务器与中间件部署信息.md) — 开发环境中间件连接信息与账密（**含敏感信息，不得提交公开仓库**）
> 5. [docs/MySQL数据表记录.md](./MySQL数据表记录.md) — 全部数据表建表语句与索引说明

---

## 一、项目总览

### 1.1 产品定位

FaFa 是一个**宠物生命周期管理微信小程序**。核心主张：「记录它的一生，也让 AI 记住它的一生」。

- 长期记录宠物数据（每日打卡、体重、喂食、提醒、照片）→ 建立宠物数字档案 → AI 理解这些数据 → 帮助主人查询、分析、守护、回忆。
- 三条核心原则：① 宠物是核心，不是社交；② AI 是贯穿所有页面的智能层，不是孤立聊天页；③ 记录成本决定产品生死（每日记录 ≤2 次点击）。
- 产品红线（**不做**）：社区/社交、商城、在线问诊/AI 诊断/处方建议（医疗合规红线）、IoT、宠物交易等。详见 [docs/分阶段开发计划.md](./分阶段开发计划.md) 1.3。

### 1.2 仓库布局

```
D:\dev\Java\FaFa\
├── .agent/
│   └── development-guidelines.md   # 开发规范（Codex 必须先读）
├── docs/                           # 产品与技术文档（详见 3.4）
├── fafa-java/                      # Java 业务服务（Spring Boot 3.5，端口 8080）
├── fafa-python/                    # Python AI 服务（FastAPI，端口 8000，仅内网）
├── fafa-miniapp/                   # 微信小程序前端
└── README.md                       # 目前为空，可后续补充
```

### 1.3 总体架构

单体 + 双服务（Java 业务 / Python AI）+ 六个中间件，架构图见 [技术选型与项目框架搭建.md](./技术选型与项目框架搭建.md) 2.1：

```
微信小程序 ──HTTPS──> fafa-java(:8080) ──SQL──> MySQL 8
                          │  ├─> Redis 7（Sa-Token 会话 / 配额计数 / 锁）
                          │  ├─> MinIO/OSS（对象存储，生产切 OSS）
                          │  ├─> RocketMQ（异步 AI 任务投递，生产）
                          │  └─> HTTP 内网（同步 AI，轻负载 + Windows 开发环境）
                          ▼
                  fafa-python(:8000) ──> 阿里云百炼 Qwen 系列模型
                          ├─> Qdrant（向量库：fafa_media / fafa_pet_profiles）
                          └─> HTTP 回调 ──> fafa-java /api/callback/**
```

关键边界：
- 小程序**只与 fafa-java 通信**；fafa-python 不对外暴露（8000 端口任何环境不得暴露公网）。
- 重 AI 负载（照片/视频向量化、宠物三视图匹配）走 RocketMQ 异步；轻 AI 负载（语义检索、对话）走同步 HTTP。
- 服务间认证：内网隔离 + `X-Internal-Token` 静态令牌（Java→Python、Python→Java 双向携带）。

---

## 二、技术栈与版本

### 2.1 fafa-java（见 [pom.xml](../fafa-java/pom.xml)）

| 技术 | 版本 |
|------|------|
| Java | 21（LTS） |
| Spring Boot | 3.5.5 |
| MyBatis-Plus | 3.5.9（含 mybatis-plus-jsqlparser 分页依赖） |
| Druid | 1.2.23 |
| Sa-Token | 1.39.0（redis-jackson 会话存 Redis） |
| Redisson | 3.36.0 |
| Hutool | 5.8.34 |
| Knife4j | 4.5.0 + springdoc 2.8.9（显式声明覆盖旧版） |
| RocketMQ | rocketmq-spring-boot-starter 2.3.1 |
| MinIO SDK / 阿里云 OSS SDK | 8.5.12 / 3.18.1 |
| Flyway | Spring Boot 内置（flyway-core + flyway-mysql） |
| Micrometer + Actuator | 内置（Prometheus / 健康检查） |

### 2.2 fafa-python（见 [requirements.txt](../fafa-python/requirements.txt)）

FastAPI + uvicorn、pydantic-settings、loguru、qdrant-client、httpx（openai/langchain 按需）。模型统一走阿里云百炼 DashScope OpenAI 兼容入口 `https://dashscope.aliyuncs.com/compatible-mode/v1`。

### 2.3 AI 模型分工（阿里云百炼 Qwen 系列）

| 用途 | 模型 |
|------|------|
| 轻量对话/意图/标签 | qwen-flash、qwen-turbo |
| 复杂推理/Agent/报告 | qwen-plus |
| 图像理解（照片描述） | qwen-vl-plus |
| 多模态嵌入（照片/三视图） | qwen3-vl-embedding（1024 维） |
| 文本嵌入 | text-embedding-v4（纯文本场景）；照片语义检索使用 qwen3-vl-embedding 融合向量 |

模型名统一配置在 Python [config.py](../fafa-python/app/core/config.py)（`model_*` 字段），业务代码只声明「用途」，不直接写模型名。

### 2.4 中间件（开发环境在 `192.168.1.14`，Docker Compose 部署）

| 中间件 | 端口 | 用途 |
|--------|------|------|
| MySQL 8 | 3306 | 业务数据唯一事实来源，库 `fafa` |
| Redis 7 | 6379 | 会话、配额计数、缓存、锁 |
| MinIO | 9000/9001 | 对象存储（开发/测试），单桶 `fafa-media` 公共读 |
| Qdrant | 6333/6334 | 向量库（fafa_media / fafa_pet_profiles） |
| RocketMQ 4.9.4 | 9876/10911 | Java→Python 异步 AI 任务（Dashboard 占用 8080） |

> 具体地址/账密/修复记录见 [docs/服务器与中间件部署信息.md](./服务器与中间件部署信息.md)（**含敏感信息，切勿外泄**）。

---

## 三、项目组成（目录导览）

### 3.1 fafa-java — DDD 四层分包

```
fafa-java/src/main/java/com/fafa/
├── FaFaApplication.java
├── interfaces/                # 接口层：REST 控制器 + 请求/响应 DTO + assembler
│   ├── controller/            # Auth / User / Pet / DailyRecord / FeedingRecord /
│   │                          #   WeightRecord / Reminder / EventRecord / Photo / Tag /
│   │                          #   Quota / Callback / Ping
│   ├── dto/                   # 各业务子包：auth/ pet/ record/ reminder/ media/ tag/ callback/
│   ├── assembler/             # DTO <-> 领域对象转换
│   └── web/GlobalExceptionHandler.java
├── application/               # 应用层：用例编排（事务边界）
│   ├── service/               # AuthAppService / UserApplicationService /
│   │                          #   PetApplicationService / DailyRecordApplicationService /
│   │                          #   FeedingRecordApplicationService / WeightRecordApplicationService /
│   │                          #   ReminderApplicationService / ReminderPushService /
│   │                          #   EventRecordApplicationService / MediaApplicationService /
│   │                          #   TagApplicationService / QuotaApplicationService
│   ├── dto/                   # Command/Result（login/ user/ pet/ record/ reminder/ event/ media/ tag/ quota/）
│   └── subscriber/            # 领域事件订阅（预留）
├── domain/                    # 领域层：业务规则核心（不依赖任何外层/框架）
│   ├── model/                 # user/ pet/ record/ reminder/ event/ media/ tag/ 子包实体与枚举
│   ├── repository/            # 仓储接口（只定义，不实现）
│   ├── service/               # 领域服务（ReminderScheduleService / SpeciesRecordPolicy）
│   └── event/                 # 领域事件（预留）
└── infrastructure/            # 基础设施层：实现 domain 仓储接口
    ├── persistence/           # po/ mapper/ repository/ + BaseDO/SoftDeleteDO/JsonListConverter/MyMetaObjectHandler
    ├── oss/                   # StorageService 接口 + MinioStorageService / OssStorageService
    ├── ai/                    # AiTaskDispatcher（Http + RocketMQ 双实现）+ SemanticSearchGateway
    ├── mq/                    # AiTaskProducer / PhotoAnalysisMessage
    ├── client/                # PythonAiClient / PhotoAnalyzeRequest
    ├── wechat/                # WeChatClient（code2Session）
    ├── schedule/              # ReminderPushScheduler（提醒推送定时任务）
    └── config/                # FaFaProperties / SaTokenConfig / MybatisPlusConfig /
                               #   RestClientConfig / InternalTokenInterceptor / MinioConfig
```

**依赖方向**：interfaces → application → domain ← infrastructure。domain 层不依赖外部技术。

### 3.2 fafa-python

```
fafa-python/
├── main.py                    # FastAPI 入口
├── requirements.txt
├── .env.example               # 环境变量模板（复制为 .env 填真实值）
├── app/
│   ├── api/                   # health / chat / pets / photos / vector / record
│   ├── service/               # photo_service（已实现）/ callback_client / pet_vector_service /
│   │                          #   chat_service / report_service
│   ├── agent/tools/           # AI Agent 工具（预留）
│   ├── core/                  # config.py（pydantic-settings）/ llm.py / qdrant.py / logging.py
│   ├── consumer/              # photo_consumer（RocketMQ 消费者，仅 Linux 启用）
│   └── schemas/               # Pydantic 模型（snake_case）
└── scripts/check_dashscope.py # 百炼连通性自检脚本
```

> `photos.py`、`vector.py`、`photo_service.py` 已实现 Phase 4 素材分析/语义检索/向量删除链路；仍需连接真实 MinIO、Qdrant、百炼环境完成端到端验收。

### 3.3 fafa-miniapp（微信小程序）

- `app.js` / `app.json` / `app.wxss` + `pages/`（index 首页、login 登录页）
- `utils/config.js`（后端 baseUrl）+ `utils/request.js`（统一请求封装）
- 用于联调后端服务；正式页面随各 Phase 前端开发推进。

### 3.4 docs — 文档清单

| 文档 | 用途 |
|------|------|
| `FaFa产品功能设计-最终版.md` | 产品功能设计（上游需求来源） |
| `分阶段开发计划.md` | 分阶段开发计划（Phase 0-10；Phase 4 只有概要行，详细见 dev/phase4） |
| `技术选型与项目框架搭建.md` | 技术架构基线、服务间契约、日志/可观测性 |
| `服务器与中间件部署信息.md` | 开发环境中间件连接信息（含账密，勿外泄） |
| `MySQL数据表记录.md` | 全部数据表建表语句与索引（**改表必须同步此文档**） |
| `dev/phase1~phase3-implementation-plan.md` | 各阶段实施文档（已完成） |
| `dev/phase4-implementation-plan.md` | Phase 4 实施文档 |
| `dev/phase5-implementation-plan.md` | Phase 5 AI 对话实施文档 |
| `dev/phase6-implementation-plan.md` | **Phase 6 医疗健康实施文档（当前开发依据）** |
| `市场需求调研与功能拓展分析.md` | 需求调研 |

---

## 四、开发规范（接续者必须遵守）

完整规范见 [.agent/development-guidelines.md](../.agent/development-guidelines.md)，核心要点：

### 4.1 DDD 分层

- 业务规则放 **domain 层**（实体工厂方法 `create`/`restore`、状态机、不变式校验）；应用层只做编排与事务。
- **Repository 接口定义在 domain，实现在 infrastructure**（`XxxRepositoryImpl`，负责 PO↔领域对象转换）。
- 控制器只做参数校验与协议转换，不写业务逻辑、不直接访问 Mapper、不写 try-catch。

### 4.2 统一响应与错误码

- 所有接口返回 `Result<T>`（code/message/data，见 [Result.java](../fafa-java/src/main/java/com/fafa/common/Result.java)），全局异常处理器统一捕获。
- 业务异常抛 `BusinessException(ErrorCode.XXX)`，错误码按模块分段：
  - `10xxx` 用户系统 / `20xxx` 宠物 / `30xxx` 日常记录 / `40xxx` 素材与成长档案 / `50xxx` 提醒系统 / `60xxx` 预留
- 新增错误码必须登记到 [ErrorCode.java](../fafa-java/src/main/java/com/fafa/common/ErrorCode.java)。

### 4.3 关键约定

- **命名**：类大驼峰、方法小驼峰、常量全大写下划线、包小写按层级组织。
- **日志**：@Slf4j；INFO 关键操作、WARN 业务异常、ERROR 系统异常（含堆栈）；禁止无意义日志与 System.out。
- **注释**：类注释说明职责（`@author FaFa Team` + `@since x.0`）、复杂业务规则必须注释、公共 API 有 Javadoc。
- **事务**：跨仓储写操作加 `@Transactional`；上传等「外部副作用 + 事务」场景用 `registerAfterCommit(...)` 在提交后再发异步任务。
- **不搞过度设计**：简单够用即可，不提前抽象。

### 4.4 环境差异策略（硬规则）

- 环境差异只允许通过 **配置（profile / 环境变量）+ 条件注册** 切换，**禁止在业务代码里写 `if (env == dev)`**。
- 已落地：对象存储 `StorageService` 策略（MinIO/OSS，`fafa.storage.type` 条件注册）；AI 任务投递 `AiTaskDispatcher` 策略链（MQ 优先、HTTP 兜底）。

### 4.5 数据库与迁移

- 库 `fafa`，utf8mb4；表/字段 snake_case；每表含 `id`/`created_at`/`updated_at`，需软删除的表加 `is_deleted`。
- Flyway 迁移脚本在 `fafa-java/src/main/resources/db/migration/`：现有 `V1.1`（用户）、`V2`（日常记录）、`V3`（提醒）、`V4`（素材，Phase 4）。
- 手写 XML SQL 必须显式带 `is_deleted = 0`（MP 不拦截自定义 SQL）。
- **改表必须同步** `docs/MySQL数据表记录.md`。

---

## 五、开发与运行操作

### 5.1 环境准备

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 本机已装 |
| Maven | 3.8+ | 构建 fafa-java |
| Python | 3.11+ | 运行 fafa-python，`pip install -r fafa-python/requirements.txt` |
| 中间件 | 见 2.4 | 已部署于 192.168.1.14（Docker），无需本机安装 |
| 微信开发者工具 | — | 联调小程序 |

### 5.2 配置注入（敏感信息不进代码库）

- **Java**：`application-local.yml`（gitignore）或环境变量 `WECHAT_APPID` / `WECHAT_SECRET` / `DASHSCOPE_API_KEY` / `INTERNAL_TOKEN`。当前 `application-dev.yml` 内已含微信与中间件开发默认值。
- **Python**：复制 `.env.example` 为 `.env`，填入 `DASHSCOPE_API_KEY`、`INTERNAL_TOKEN`、`JAVA_BASE_URL` 等。

### 5.3 启动 fafa-java

```bash
cd fafa-java
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- 端口 8080；健康检查 `GET /actuator/health`；接口文档 Knife4j：`http://localhost:8080/doc.html`（dev/test 开启，prod 关闭）。
- 启动时 Flyway 自动执行迁移脚本。
- Profile：`dev`（本地）/ `test` / `prod`（生产，切 OSS、关 Knife4j）。

### 5.4 启动 fafa-python

```bash
cd fafa-python
uvicorn main:app --host 0.0.0.0 --port 8000
```

- 健康检查 `GET /health`；`scripts/check_dashscope.py` 可自检百炼连通。
- RocketMQ 消费者（仅 Linux 生产）：`python -m app.consumer.photo_consumer`，Windows 开发走 HTTP 直推链路。

### 5.5 编译与测试

```bash
cd fafa-java
mvn compile      # 编译
mvn test         # 全量单元测试（Phase 3 完成时 95 个用例全绿）
```

- 测试集中在 `fafa-java/src/test/java/com/fafa/domain/`（领域层单元测试，P0-P3 已有：User/Pet/DailyStatus/FeedingRecord/WeightRecord/Reminder 等）。

### 5.6 联调验证

- 微信小程序登录：先在 `fafa-miniapp` 中配好后端地址，用微信开发者工具打开，走登录 → 各功能页。
- 接口调试：Knife4j `/doc.html` 登录后带 token 调用。
- AI 链路：需 fafa-java 与 fafa-python 同时运行，中间件就绪。

---

## 六、阶段进度

| Phase | 主题 | 状态 | 说明 |
|-------|------|------|------|
| 0 | 用户系统 | ✅ 完成 | 微信登录、用户资料、会员框架；表 user / user_quota_usage |
| 1 | 宠物档案 | ✅ 完成 | 宠物 CRUD、排序、三视图向量化、数字主页；表 pet |
| 2 | 日常记录核心 | ✅ 完成 | 每日打卡（六维度）、体重、喂食；表 daily_status / feeding_record / weight_record |
| 3 | 提醒系统 | ✅ 完成 | 提醒 CRUD、完成闭环、今日提醒、定时推送；表 reminder / reminder_completion_history / event_record；95 测试全绿 |
| **4** | **素材库与 AI 摄入** | 🚧 **代码完成，待联调** | 照片/视频上传、tags、向量化、语义搜索、存储/AI 配额已实现；待真实百炼/Python 进程完成端到端验收 |
| 5 | AI 对话 Agent | 🚧 **核心代码完成，待联调** | 会话持久化、真实数据问答、照片/视频检索、自然语言记录草稿 |
| **6** | **健康管理** | 🚧 **核心代码完成，待联调** | V6 医疗表、就医档案、用药计划/提醒闭环、规则预警、医疗摘要、小程序健康首页 |
| **7** | **食品与饮食** | 🚧 **核心代码完成，待联调** | V7 食品卡、包装识别/配额、提示分、换粮、喂食计划、动态统计、Agent 只读工具和小程序最小入口 |
| **8** | **养宠记账** | 🚧 **核心代码完成，待联调** | V8 开支表、手工记账 CRUD、月/年统计、医疗费用自动同步、Agent 只读查询和小程序记账页 |
| **9** | **报告与创作** | 🚧 **核心代码完成，待联调** | V9 周/月/季/年报告、事实快照与 AI 表达层、分享笔记/小红书草稿、周期草稿调度、Agent 只读查询和小程序报告入口 |
| 10 | 家庭协同与上线 | ⏳ 未开始 | 多人共管、临时授权、上线打磨 |

---

## 七、Phase 4 交接清单（Codex 下一步重点）

Phase 4 实施文档：[docs/dev/phase4-implementation-plan.md](./dev/phase4-implementation-plan.md)（14 节，含接口契约、配额机制、Python 改造清单、任务执行顺序、验收标准）。

### 7.1 Phase 4 已完成部分（Java + Python）

1. ✅ ErrorCode 新增 `40xxx` 段（MEDIA_NOT_FOUND / FILE_TOO_LARGE / STORAGE_QUOTA_EXCEEDED / AI_SEARCH_QUOTA_EXCEEDED / TAG_* / EVENT_RECORD_NOT_MODIFIABLE 等）
2. ✅ 领域枚举：`MediaType`（photo/video）、`MediaAnalysisStatus`（pending/processing/completed/failed/skipped）、`TagCategory`（pet/location/action/object/mood/other）
3. ✅ 领域实体：`MediaAsset`（状态机 + 归属确认）、`UserTag`（create/restore/touch）；`EventRecord` 扩展 `createManual`/`updateManual`
4. ✅ 仓储接口 + 实现：`MediaAssetRepository`、`UserTagRepository`（含 `upsertAndTouch` 原子打标 SQL）、`UserQuotaUsageRepository`（原子增量/非负扣减）
5. ✅ `QuotaApplicationService`：存储配额（user_quota_usage 原子增量、超限拒绝）+ AI 检索月配额（Redis 计数、VIP 不限）
6. ✅ `AiTaskDispatcher` 扩展 `dispatchMediaAnalysis`/`deleteMediaVector`（Http + RocketMQ 双实现）+ `SemanticSearchGateway`
7. ✅ `MediaApplicationService`：上传（OSS 先行→事务落库→AFTER_COMMIT 分发分析）/ 检索 / 语义检索 / 编辑 / 确认归属 / 删除四步链路 / 回调处理
8. ✅ `TagApplicationService` + 接口层控制器（PhotoController / TagController / QuotaController）+ 请求 DTO（Upload/Update/ConfirmPet/SemanticSearch/CreateTag）；springdoc 分组已含 photo/tag/event-record/quota
9. ✅ 数据库：`user_tag` 启用、`media_asset` 建表（MCP 执行 + Flyway V4 + MySQL数据表记录.md 已同步）
10. ✅ 回调接线：`CallbackController` 校验内部令牌后调用 `MediaApplicationService.handleAnalysisCallback(...)`，成功/失败回调均幂等处理
11. ✅ 手动事件 CRUD：`EventRecordApplicationService` / `EventRecordController` 支持 manual 事件创建、编辑、删除，提醒事件只读；tags 联动热度字典
12. ✅ 宠物集成：创建宠物自动生成宠物名标签；删除宠物清空素材归属但保留素材
13. ✅ Python 摄入链路：照片/视频 qwen3-vl-embedding、Qdrant `fafa_media` upsert、三视图相似度识别（多宠物全量命中）、Java 回调重试、语义检索、向量幂等删除、RocketMQ 订阅
14. ✅ Phase 4 领域测试：Java 测试总数 107 个全绿；Python 模块语法检查通过

### 7.2 Phase 4 待办（按实施文档第 14 节顺序）

| # | 任务 | 说明 |
|---|------|------|
| 1 | ✅ 接口层收尾 | 回调已接线并受 `X-Internal-Token` 保护；`mvn compile` 已通过 |
| 2 | ✅ `EventRecordApplicationService` 扩展 | 手动事件 CRUD、manual-only 保护、tags 联动已完成 |
| 3 | ✅ `PetApplicationService` 集成 | 宠物名标签与素材归属清理已完成 |
| 4 | ✅ Python 侧实现 | `photo_service`、`photos.py`、`vector.py`、`callback_client`、RocketMQ 订阅已完成 |
| 5 | ✅ 领域层单元测试 | Phase 4 测试已补齐，Java 全量 107 个用例全绿 |
| 6 | ✅ `mvn compile` + `mvn test` 全量验证 | 编译通过，测试全绿 |
| 7 | 🚧 联调验证 + 文档收尾 | 需在真实 MinIO/Qdrant/百炼环境完成上传→分析→回调→语义检索验收；验收通过后将实施文档状态改为「已完成」 |

### 7.3 Phase 4 关键设计决策（接续者先理解再动手）

- **素材归属**：`media_asset.tags` 是宠物归属业务事实，支持同一媒体追加多个宠物名称；单宠物兼容保留 `pet_id`。多宠物识别不进入待确认流程，全部达到阈值的宠物都写入 tags；宠物改名通过异步任务迁移媒体 tags。
- **AI 分析状态机**：`pending → processing → completed/failed`，由回调驱动，Java 不维护中间态；重复回调幂等（已终态直接跳过）。照片和视频均进入 AI 分析，视频向量化要求百炼可访问的对象存储 URL。
- **删除四步链路**：先删库（逻辑删除）→ 释放存储配额 → 删对象存储文件（失败记日志不阻断）→ 删 Qdrant 向量（HTTP 直调幂等）。
- **配额机制**：存储走 `user_quota_usage` 原子 SQL（`increaseWithinLimit` 带 `used + delta <= limit` 条件防超发）；AI 检索走 Redis `INCR` 计数 + 月 TTL，第 N+1 次超限回滚并抛错；失败请求不占额度（`tryAcquire` 成功后才放行、失败 `release`）。配置在 [FaFaProperties.java](../fafa-java/src/main/java/com/fafa/infrastructure/config/FaFaProperties.java) 的 `fafa.media` / `fafa.quota`（普通用户存储 2GB、AI 检索 10 次/月、VIP 不限、单批 20 个、图片 10MB/视频 50MB）。
- **标签语义**：素材/事件上的 `tags` 是**快照**（JSON 列），标签定义删除不影响历史数据；`user_tag` 表是热度字典，`upsertAndTouch` 原子打标（usage_count+1、last_used_at 更新）。
- **JSON 列映射**：见 [JsonListConverter.java](../fafa-java/src/main/java/com/fafa/infrastructure/persistence/repository/JsonListConverter.java)；检索 tags 用 MySQL `JSON_OVERLAPS`。

### 7.4 服务间契约（Java ↔ Python，务必两侧逐字节一致）

| 方向 | 接口 | 用途 |
|------|------|------|
| Java→Python | `POST /api/photos/analyze` | 媒体理解与向量化（返回 202，后台执行） |
| Java→Python | `POST /api/photos/search` | 语义检索（query + petId? + 时间范围 + limit） |
| Java→Python | `DELETE /api/vector/{embedding_id}` | 删除素材向量（point = uuid5("media_{id}")） |
| 小程序→Java | `POST /api/photos/{id}/reanalyze` | 重置历史素材状态并重新投递照片/视频 AI 分析 |
| Python→Java | `POST /api/callback/photo-analysis` | 回写结果（media_id/success/recognized_pet_ids/recognition_score/error_message） |

- 消息体字段 camelCase（Java 序列化）；Python 消费端做 camelCase→snake_case 映射。
- `/api/photos/analyze` 请求字段包含 `user_id/photo_id/url/file_url/media_type/tags/pet_id/candidate_pet_ids/taken_at/message_id`；`url` 可为视频临时预签名地址，`file_url` 是写入 Qdrant 的 OSS 永久引用，`taken_at` 用于时间范围过滤。
- 回调/请求均携带 `X-Internal-Token`。
- MQ Topic：`fafa_photo_analysis_topic`（tag=photo，消息含 messageId 幂等键）；**既有 `fafa_pet_photo_vectorization` topic 不动**（三视图链路已稳定）。

### 7.5 关键验收标准（Phase 4，来自实施文档第 15 节）

- 照片/视频上传 → Qdrant `fafa_media` 可查 point → 回调后 MySQL 状态/最高匹配分数/宠物名称 tags 更新
- 三视图相似度达到配置阈值时全部命中宠物都会追加名称 tags，不保留待确认项
- 语义检索：自然语言 + 宠物/时间过滤叠加；普通用户第 11 次被拒、VIP 不限
- 存储配额：普通用户超 2GB 上传被拒；删除后配额恢复
- 视频上传 → qwen3-vl-embedding 向量化 → 回调 Java → 语义检索正常
- 宠物创建自动生成宠物名标签；同一用户宠物名称唯一；改名异步迁移媒体标签；删除后保留历史 tags 快照、素材保留
- 手动事件创建/编辑/删除 + tags 联动；提醒生成事件不可改

### 7.6 Phase 6 已实现内容与联调重点

1. ✅ Java 新增 `V6__create_medical_health_tables.sql`，包含就医、附件、用药计划、用药记录、健康预警五张表。
2. ✅ 就医档案 CRUD、已有素材关联、医疗时间线；时间线合并手动事件、疫苗/驱虫/体检完成历史和实际用药。
3. ✅ 用药计划领域状态机；固定频次自动创建 `ReminderType.MEDICATION`，按需用药不创建周期提醒；提醒完成在同一事务幂等写入 `medication_record`。
4. ✅ 六条纯规则健康预警、证据快照、指纹去重、确认/解决、定时扫描和红色优先排序。
5. ✅ Java → Python `/api/medical/summaries` 与 Python → Java 医疗事实查询接口；无模型 key 时返回确定性事实摘要，AI 失败不生成诊断结论。
6. ✅ 小程序首页增加医疗健康入口，展示预警、就医记录、当前用药和摘要。

Phase 6 联调顺序：执行 V6 → 创建就医档案 → 创建用药计划 → 完成 medication 提醒 → 连续保存异常每日状态 → 评估预警 → 启动 Python 生成摘要 → 小程序页面验证。重点检查 `.env`/`application-dev.yml` 的 `INTERNAL_TOKEN`、Python `JAVA_BASE_URL`、DashScope key 和 MySQL Flyway 权限。

### 7.7 Phase 7 已实现内容与联调重点

1. ✅ Java 新增 `V7__create_food_and_feeding_plan_tables.sql`，创建食品卡、包装识别任务、换粮计划、喂食计划/计划项，并扩展 `feeding_record` 计划关联字段。
2. ✅ 食品卡 CRUD、归档/激活/逻辑删除、营养字段校验、来源媒体/识别任务追溯和 active 计划引用保护。
3. ✅ 包装图片识别：媒体归属校验、普通/VIP 月配额、幂等键、Python 视觉识别、草稿保存、用户确认/拒绝、来源任务唯一约束。
4. ✅ 配方信息提示分、换粮比例校验/反应记录、喂食计划建议/确认/暂停/恢复/完成、今日清单和喂食记录幂等落库。
5. ✅ 动态喂养事实统计、食品卡/计划 Agent 只读接口和小程序食品/今日计划最小入口。

Phase 7 联调顺序：执行 V7 → 手工创建食品卡 → 查询提示分 → 上传包装图片并识别/确认 → 验证第 6 次普通用户识别被拒 → 创建换粮/喂食计划 → 完成今日清单 → 查询动态分析 → 在 Agent 中询问食品卡和计划。详见 [docs/dev/phase7-implementation-plan.md](./dev/phase7-implementation-plan.md) 与 [docs/接口文档.md](./接口文档.md) Phase 7。

### 7.8 Phase 8 已实现内容与联调重点

1. ✅ Java 新增 `V8__create_expense_record_table.sql` 和 `expense_record` 仓储，金额统一使用 `BigDecimal/DECIMAL(12,2)`，分类和来源使用领域枚举。
2. ✅ 手工开支创建、分页查询、详情、更新、逻辑删除、分类字典和月度/年度统计接口；所有资源做用户与宠物归属校验。
3. ✅ 医疗档案创建/更新/删除接入同事务费用同步：同一就医档案只保留一条自动医疗开支，清空费用或删除档案逻辑删除，重新填写费用恢复原记录。
4. ✅ 自动医疗开支不可从记账接口编辑或删除；食品卡价格只作为参考，关联开支保存食品名称快照，不自动记账。
5. ✅ Python Agent 增加 `getExpenseHistory` / `getExpenseStatistics` 只读工具和费用意图路由；小程序增加记账统计和快速录入页面。

Phase 8 联调顺序：执行 V8 → 查询分类并创建手工开支 → 验证跨用户资源拒绝 → 创建带费用的就医档案 → 重复更新/清空/删除并检查自动账单幂等 → 查询月度/年度统计 → 启动 Python 询问费用 → 小程序测试录入和统计刷新。详见 [docs/dev/phase8-implementation-plan.md](./dev/phase8-implementation-plan.md) 与 [docs/接口文档.md](./接口文档.md) Phase 8。

### 7.9 Phase 9 已实现内容与联调重点

1. ✅ Java 新增 `V9__create_report_and_content_tables.sql`，创建 `pet_report`、`content_draft`、`content_schedule` 三张表，周期和草稿幂等约束已落库。
2. ✅ 报告事实快照由 Java 从宠物、每日状态、体重、喂食、事件、提醒、医疗、用药、预警、费用和素材记录组装；Python 只处理快照，不直接查询数据库。
3. ✅ 报告支持周/月/季/年、同周期复用、强制重生成、AI 失败确定性降级、结构化来源字段和逻辑删除。
4. ✅ 内容草稿支持分享笔记/小红书文案、素材顺序、幂等键、用户编辑、重新生成、逻辑删除和 7/15/30 天周期调度；不接入外部发布平台。
5. ✅ 报告和内容生成共用 Redis 月配额，普通用户默认 3 次、VIP 默认 10 次；Agent 仅增加报告只读工具。
6. ✅ 小程序增加报告列表、报告详情、内容草稿最小入口；接口文档和 MySQL 表文档已同步。

Phase 9 联调顺序：执行 V9 → 生成月报/季报并重复请求 → 检查事实快照与降级状态 → 创建和编辑内容草稿 → 验证幂等、跨用户素材和配额 → 配置周期任务 → Agent 查询最近报告 → 小程序页面验证。详见 [docs/dev/phase9-implementation-plan.md](./dev/phase9-implementation-plan.md) 与 [docs/接口文档.md](./接口文档.md) Phase 9。

---

## 八、已知坑与注意事项（历史修复记录，勿重蹈）

1. **MySQL 连接**：`characterEncoding` 必须写 `UTF-8`（Connector/J 自动映射 utf8mb4），写 `utf8mb4` 会连不上；Flyway 需 `fafa` 用户有 `performance_schema.user_variables_by_thread` SELECT 权限。
2. **RocketMQ**：Broker 需在 `broker.conf` 设 `brokerIP1=192.168.1.14`（否则容器内网 IP 注册，远程客户端投递失败）；Dashboard 新版镜像监听容器内 8082，compose 映射应为 `8080:8082`。
3. **RocketMQ Dashboard 占用服务器 8080**：后续若把 fafa-java 部署到该服务器需先停 Dashboard 或改 Java 端口。
4. **虚拟线程**：`spring.threads.virtual.enabled=true` 已启用；避免长时间持有 `synchronized`（钉住载体线程），互斥用 `ReentrantLock`。
5. **MyBatis-Plus 3.5.9 分页**：需单独引入 `mybatis-plus-jsqlparser` 依赖（pom 已加）。
6. **服务间契约纪律**：改任一侧契约必须同步另一侧并更新技术文档 7.3 表格。
7. **文档同步纪律**：改表 → 同步 `MySQL数据表记录.md`；阶段完成 → 更新对应 `dev/phaseX-implementation-plan.md` 头部状态。
8. **敏感信息**：`服务器与中间件部署信息.md` 含全部账密，不得提交公开仓库；密钥一律走环境变量/.env。

### 7.10 Phase 10 协同与应急信息卡

- `pet.user_id` 永远表示宠物空间 owner；当前登录用户表示 operator，不能从请求体信任 `userId`、`operatorId` 或 `role`。
- 成员访问统一经 `PetAccessApplicationService` 判断。owner 拥有完整权限；member 可读取宠物资料、记录、医疗/费用/食品/报告和应急卡，并可创建日常记录；caregiver 只拥有读取基础资料/记录和创建记录的临时权限。
- 成员管理、医疗/费用/食品/报告写入、应急资料编辑和审计查询均为 owner-only。caregiver 到期由权限查询和成员扫描共同失效，不能通过客户端恢复。
- 邀请只保存 token SHA-256 摘要，raw token 仅创建响应返回一次；接受邀请使用数据库行锁和唯一成员关系，不能重复接受。
- V10 新表和 `operator_id` 迁移详见 `V10__create_collaboration_and_emergency_tables.sql`。旧 owner 流程在协同开关关闭时继续可用，生产环境默认关闭后灰度开启。
- 共享提醒按 `reminder + recipient + deliveryWindow` 幂等，失败投递使用 `retry_count` 按配置有限重试；通知适配器异常不得阻塞其他成员。
- 应急卡只展示宠物档案和用户确认资料。首次保存 version=0，更新提交服务端版本；不从 AI 或病历推断未填写字段。

---

## 九、常用命令速查

```bash
# Java 编译 + 测试
cd fafa-java
mvn compile
mvn test

# 启动 Java（dev profile）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 启动 Python
cd fafa-python
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000

# Python 百炼连通自检
cd fafa-python && python scripts/check_dashscope.py
```

---

> 本文档创建：2026-08-23
> 维护：FaFa Team
> 接续者交接时建议先通读：development-guidelines.md → phase4-implementation-plan.md → 技术选型 → 本文档第六章（当前进度）→ 第七章（待办清单）。
