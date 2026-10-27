# FaFa Android 客户端技术栈与项目创建指南

> 版本：v1.0｜编写日期：2026-10-26
> 依据：FaFa_企业级技术架构与服务端开发规范 v1.0、前后端交互开发与模块验收手册 v1.0、接口与数据契约 v2.1、页面与交互设计规格 v2.1、Pen 画布 FaFa.pen（D1–D10 + A00 设计物料库）
> 用途：作为 Android 客户端的技术选型基线、工程创建规范和依赖版本锁定依据。创建仓库与添加依赖前以本文为准；版本号随官方稳定版演进，按第 12 节规则更新。

---

## 1. 决策记录与文档关系

### 1.1 客户端形态决策

| 项 | 结论 |
|---|---|
| 客户端形态 | **原生 Android**（Kotlin + Jetpack Compose），不做 Flutter/React Native 跨端 |
| 首发平台 | Android 单端；iOS 后续评估，复用同一 REST 合约，领域层保留 Kotlin Multiplatform 演进可能 |
| 与交互手册 §5 的关系 | 手册 §5 中"iOS/Android 优先 Flutter 一个客户端"的客户端建议，**由本文更新为原生 Android**；手册其余内容（REST 合约、错误码、队列、验收矩阵）全部不变 |
| 决策理由 | ① 后端团队为 Java 背景，Kotlin 迁移成本最低，可前后端共用语言生态与思维；② 单端首发无需跨端框架，Compose 是 Google 对新 App 的官方主推方向，长期维护风险最低；③ Pen 设计的 soft26 三主题 + 玻璃拟态可用 Material 3 + 自定义 Design Token 精确还原；④ 官方架构指南、Now in Android 参考项目提供成熟模板，AI 辅助编码（含 Android Studio 内置 Agent）对 Compose 的支持最好 |

### 1.2 与服务端基线的对齐

- 客户端只依赖 [接口与数据契约 v2.1](宠物APP_接口与数据契约_v2.1.md) 定义的 REST 合约（`/api/v1`、data/meta 信封、错误码、cursor 分页、revision/ETag、Idempotency-Key、job/operation 状态），不感知 gateway/admin/biz 微服务划分与 Nacos/Dubbo。
- AI 流式对话走服务端 SSE；媒体直传走对象存储预签名 URL；推送走服务端 `notification.send` 消费后的厂商通道（见第 8 节）。
- 客户端不直连 MySQL/Redis/RabbitMQ/Python 服务；Python AI 服务仅由 Java 侧调用。

---

## 2. 技术栈总览（结论速览）

| 类别 | 选型 | 版本基线（2026-10 稳定线） | 说明 |
|---|---|---|---|
| IDE | Android Studio | Rabbit \| 2026.2.x | 内置 JDK（JBR 21）、AI Agent、MCP 集成 |
| 语言 | Kotlin | 2.2.x（或 2.3.x，与 KSP2 配套） | 全项目 Kotlin；不混用 Java 源码 |
| 构建 | Gradle + AGP | Gradle 9.x / AGP 9.x | Kotlin DSL + Version Catalog（`libs.versions.toml`）；AGP 9 内置 Kotlin 支持 |
| UI | Jetpack Compose + Material 3 | Compose 1.11.x 稳定线（1.12 需 compileSdk 37 + AGP 9，可直接采用） | Compose BOM 统一版本；单 Activity |
| 导航 | Navigation 3（androidx.navigation3） | 1.2.0（2026-09 转正） | Google 当前对 Compose 导航的官方推荐 |
| 架构 | 官方推荐架构（UI / 可选 Domain / Data 三层 + UDF） | — | ViewModel + StateFlow + 仓库模式；不引入 MVI 框架库 |
| DI | Hilt | 2.59.x | 编译期校验；配合 KSP2 |
| 网络 | Retrofit 3 + OkHttp 5 + kotlinx.serialization | Retrofit 3.0.x / OkHttp 5.x | SSE 用 `okhttp-sse`；不做双网络栈 |
| 持久化 | Room + DataStore + Paging 3 | Room 2.8.5 / DataStore 1.1.x+ / Paging 3.5.x | Room 存缓存与待同步操作；DataStore 存偏好与会话（Tink 加密）；Paging 3 承接 cursor 分页 |
| 异步 | Kotlin Coroutines + Flow | 随 Kotlin | 结构化并发；长任务用 WorkManager 2.10.x+ |
| 图片 | Coil 3 | 3.x | KMP 架构，Compose 原生集成，Okio 缓存 |
| 相机/选图 | CameraX + 系统 Photo Picker | CameraX 1.5+ | Photo Picker 免存储权限 |
| 图表 | Vico（Compose） | 2.x | 体重趋势、投喂统计等 |
| 安全 | Android Keystore + androidx.biometric + DataStore Tink | — | security-crypto（EncryptedSharedPreferences）已废弃，不得使用 |
| 测试 | JUnit + Turbine + MockK + Robolectric + Compose UI Test + MockWebServer | — | 截图测试用 Roborazzi（可选） |
| 质量 | Lint + ktlint + detekt + R8 + Baseline Profiles | — | CI 强制 lint + test |

---

## 3. 项目创建规范

### 3.1 IDE 与环境

- 安装 **Android Studio Rabbit（2026.2.x 稳定版）**，自带 JBR 21 与 Android SDK 37 平台。
- JDK：编译 toolchain 统一 **17**（与服务端 Java 21 不要求一致；客户端以 AGP 9 兼容矩阵为准，团队统一 17 或 21 均可，锁定其一）。
- 网络环境为大陆时，Gradle/Maven 配置阿里云镜像（`settings.gradle.kts` 中在 `google()`、`mavenCentral()` 之前追加 `maven("https://maven.aliyun.com/repository/google")` 与 `.../public`），CI 机器除外。

### 3.2 新建项目向导选择

| 向导项 | 选择 |
|---|---|
| 模板 | **Empty Activity（Compose）**——默认即 Compose + 版本目录模板 |
| 项目位置 | `d:\dev\Java\FaFa\FaFaAndroid`（与服务端仓库平级，或按团队仓库规划拆独立 Git 仓库） |
| Package name | `com.jwd.fafa`（已于 2026-10-26 确定并落库；applicationId 与 namespace 一致） |
| Build configuration language | **Kotlin DSL**（默认） |
| Minimum SDK | **API 26（Android 8.0）** |
| Build/Target SDK | compileSdk **37**、targetSdk **36** |

### 3.3 SDK 版本策略

| 项 | 值 | 理由 |
|---|---|---|
| minSdk | 26 | java.time 原生可用（无需 desugaring）；通知渠道（API 26）；覆盖大陆主流设备 >95% |
| targetSdk | 36 | Google Play 2026-08 起要求 36；国内商店普遍跟随；Android 16 行为变更（前台服务、照片权限等）需验收 |
| compileSdk | 37 | Compose 1.12+ 与 AGP 9 要求；新控件/新 API 可用 |
| 版本目录 | `libs.versions.toml` | 所有第三方版本集中在目录文件；禁止在模块 `build.gradle.kts` 里写死版本 |

### 3.4 构建变体与环境

```
buildTypes / productFlavors：
  dev      → BASE_URL=https://dev-api.fafa…（applicationIdSuffix=.dev）
  staging  → BASE_URL=https://staging-api.fafa…（applicationIdSuffix=.staging）
  prod     → BASE_URL=https://api.fafa…
```

- 三个环境可并行安装；`dev/staging` 仅 debug 构建，启用 Chucker 网络面板与日志。
- 服务端 baseUrl、对象存储 bucket、推送 appKey 等全部经 `BuildConfig`/`local.properties` 注入，不提交密钥。

### 3.5 Version Catalog 基线示例

```toml
[versions]
agp = "9.0.x"                # 以 Android Studio Rabbit 内置推荐为准
kotlin = "2.2.x"             # 与 KSP2 配套，创建时取最新稳定
ksp = "<配套 Kotlin 的 KSP2 版本>"
composeBom = "2026.xx.xx"    # Compose BOM，创建时取最新
navigation3 = "1.2.0"
lifecycle = "2.11.0"
hilt = "2.59.x"
room = "2.8.5"
paging = "3.5.1"
datastore = "1.1.x"
retrofit = "3.0.x"
okhttp = "5.x"
kotlinxSerialization = "1.9.x"
coil = "3.x"
work = "2.10.x"
camerax = "1.5.x"
vico = "2.x"
biometric = "1.x"

[libraries]
androidx-navigation3-runtime = { group = "androidx.navigation3", name = "navigation3-runtime", version.ref = "navigation3" }
androidx-navigation3-ui = { group = "androidx.navigation3", name = "navigation3-ui", version.ref = "navigation3" }
# …其余按 androidx 官方版本页逐项登记
```

> 上表 `x` 占位项在创建仓库时按 [AndroidX 稳定版页](https://developer.android.com/jetpack/androidx/versions/stable-channel) 当日稳定版锁定，并回填本文或版本目录注释。

---

## 4. 架构设计（官方推荐架构）

### 4.1 分层与依赖方向

按 [Google 官方 App 架构指南](https://developer.android.com/topic/architecture)三层组织：

```
UI 层（feature 模块）
  Compose 界面 + ViewModel（StateFlow<UiState>）+ Navigation 3 路由
      ↓ 依赖
可选 Domain 层（仅当多个 feature 共享业务规则时建 UseCase，如食品能量计算、冲突处理）
      ↓ 依赖
Data 层（core:data）
  Repository 接口与实现；组合 Network + Database + DataStore 数据源
      ↓
core:network（Retrofit/OkHttp/SSE）  core:database（Room）  core:datastore（偏好/凭据）
```

规则：

1. **单向数据流（UDF）**：UI 事件向上交给 ViewModel，状态向下经 `StateFlow` 渲染；Compose 侧 `collectAsStateWithLifecycle()`。
2. **事件不跨层穿透**：UI 不直接调 Retrofit/Room；Data 层不发 UI 事件。
3. **DTO 与领域模型分离**：`core:model` 放纯 Kotlin 领域模型；`core:network` 的 DTO 转换发生在数据源内。未来若上 KMP，`core:model` 与 Domain 可原样复用。
4. **错误与状态**：统一 `Result<T>`/sealed 状态；加载/空/错误/无权限/冲突状态对应手册 §6.4 的错误映射表，由共享的 `ErrorHandler`/UI 状态实现，禁止各页面自行 if-else。

### 4.2 导航

- 单 Activity + **Navigation 3**（`NavDisplay` + 类型安全 backstack）。每个 feature 模块暴露路由对象（如 `AuthRoutes`、`FoodRoutes`），由 `:app` 汇总装配，feature 之间不直接依赖。
- Deep link（分享、推送落地页）在路由层映射到页面参数，权限不足时回落到登录/无权限页。

### 4.3 手册交互约定在客户端的落地

| 手册约定（§6） | 客户端实现 |
|---|---|
| 成功 `data/meta`、失败 `code/message/fieldErrors/requestId/retryable` | `core:network` 统一 `ApiResponse<T>` + 错误解码器，映射为 sealed `UiError` |
| cursor 分页 | Paging 3 `RemoteMediator`（Room 缓存 + 网络追加）；筛选条件变化即废弃旧 cursor |
| 修改带 revision/ETag | 写请求统一携带 `If-Match`；收到 409 弹冲突 UI（显示新版本与冲突字段），保留用户输入 |
| 提交携带 `Idempotency-Key` | 写操作拦截器自动生成并持久化 UUID，随"待核实操作"一起保存，重试复用同一 key |
| 断网/结果未知标"待核实" | Room 内建 `pending_operations` 表（operationId、payload、状态、时间），WorkManager 定时重试并查询原 operation，禁止另发新操作 |
| 长任务 202 + jobId | `JobsRepository` 轮询/订阅 job 状态，UI 显示进度，成功前不显示"完成" |
| 时间 ISO 8601 带偏移、UTC 存储 | `java.time.OffsetDateTime` 解析；Room 存 UTC，展示层按家庭时区/localDate 计算"今日" |

---

## 5. 各域技术选型明细

### 5.1 UI 与设计系统

- **Jetpack Compose + Material 3**；以 [页面与交互设计规格 v2.1] 与 Pen A00 物料库为准，把 soft26-sea/rose/sun 三主题做成三套 `ColorScheme` 令牌（`core:designsystem`），不启用 Material You 动态取色作为默认（品牌色优先，动态色可作设置项）。
- **玻璃拟态（Clear Glass）**：Android 12+ 用 `Modifier.blur`/RenderEffect 与 `Window.setBackgroundBlurRadius` 实现；Android 8–11 降级为半透明纯色/渐变，保证可用性。
- Pen 组件映射：Button/Chip/Status/Segmented Control/TabBar → 自定义 M3 组件（`fafaSoft26` 系列）集中在 `core:designsystem`，页面禁止直接散写样式。
- 字体：系统字体栈（MiSans/HarmonyOS Sans 依设备）+ 中文排版规则（行高、数字等宽用于金额与克数）；图标用 Material Symbols + 自有插画资源（Pen 导出 SVG → `res/drawable`/Vector）。
- 动效：M3 Expressive 运动曲线；页面切换、卡片展开遵循规格文档定义的过渡。

### 5.2 网络层

```kotlin
// core:network 栈：单一 HTTP 栈
Retrofit 3.0（converter-kotlinx-serialization）
  + OkHttp 5（BOM）
  + okhttp-sse           // AI 对话流式（AI-01–03）
  + 拦截器：会话注入、Idempotency-Key、If-Match、User-Agent、requestId 透传
  + Authenticator：401 时静默刷新会话；刷新失败广播登出并清理缓存
```

- DTO 用 `kotlinx.serialization`（`@Serializable`，未知字段宽容解析）；金额用 `Long`（最小货币单位）、重量/克数用 `BigDecimal`（自定义序列化器），禁止 Double。
- AI 流式：`EventSource` + 逐块 JSON 解析；协程取消即断流；支持按会话恢复（对接 AI-02 流式恢复/中断）。
- 调试：debug 构建挂 Chucker 拦截器；联调用 MockWebServer 造 401/409/429/5xx 用例。

### 5.3 本地存储

| 存储 | 用途 |
|---|---|
| Room 2.8.5 | 列表缓存（配合 Paging 3）、草稿、`pending_operations` 待同步队列、消息记录、离线可看的资料镜像 |
| DataStore（Preferences/Proto） | 设置项、通知偏好、主题选择、协议同意版本 |
| DataStore `datastore-tink`（AeadSerializer） | 刷新凭据等敏感值的加密持久化（Keystore 保护密钥；EncryptedSharedPreferences 已废弃，禁止新增使用） |
| 文件 | 媒体原件仅引用系统 URI；导出文件走 `MediaStore Downloads`/SAF |

### 5.4 后台任务与上传

- WorkManager 2.10.x+：
  - 媒体批量上传队列（MEDIA-01–03）：每文件一个独立重试单元，失败单独标记；API 34+ 长时任务声明 `FOREGROUND_SERVICE_DATA_SYNC`。
  - 待同步操作重试、导出结果下载、OCR/报告 job 轮询兜底。
- 直传对象存储：服务端签发短期预签名 URL → 客户端直传 → `MEDIA_COMMIT` 提交 → 轮询 job 状态；客户端不持有长期对象存储凭据。

### 5.5 扫码与 OCR

- 条码识别（FOOD-01）：**ML Kit barcode-scanning bundled 版**（`com.google.mlkit:barcode-scanning`，模型随 APK 打包，不依赖 GMS）；备选 zxing-embedded。
- 食品标签/病历 OCR 由服务端 Python 完成，客户端只负责拍照/上传与人工核对（E2E-07），不在端内做文字识别。

### 5.6 通知与推送（无 GMS 前提）

大陆渠道包不集成 Firebase/FCM。推送体系：

| 环节 | 方案 |
|---|---|
| 通道 | 厂商通道（华为 Push Kit、小米、OPPO、vivo、荣耀）+ 聚合 SDK 二选一：极光 JPush / 个推 / 腾讯 TPNS / 阿里云移动推送（**待定项**，结合备案与隐私合规评估后锁定） |
| 触达 | 服务端 `notification.send` 队列消费后按设备通道下发；App 内仍以站内通知列表为准（推送只是触达手段，不代表执行成功——手册 §6.2） |
| 本地兜底 | WorkManager 周期同步今日任务/提醒（不依赖精确闹钟权限，不申请 `SCHEDULE_EXACT_ALARM`） |
| 权限 | Android 13+ `POST_NOTIFICATIONS` 运行时权限；首次请求放在用户完成引导、可说明价值之后 |
| 渠道 | 按类别建 NotificationChannel（照护提醒/家庭动态/系统），与 NOTIFICATION_PREFS 映射 |

### 5.7 媒体与相机

- 选图：系统 **Photo Picker**（`PickVisualMedia`，免存储权限）为主；低版本/无更新设备降级 `ACTION_OPEN_DOCUMENT`，必要时按最小区间申请 `READ_MEDIA_IMAGES`（Android 13+ 分级权限）。
- 拍摄：CameraX 1.5+（拍照为主，VideoCapture 首版不需要）。
- 展示：Coil 3，列表用缩略图 URL，原图经 `MEDIA_ACCESS` 短期签名 URL 加载并遵守缓存清理（撤权后清理私有缓存）。

---

## 6. 工程模块结构

```text
FaFaAndroid/
├── app/                        # 壳：Activity 宿主、Nav3 装配、Hilt 入口、启动屏
├── core/
│   ├── common/                 # Result/时间/扩展/常量
│   ├── model/                  # 纯 Kotlin 领域模型（KMP 预留）
│   ├── designsystem/           # soft26 三主题令牌、组件、图标、动效（对应 Pen A00）
│   ├── network/                # Retrofit/OkHttp/SSE/错误解码/拦截器
│   ├── database/               # Room schema、缓存表、pending_operations
│   ├── datastore/              # 偏好 + Tink 加密凭据
│   ├── data/                   # Repository 接口与实现（依赖 network/database/datastore）
│   └── media/                  # Photo Picker/CameraX/上传队列封装
└── feature/                    # 每个 feature 只依赖 core，不互相依赖
    ├── auth/                   # AUTH-01–05（M00）
    ├── pet/                    # PET-01–05、FAM-01–05（M01）
    ├── today/                  # TODAY、HOME（M02）
    ├── care/                   # CARE、DAILY、交接卡（M03）
    ├── food/                   # FOOD/MIX/PLAN/FEED/库存 FOOD-06（M04、M10）
    ├── health/                 # HEALTH/BODY/MEDICAL（M05）
    ├── memory/                 # MEDIA/MEMORY/NOTE（M06）
    ├── assistant/              # AI-01–03、REPORT-01–02（M07）
    ├── community/              # DISC-01–10（M09）
    └── settings/               # MY/BILL/CUSTOM（M08）
```

- feature 模块与手册 M00–M10、Pen D1–D10 一一对应，便于按模块提交与联调（手册 §8 顺序）。
- 起步期允许合并为更少模块（如 `feature:main` 先承载首页），但目录分包保持上述边界，达到两三个页面后再拆模块。

---

## 7. 数据与状态模式（关键页面的推荐写法）

```kotlin
// UiState 统一形态（示例）
data class TodayUiState(
    val pets: List<PetSummary> = emptyList(),
    val currentPetId: PetId? = null,
    val sections: Map<TodaySection, SectionState> = emptyMap(), // 一块失败不遮挡其它分区（M02 自测）
    val isRefreshing: Boolean = false,
)

sealed interface SectionState {
    data object Loading : SectionState
    data object Empty : SectionState
    data class Ready(val data: SectionData) : SectionState
    data class Error(val error: UiError, val retryable: Boolean) : SectionState
}
```

- ViewModel 持有 `MutableStateFlow<UiState>` + `stateIn WhileSubscribed(5s)`；副作用（导航、弹层）用 `Channel`/`SharedFlow` 单独通道。
- 列表页统一 Paging 3 + Room 缓存；详情页"网络优先 + 缓存兜底"。
- 所有写操作走同一 `SubmitController`：校验 → 提交（禁用按钮 + Idempotency-Key）→ 成功/失败/冲突/待核实四态（手册 §6.2）。

---

## 8. 中国大陆合规与分发

| 项 | 要求与做法 |
|---|---|
| App 备案 | 工信部 App 备案（ICP 备案关联）完成后方可上架；包名/证书信息与备案一致 |
| 隐私合规 | 首启《用户协议》《隐私政策》同意弹窗（AUTH-01 已设计）；个人信息保护法/工信部 164 号文：权限最小化、SDK 清单公示、隐私政策列明第三方 SDK（推送、崩溃、统计）；同意前不得收集与初始化采集类 SDK |
| 权限策略 | 相册在首次选图时请求（手册 M00）；相机、通知按场景渐进申请；拒绝后功能降级可用手动输入 |
| 崩溃与统计 | 不用 Firebase；候选：自托管 Sentry / 腾讯 Bugly（崩溃），友盟+ / ThinkingData（统计）（**待定项**，随隐私评审确定） |
| 分发 | 国内商店（华为、小米、OPPO、vivo、荣耀、应用宝等）以 APK 为主（部分支持 AAB）；签名 v2/v3，**密钥自管**（无 Play App Signing），keystore 入库加密备份 |
| 内测分发 | 蒲公英/fir.im 或内部分发页；dev/staging 包独立 applicationId |
| 应用内更新 | 商店外自更新需符合各商店规则；首版建议引导跳转商店 + 官网 APK，版本信息走服务端配置 |

---

## 9. 测试与质量

| 层 | 工具 | 覆盖重点（对应手册自测） |
|---|---|---|
| 单元测试 | JUnit + MockK + Turbine | 能量/金额计算、错误码→UiError 映射、写操作状态机、幂等键复用、cursor 重置 |
| 数据层测试 | MockWebServer + Room in-memory | 401/403/409/422/429 分支、断网重试、ETag 冲突、SSE 断流恢复 |
| UI 测试 | Compose UI Test（createComposeRule）+ Robolectric | 关键旅程：注册→建宠→记录→完成任务（E2E-01/02）、冲突弹窗、无权限态 |
| 截图测试（可选） | Roborazzi | soft26 三主题与玻璃降级回归 |
| 静态检查 | Lint（含官方 Compose/性能规则）+ ktlint + detekt | CI 必过 |
| 性能 | Baseline Profiles + Macrobenchmark | 冷启动、首页滚动；R8 full mode |
| 内存 | LeakCanary（debug）+ Plumber | 媒体页、SSE 长连接泄漏排查 |

CI（GitHub Actions 或自建 Runner）：`lint → test → assembleStaging → assembleProd`，产物含 mapping.txt 上传崩溃平台。

---

## 10. 与服务端联调约定

1. **契约先行**：以 [接口与数据契约 v2.1] 为唯一事实源；建议服务端随版本发布 OpenAPI 文档，客户端 DTO 以其为准（可用 openapi-generator 生成骨架 + 人工校准序列化注解，是否启用**待定**）。
2. **联调环境**：dev 环境直连服务端开发库；客户端提供"环境切换"隐藏入口（设置页连点版本号），仅 dev/staging 构建。
3. **联调用例包**：每个模块交付时附 MockWebServer 用例（正常/空/401/403/404/409/422/429/5xx/断网），先本地绿再进真实联调。
4. **问题反馈带证据**：bug 单附 requestId / jobId / Chucker 导出，按手册 §9 验收记录格式登记。

---

## 11. 明确不采用项（首版）

| 不采用 | 原因 |
|---|---|
| Flutter / React Native | 已决策原生 Android（§1.1）；手册 §5 客户端行随之更新 |
| Firebase 全家桶（Crashlytics/Analytics/FCM） | 大陆不可用；替代见 §5.6、§8 |
| KMP 跨端（现阶段） | 保留演进可能（core:model 纯 Kotlin），首版不引入 KMP 插件与额外复杂度 |
| RxJava、MVP、MVIKotlin/Orbit 等框架库 | 用官方 Coroutines/Flow + 推荐架构；避免第二套异步模型 |
| EncryptedSharedPreferences / Jetpack Security Crypto | 官方已废弃；用 DataStore Tink / Keystore |
| Kapt | 用 KSP2 |
| Compose Multiplatform / 第三方跨端 UI | 单端不引入 |
| 自研路由框架 | 用 Navigation 3 |

---

## 12. 版本核对与更新规则

- 本文版本号为 2026-10-26 的稳定线快照；**创建仓库当天**按以下官方页锁定精确补丁版本并回填 `libs.versions.toml`：
  - AndroidX 全量稳定版：https://developer.android.com/jetpack/androidx/versions/stable-channel
  - Android Studio 发布：https://developer.android.com/studio/releases
  - Compose 与 AGP/compileSdk 兼容：https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler
  - Kotlin/KSP：https://kotlinlang.org/
- 升级规则对齐服务端规范 §3.1：以"兼容组合"为单位升级（AGP+Gradle、Kotlin+KSP+Compose Compiler、Compose BOM），升级前在隔离分支跑全量测试与关键页回归，记录日期与验证结果。
- 触发文档更新的事件：min/targetSdk 调整、推送聚合 SDK 选定、OpenAPI 生成策略确定、包名确定。

## 13. 待定项清单

| 编号 | 事项 | 影响面 | 建议决策时点 |
|---|---|---|---|
| T-1 | ~~Package name~~ **已确定 `com.jwd.fafa`**（2026-10-26，仓库 FaFaAndroid 已按此创建并完成包名重构） | 备案、签名、商店 | — |
| T-2 | 推送聚合 SDK（极光/个推/TPNS/阿里云） | 通知触达、隐私清单 | M02 联调前 |
| T-3 | 崩溃/统计平台（Sentry 自托管 vs Bugly；友盟 vs ThinkingData） | 合规、运维 | 平台基础模块期 |
| T-4 | OpenAPI 代码生成 vs 手写 DTO | 网络层开发效率 | 网络层搭建前 |
| T-5 | 团队统一 JDK（17 或 21） | CI 镜像 | 建仓时 |
| T-6 | 仓库位置（FaFaAndroid 子目录 vs 独立仓库） | 版本管理与 CI | 建仓前 |

---

## 14. 参考来源

- [Android 应用架构指南（官方推荐）](https://developer.android.com/topic/architecture)
- [架构建议清单（Compose/Hilt/Navigation 3 等强烈建议项）](https://developer.android.com/topic/architecture/recommendations)
- [Android Studio 发布说明](https://developer.android.com/studio/releases)
- [AndroidX 稳定版一览](https://developer.android.com/jetpack/androidx/versions/stable-channel)
- [Navigation 3 发布说明](https://developer.android.com/jetpack/androidx/releases/navigation3)
- [Compose Compiler Gradle 插件与 AGP/compileSdk 兼容](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler)
- [Room 发布说明（2.8.x，KMP 支持）](https://developer.android.com/jetpack/androidx/releases/room)
- [DataStore 发布说明（datastore-tink 加密）](https://developer.android.com/jetpack/androidx/releases/datastore)
- [Retrofit（3.0）](https://github.com/square/retrofit)｜[OkHttp](https://github.com/square/okhttp)
- [Coil 3（KMP 图片加载）](https://coil-kt.github.io/coil/)
- [EncryptedSharedPreferences 废弃与替代方案分析（2026-08）](https://blog.includesecurity.com/2026/08/encryptedsharedpreferences-is-dead-heres-what-you-should-use-instead/)
- [Now in Android（Google 官方参考工程）](https://github.com/android/nowinandroid)

---

**本文维护**：技术栈变更需同步更新 §2 总览表与 `libs.versions.toml`；与服务端契约相关的变更回写 [前后端交互开发与模块验收手册 v1.0] 对应章节。
