# FaFa Project — Comprehensive Analysis

> Analysis date: 2026-10-06 · Scope: full repository at `d:\dev\Java\FaFa`
> Purpose: baseline understanding of architecture, components, data flows and technical debt before subsequent modifications.

---

## 1. Executive Summary

**FaFa（发发）** is a pet lifecycle management system delivered as a WeChat mini-program. This repository contains the **Java business backend (`fafa-java`)**, one of three cooperating services:

| Service | Stack | Role |
|---|---|---|
| `fafa-miniapp` (not in this repo) | Native WeChat mini-program JS | Frontend; currently login-capable shell |
| **`fafa-java` (this repo)** | Spring Boot 3.5.5 / Java 21 (virtual threads) | Business service: pets, records, feeding, medical, reminders, expenses, collaboration, quotas, AI orchestration |
| `fafa-python` (not in this repo) | FastAPI + LangGraph 5-node Agent | AI service: chat agent, photo/media embedding & recognition (DashScope/Qwen + Qdrant vectors) |

The backend is a **textbook hexagonal / DDD four-layer monolith** (`interfaces → application → domain ← infrastructure`) with unusually disciplined domain modeling: guard-method state machines, idempotency keys everywhere, callback-driven async AI flows with out-of-order protection, and a three-tier collaboration permission model. The main gaps: **real secrets committed to the repo**, the **domain-event module is a placeholder**, **scheduler jobs lack distributed locking**, test coverage is domain-only, and several docs/config conventions have drifted from code.

---

## 2. Repository Layout

```
FaFa/
├── .agent/
│   └── development-guidelines.md    # DDD/coding/logging standards for humans & AI (updated 2026-08-18)
├── docs/
│   ├── dev/功能模块.md               # Feature spec per module (partially outdated vs. code)
│   ├── src/                         # Assets (cat images, demo video) — dev reference material
│   ├── 服务器与中间件部署信息.md      # ⚠ Dev server & middleware credentials (see §12 Security)
│   └── 项目整体架构.png              # System architecture diagram (Java↔Python↔middleware)
├── fafa-java/
│   ├── pom.xml                      # Single-module Maven build
│   └── src/
│       ├── main/java/com/fafa/
│       │   ├── FaFaApplication.java
│       │   ├── common/              # Result<T>, ErrorCode, BusinessException
│       │   ├── interfaces/          # Layer 1: 31 controllers, DTOs, 2 assemblers, GlobalExceptionHandler
│       │   ├── application/         # Layer 2: 37 services, 6 ports (gateways), command/DTO records
│       │   ├── domain/              # Layer 3: 16 model packages, 31 repository interfaces, 7 domain services
│       │   └── infrastructure/      # Layer 4: AI/MQ/OSS/WeChat adapters, persistence, config, schedulers, security
│       ├── main/resources/
│       │   ├── application.yml      # Shared config (virtual threads, Flyway, MyBatis-Plus, Sa-Token, actuator)
│       │   ├── application-{dev,test,prod}.yml
│       │   ├── db/migration/        # Flyway V1.1 → V12
│       │   └── logback-spring.xml
│       └── test/java/com/fafa/      # 29 unit tests (28 domain + 1 application)
└── README.md                        # (empty)
```

### Layer dependency rules (enforced by convention)
- `interfaces` → `application` → `domain`; `infrastructure` implements `domain`/`application` ports (dependency inversion).
- `domain` is framework-free except two `@Service` domain services (`ReminderScheduleService`, `SpeciesRecordPolicy`); aggregates use `create/restore` factory methods, guard methods throw `IllegalStateException` on illegal transitions.
- Controllers contain no business logic — validation (Jakarta) + DTO↔Command conversion only; uniform `Result<T>` envelope; `@RestControllerAdvice` global exception mapping.

---

## 3. Technology Stack Inventory

### 3.1 Build & runtime
| Item | Value |
|---|---|
| Language | **Java 21** (virtual threads enabled: `spring.threads.virtual.enabled=true`) |
| Framework | **Spring Boot 3.5.5** (web, validation, actuator, data-redis) |
| Build | Maven single module, `spring-boot-maven-plugin`, artifact `fafa-java-1.0.0` |
| Java→JSON | Jackson (snake_case payload adapters via `@JsonNaming` for Python contracts) |
| Boilerplate | Lombok |

### 3.2 Persistence
| Library | Usage |
|---|---|
| **MyBatis-Plus 3.5.9** (+jsqlparser) | ORM; `BaseMapper` mappers, `LambdaQueryWrapper`, pagination interceptor, `assign_id` snowflake IDs (some POs use DB `AUTO`), logic delete via `@TableLogic isDeleted` |
| **MySQL 8.0** (mysql-connector-j) | Primary store, DB `fafa` (utf8mb4) |
| **Druid 1.2.23** | Connection pool; dev-only stat view servlet `/druid` |
| **Flyway** (core + mysql) | 12 migration scripts, `baseline-on-migrate: true` |

### 3.3 Auth, cache & messaging
| Library | Usage |
|---|---|
| **Sa-Token 1.39.0** (+redis-jackson) | Session auth; header `Authorization`, UUID token style, 30-day timeout, Redis-backed; global `SaInterceptor` with whitelist |
| **Redis 7.2** | Sa-Token sessions, quota counters (monthly INCR/DECR), collaboration rate limiter (fixed 1h windows) |
| **Redisson 3.36.0** | Declared in pom (client lib present) |
| **RocketMQ 4.9.4** (rocketmq-spring 2.3.1) | Java is **producer-only**: async AI task dispatch (2 topics); consumers live in fafa-python |

### 3.4 Storage & integrations
| Library / Service | Usage |
|---|---|
| **MinIO 8.5.12** | Dev/test object store; single public-read bucket `fafa-media`; auto bucket+policy setup at startup |
| **Aliyun OSS SDK 3.18.1** | Prod object store (`fafa-prod` bucket); strategy-switched via `fafa.storage.type` |
| **WeChat Open Platform** | `code2Session` login (`WeChatClient`); subscription-message push is a reserved (unimplemented) integration point |
| **DashScope (Aliyun Bailian)** | Qwen models — accessed only via fafa-python, not directly from Java |
| **Qdrant** (via python) | Vector collections `fafa_media`, `fafa_pet_profiles` (1024-dim qwen3-vl-embedding) |

### 3.5 Observability & docs
- **Micrometer tracing (Brave bridge) + Prometheus registry**; actuator exposes `health,info,prometheus`; logback to `./logs`.
- **Knife4j 4.5 + springdoc 2.8.9** — OpenAPI grouped by 19 path groups (dev only; disabled in prod).

### 3.6 Configuration property tree (`fafa.*` via `FaFaProperties`)
Key namespaces: `wechat`, `python.base-url`, `internal-token`, `storage.type`, `ai.dispatcher` (http|rocketmq), `pet.limit` (normal=2/vip=999), `media.*` (10MB photo / 50MB video / 20 per batch), `quota.*` (storage 2GB/20GB; ai-search 10/mo free, VIP unlimited; food-recognition 5/50; report-content 3/10 shared), `chat.*` (8000 chars, history 20, agent timeout 45s, tool timeout 8s, max 5 tool calls, record-draft confidence 0.80), `medical.*`, `health-alert.*` (thresholds + cron), `food.*` (recognition/transition/score policy v1), `feeding.*`, `expense.*` (CNY, auto-sync-medical), `report.*`, `collaboration.*` (member limits, invitation 30min TTL, rate limits, audit retention 730d, reminder delivery cron), `minio.*`, `oss.*`.

---

## 4. Architecture & System Context

```
WeChat mini-program (fafa-miniapp)
   │  wx.login code
   ▼
WeChat Open Platform ──code2Session──► fafa-java (AuthController → WeChatClient)
   ▲
   │ REST + Authorization token (Sa-Token, all business APIs /api/**)
fafa-java  ◄──── X-Internal-Token ────► fafa-python (FastAPI + LangGraph agent)
   │  │                                      │  │
   │  ├─ sync HTTP (6 RestClients, timeouts 45–120s): /api/chat/send, /api/medical/summaries,
   │  │   /api/reports/generate, /api/content/generate, /api/foods/recognize,
   │  │   /api/pets/profile-photos, /api/photos/analyze, /api/photos/search, /health
   │  ├─ async RocketMQ (prod): fafa_pet_photo_vectorization, fafa_photo_analysis_topic:photo
   │  ├─ callbacks python→java: POST /api/callback/photo-analysis, /api/callback/pet-vectorization
   │  └─ agent data fetch python→java: GET/POST /internal/ai/** (18 read-only endpoints)
   ▼
Middleware @192.168.1.14 (Docker Compose, network fafa-network):
   MySQL 8.0 (Flyway) · Redis 7.2 · MinIO (dev) / Aliyun OSS (prod) · RocketMQ · Qdrant
```

Dispatch strategy: `fafa.ai.dispatcher=http` in dev (direct POST), `rocketmq` in prod (peak-shaving). Vector-delete / lightweight idempotent ops always go HTTP regardless of mode. Messages carry object-storage URLs only (videos use 1-hour presigned URLs generated by Java). All python-bound HTTP calls inject `X-Internal-Token` via `RestClientConfig` default headers; `traceId` (MDC) propagates via `X-Trace-Id`.

---

## 5. Module Inventory (business features)

### 5.1 Auth & user
- **WeChat-only login**: code → `code2Session` → openid → auto-register on first login → Sa-Token session. Logout destroys session.
- User profile (nickname/phone/gender/age/city), avatar upload.
- **VIP lifecycle**: upgrade/renew (VIP expiry lazily evaluated on read via `User.expireVip()`); no payment integration yet (upgrade endpoint is essentially open — see §12). VIP status drives pet limits, quotas, collaboration member slots.

### 5.2 Pet profile & vectorization
- Registration requires **exactly 3 photos** (slot 1 avatar / 2 front / 3 side); parallel virtual-thread upload to OSS; orphan cleanup on failure.
- Per-user unique pet name; pet count limit (free 2 / VIP 999).
- **Vectorization state machine**: `PENDING → COMPLETED/FAILED`; photo replacement resets to PENDING; late failure callbacks can never downgrade COMPLETED.
- Delete: logical delete + cascade reminder cleanup + media de-reference + Qdrant vector cleanup (HTTP).

### 5.3 Daily records & weight
- **Daily status check-in**: 6 dimensions (appetite/energy/water/defecation/vomiting/urination) with **species-specific policies** (`SpeciesRecordPolicy`): cats must report urination (urgent-care hint), dogs have no urination dimension. One-record-per-day upsert; “all normal” one-tap; “copy last”; escape values (`not_seen`) are neutral.
- **Weight records**: create updates `pet.weight` in the same transaction (redundant “latest known weight” semantic); trend over ≤366 days (default 90); delete does not roll back `pet.weight`.

### 5.4 Feeding & food
- **Food cards**: CRUD + archive/activate; full nutrition fields (7 nutrients, energy, Ca/P), ingredients/additives snapshots; delete blocked while referenced by an active plan (`FOOD_IN_ACTIVE_PLAN`).
- **Food recognition** (AI): create task (media + idempotency key) → acquire monthly quota → sync call to python `/api/foods/recognize` → user confirms/rejects → FoodCard; drafts expire after 7 days.
- **Food safety score**: deterministic, explainable 4×25-point scoring (protein clarity, additives penalty 5/flag, Ca:P ratio in [1.0,2.0], life-stage label); total only when all four known; always carries a disclaimer (informational, not a safety certification).
- **Food transition plans**: 3–30 days, daily ratio steps summing to exactly 100%, per-day reaction log, complete/cancel lifecycle.
- **Feeding plans**: rule-based recommendation (non-AI, “rule” source + disclaimer), items ≤12 with unique itemKey/mealTime, pause/resume/complete, “today checklist” joins plan items with actual feeding records.
- **Feeding records**: event-stream CRUD; permission-gated (`CREATE_RECORD`/`READ_RECORD`, edit/delete owner-only); ≤5min future tolerance; plan-execution records carry food snapshot and are idempotent per plan item.

### 5.5 Medical & health
- **Medical visits**: CRUD + ≤9 attachments; date not in future; **same-transaction expense sync** (`syncMedicalVisitExpense`, auto mode on) keeping manual/medical expense sources consistent.
- **Medication plans**: ACTIVE↔PAUSED→COMPLETED/DELETED; ≤6 administration times/day, AS_NEEDED cannot have fixed times; **bidirectional reminder linkage** (each administration time derives a `Reminder.createMedication`; pause/resume/complete/delete cascades to reminders).
- **Medication records**: TAKEN/SKIPPED/MISSED/CANCELLED; TAKEN requires `takenAt` (≤5min future tolerance); idempotent per plan+scheduledAt; can be generated from reminder completion in the same transaction.
- **Medical summary** (AI): ≤90-day window, gateway to python.
- **Health alerts**: rule engine with 6 rules over a 7-day window (see §7), fingerprint (SHA-256) dedup, auto-resolve of no-longer-matching alerts, red-level re-alert after 24h, 180-day retention; scheduler every 30 min; acknowledge/resolve endpoints; read requires `READ_MEDICAL` permission.

### 5.6 Reminders & events
- Types: vaccine/deworming/bath/nail_trim/checkup/medication/birthday/daily_record/custom. MEDICATION reminders can only be created by medication plans.
- **No persisted completed state**: periodic reminders advance `nextRemindTime` in place (ONCE → FINISHED); next period computed from the *planned* time (no drift), with overdue roll-forward; WEEKLY=ISO weekday, MONTHLY clamps to month end.
- Completion is a single transaction: domain completion → `ReminderCompletion` history (append-only, snapshot title/type) → whitelist types generate an `EventRecord` (growth timeline) → medication type writes medication record.
- **Push scan** (hourly; dev every 5 min): due + overdue + tomorrow-important (vaccine/checkup/deworming, 1-day advance); per-reminder once-per-day dedup; collaboration mode uses `ReminderNotificationDelivery` with business-key delivery-window idempotency and retry cap (3). **Actual push channel is a stub (log only)** — WeChat subscription messages are the planned integration.
- Event records: manual events editable/deletable; reminder-generated events immutable.

### 5.7 Media (photos/videos)
- Batch upload ≤20 (10MB photo / 50MB video); parallel virtual-thread OSS upload; storage quota atomically incremented via conditional UPDATE (failure → full rollback + uploaded-file cleanup); AI analysis dispatched AFTER_COMMIT.
- **Ownership recognition flow**: python embedding callback assigns petId only when exactly 1 candidate matches and no explicit assignment; multiple matches → pet-name tags only; manual confirm-pet endpoint overrides.
- Tags ≤10 manual (auto pet tags exempt), pet rename triggers async tag migration.
- Semantic search (AI): monthly quota (free 10/mo, VIP unlimited) with DECR rollback on failure; Qdrant results re-filtered against MySQL (`is_deleted`) to tolerate vector residue; deterministic time-summary generation.
- Delete: logical delete + quota release + AFTER_COMMIT OSS/vector cleanup.

### 5.8 AI chat & content
- **Chat sessions/messages**: pet-bound sessions; `clientMessageId` idempotency; user message persisted → gateway call (no DB transaction held during model call) → assistant message persisted with blocks/citations/toolCalls/recordDraft JSON, model + token usage, traceId; blocks whitelist {stat, chart, media, link, notice}; history limit 20.
- **Recommendations**: deterministic (≤6) from recent weight/daily-status/event presence — no AI call.
- **Report generation** (week/month/quarter/year): fact snapshot assembly (11 fact categories, `report-facts-v1`) → quota (shared with content) → python generation → **content validation** (length caps, sourceKey prefix whitelist across 10 fact namespaces, banned-medical-terms list) → completed/degraded (deterministic fallback) / failed. Force-regenerate bumps `generationVersion`.
- **Content drafts & schedules** (share_note / xiaohongshu): same fact assembler + quota pool; draft ≤12000 chars + banned-medical-words; schedules allow interval ∈ {7,15,30} days, nextRunAt rolling, per-run idempotency key `schedule-{id}-{nextRunAt}`, ≤10 due schedules per scan (every 10 min).

### 5.9 Expenses
- Manual CRUD (amount >0, ≤1,000,000.00, exactly 2 decimals via `Money` VO, date not in future, 6 categories); AI/system-generated medical-expense records are immutable and auto-restorable.
- Statistics: month/year periods anchored to a date; per-category totals + shares with **rounding-residual correction so shares sum to exactly 100%**; daily/monthly trend points; output includes `dataStatus` (complete/partial), limitations, `calculationVersion`.

### 5.10 Collaboration (family sharing) & emergency card
- Invitation flow: owner creates (rate limit 20/h) → one-time token (**only SHA-256 stored**, 30-min TTL) → invitee accepts (rate limit 30/h) under pessimistic locks (`findForUpdate` on invitation + pet-user pair) → member ACTIVE; CAREGIVER must have expiry (≤30 days), MEMBER is permanent.
- **Permission model** (`PetAccessDecision`, 17 permission codes): owner = all; MEMBER = base + read medical/expense/food/report/emergency; CAREGIVER = base + read emergency only. Feature flag `fafa.collaboration.enabled` (off by default in prod). Member limits: free 0 extra members, VIP 5.
- Denied access is audited (`access_audit_logs`, 730-day retention, traceId-carrying, owner-only read).
- **Emergency card**: blood type/allergies/chronic conditions/contacts/hospital; owner-only edit under **pessimistic lock + version optimistic lock** (conflict → `EMERGENCY_PROFILE_VERSION_CONFLICT`); READ_EMERGENCY is the only medical-class permission a caregiver has (emergency use case).

### 5.11 Quotas
| Quota | Mechanism | Free | VIP |
|---|---|---|---|
| Storage | DB conditional UPDATE (`user_quota_usage`) | 2 GiB | 20 GiB |
| AI semantic search | Redis INCR by `yyyyMM` key, DECR rollback on failure | 10/mo | unlimited |
| Food recognition | Redis INCR/DECR | 5/mo | 50/mo |
| Report + content generation | Redis INCR/DECR, **shared pool across both features** | 3/mo | 10/mo |

---

## 6. API Surface (170+ endpoints, 31 controllers)

### 6.1 Public user-facing (Sa-Token; `/auth`, `/user` without `/api` prefix; everything else under `/api`)
| Group | Controller(s) | Highlights |
|---|---|---|
| Auth | AuthController | `POST /auth/wechat/login` (whitelisted), `/auth/logout` |
| User | UserController | `/user/me`, profile, avatar, `/user/vip`, `/user/vip/upgrade`, `/user/vip/renew` |
| Pets | PetController | CRUD, 3-slot photo replacement (retriggers vectorization), sort; pet-count limits |
| Collaboration | PetCollaborationController, PetInvitationController | access check, members, invitations create/revoke/accept |
| Audit | AccessAuditController | `/api/pets/{petId}/audit-logs` |
| Emergency | EmergencyCardController | profile get/update (versioned), card read |
| Tags | TagController | list/create/delete (definition only; snapshots unaffected) |
| Media | PhotoController | batch upload, list, semantic search, pending-recognition, reanalyze, edit, confirm-pet, delete |
| Chat | ChatController | sessions CRUD, messages (cursor `beforeId`), recommendations |
| Reports | ReportController | generate (force), list/get/regenerate/delete |
| Content | ContentDraftController, ContentScheduleController | drafts CRUD + regenerate; schedules upsert/enable/disable/delete |
| Medical | MedicalController | visits CRUD + attachments, timeline, medication plans (pause/resume/complete), records, summaries — 19 endpoints |
| Health | HealthAlertController | list/detail/evaluate/acknowledge/resolve |
| Food | FoodCardController, FoodRecognitionController, FoodSafetyScoreController, FoodTransitionController | cards CRUD+archive; recognition task lifecycle (confirm/reject); safety score; transition plans |
| Feeding | FeedingPlanController, FeedingRecordController, FeedingAnalysisController | recommendations (preview), plans, today checklist, records (range ≤366d), java-stats analysis |
| Records | DailyRecordController, WeightRecordController | dimensions config, upsert, all-normal, copy-last, range list; weight create/latest/trend/delete |
| Reminders | ReminderController | CRUD, today view, complete (transactional closure), pause/resume, history |
| Expenses/Events | ExpenseController, EventRecordController | CRUD, categories, statistics; manual events |
| Quota | QuotaController | usage + VIP status |
| Utility | PingController | `GET /api/ping` (whitelisted) |

### 6.2 Service-to-service (protected by `X-Internal-Token`, exempt from Sa-Token)
- `CallbackController` (`/api/callback/**`): `photo-analysis` (recognition score/petIds, legacy fields tolerated), `pet-vectorization` — both idempotent, terminal-state protected.
- `InternalAiQueryController` (`/internal/ai/**`): 18 read-only endpoints consumed by the python Agent (pets, daily status, weight, feeding, events, reminder history, media search/detail, medical visits, medication plans/records, expenses + statistics, health alerts, foods, feeding plans, reports). Returns bare DTOs (no `Result` wrapper); every method re-validates `userId` ownership.

### 6.3 Cross-cutting API behavior
- Global `Result<T>` envelope; `GlobalExceptionHandler` maps `BusinessException`/`NotLoginException`/validation/404/generic → typed error codes (`ErrorCode` enum).
- Auth is centralized in `SaTokenConfig` interceptor (no per-method annotations); whitelist: `/auth/wechat/login`, `/api/ping`, `/api/callback/**`, `/internal/**`, `/actuator/**`, swagger paths, `/error`.

---

## 7. Core Business Logic (domain services & state machines)

### Domain services (pure, unit-tested)
| Service | Rules |
|---|---|
| `HealthRuleEngine` | 6 rules over 7-day window, thresholds configurable: `REFUSAL_WITH_VOMITING` (RED, single day), `APPETITE_ENERGY_DECLINE_2D` (YELLOW, 2d), `INCREASED_WATER_3D` (3d), `DIARRHEA_2D` (2d), `VOMITING_MULTIPLE` (single day), `MEDICATION_MISSED_2` (≥2 missed/skipped, evidence capped at 10). Consecutive-day matching disallows gaps; SHA-256 fingerprint = dedup key. |
| `FoodSafetyScoreService` | 4×25-point explainable score; Ca/P ratio to 2 decimals HALF_UP in [1.0,2.0] → 25 else 12; additives 25−5×flags; total only when all dimensions known; fixed disclaimer. |
| `ReminderScheduleService` | Next-period computation from planned base time; ONCE/DAYS/WEEKLY/MONTHLY/MONTHS/YEARLY; month-end clamping; overdue roll-forward to now; value validation per type. |
| `SpeciesRecordPolicy` | Cat/other vs dog dimension configs & required fields; cat urination carries urgent-care messaging. |
| `ExpenseStatisticsCalculator` | Filter deleted + period bounds; share rounding residual absorbed by last category (sum = 100%); month→daily / year→monthly trends. |

### Key state machines
- Pet vectorization: `PENDING → COMPLETED/FAILED` (failure can never overwrite COMPLETED).
- Food recognition: `PENDING → PROCESSING → COMPLETED → CONFIRMED|REJECTED|EXPIRED` (+FAILED from pre-terminal states).
- Food transition: `DRAFT/ACTIVE → COMPLETED|CANCELLED`.
- Feeding plan: `ACTIVE ↔ PAUSED → COMPLETED`; Medication plan: `ACTIVE ↔ PAUSED → COMPLETED/DELETED`.
- Reminder: `PENDING ↔ PAUSED`, ONCE → FINISHED; periodic stays PENDING with advancing nextRemindTime.
- Health alert: `OPEN → ACKNOWLEDGED`, `→ RESOLVED` (idempotent), `redetect()` can revive resolved/expired → OPEN.
- Media analysis: `PENDING → COMPLETED|FAILED|SKIPPED` (terminal, idempotent callbacks).
- Invitation: `PENDING → ACCEPTED|EXPIRED|REVOKED`; Member: `ACTIVE → REVOKED|EXPIRED` (caregiver time-based expiry).
- Report/ContentDraft: string states `facts_ready/generating → completed|degraded|failed`.

### Conventions & invariants
- All historical range queries capped at 366 days (expense 3660); pages capped at 50 (default 20).
- Logical delete dominant (`isDeleted`); append-only facts (completion history, delivery records, audit logs); snapshots (title/type/food name/tags) survive referenced-entity deletion.
- Time injected from application layer into domain (testability); money as `Money` VO (2-decimal enforcement).

---

## 8. Data Flow (key sequences)

1. **Login**: miniapp → `POST /auth/wechat/login` → WeChat `code2Session` → find-or-register `User` → `StpUtil.login` → token (30d).
2. **Pet registration → vectorization**: 3 photos parallel-upload OSS → persist Pet (PENDING) → afterCommit dispatch (HTTP dev / MQ prod) → python embeds into `fafa_pet_profiles` → `POST /api/callback/pet-vectorization` → status COMPLETED/FAILED (idempotent).
3. **Media upload → analysis → semantic search**: batch upload → storage quota conditional UPDATE → persist MediaAsset → afterCommit dispatch `/api/photos/analyze` (video via presigned URL) → python embeds into `fafa_media`, matches pet → callback assigns petId or adds pet-name tags (exactly-one rule) → later user query `POST /api/photos/search/semantic` (quota) → Qdrant + MySQL re-filter → DTOs with deterministic time summary.
4. **Chat**: create session → `POST /messages` (idempotent) → persist user msg → `ChatAgentGateway.chat` (45s, X-Trace-Id) — python Agent itself pulls Java data via `/internal/ai/**` (tool calls ≤5, tool timeout 8s) → persist assistant msg (blocks/usage) → reply.
5. **Report**: generate → existing check → `ReportFactAssembler` snapshot → quota acquire → `ReportGenerationGateway` (90s) → `ReportContentValidator` (whitelist/banned words) → completed; on AI failure → quota release + deterministic degraded content.
6. **Reminder completion**: complete → (tx) domain advance/finish + history + EventRecord (whitelist) + medication record if MEDICATION → idempotent; push scheduler separately delivers due/tomorrow-important reminders with delivery-window idempotency.
7. **Medical ↔ expense sync**: visit create/update/delete ↔ same-tx `syncMedicalVisitExpense/removeMedicalVisitExpense` (system-generated expense immutable/restore-able).

---

## 9. Persistence Model

- **Flyway migrations** V1.1 (users) → V12 (pet vectorization status): user, media, daily/weight/feeding records, reminders (+completion, delivery), AI chat, medical/health, food/feeding-plan, expense, report/content, collaboration + emergency; V11 unique active pet name; note: no `V1` base script exists (starts at V1.1, relies on `baseline-on-migrate`).
- **PO pattern**: `BaseDO` (snowflake id, createdAt/updatedAt auto-fill via `MyMetaObjectHandler`) → `SoftDeleteDO` (+`isDeleted @TableLogic`) for traceable tables; behavior-log tables extend BaseDO only. **Inconsistency**: `PetPO`/`ChatMessagePO` declare fields directly with `IdType.AUTO` DB auto-increment, coexisting with global `assign_id` (§12).
- **Repositories**: 31 framework-free domain interfaces implemented in `infrastructure/persistence/repository/` with hand-written PO↔domain mapping; `JsonListConverter` manually serializes `List<String>` JSON columns (not a TypeHandler).
- AI-heavy tables store JSON string columns (chat blocks/citations/toolCalls/recordDraft; report/content fact & payload; plan nutrition snapshot).

---

## 10. Scheduling & Background Jobs

| Job | Cron (default) | Purpose |
|---|---|---|
| `ReminderPushScheduler` | `0 0 * * * *` hourly (dev `0 */5 * * * *`) | Due/overdue/tomorrow-important reminder delivery, once-per-day per reminder, delivery-window idempotency |
| `ContentScheduleScheduler` | `0 */10 * * * *` | Execute due content schedules (≤10 per pass) |
| `HealthAlertScheduler` | `0 */30 * * * *` (flag-gated) | Per-pet health rule evaluation across all active pets |

All three swallow exceptions to the next cycle. **No distributed lock (ShedLock/Redisson) — horizontal scaling would duplicate jobs** (§12).

---

## 11. Testing

- **29 JUnit unit tests**: 28 domain-layer (aggregates + domain services: MediaAsset, Reminder family, collaboration, emergency, food, feeding, chat, expense, report period, record enums) + 1 application-layer (`QuotaApplicationServiceTest`).
- No controller/integration tests, no Testcontainers, no architecture tests (e.g., ArchUnit), no CI config in repo.

---

## 12. Technical Debt, Risks & Improvement Areas

### 12.1 Security (highest priority)
1. **Live secrets committed in-repo**: `docs/服务器与中间件部署信息.md` contains WeChat AppSecret, DashScope API key, MySQL root/business passwords, MinIO keys (it even warns “不得提交到公开仓库” but is tracked). `application-dev.yml` hardcodes WeChat AppSecret and MinIO credentials as defaults, and dev Redis/MySQL run passwordless on LAN. → Rotate all credentials, move to env/secret manager, scrub git history, consider BFG + secret scanning.
2. **VIP upgrade without payment**: `/user/vip/upgrade` grants VIP by request parameter — fine for dev, unacceptable in production (documented as pending WeChat Pay + reconciliation).
3. Public-read media buckets rely on UUID unguessability; presigned URLs used only for AI video processing. Acceptable trade-off but should be a conscious decision (per-object ACL or private + CDN signing in prod).
4. Dev-only Druid stat servlet has weak credentials (`fafa/fafa123`) — ensure it never leaks to prod (prod has it disabled).

### 12.2 Architectural gaps
5. **Domain events are placeholders**: `domain/event` and `application/subscriber` contain only `package-info.java`; no Spring event publication anywhere. Cross-aggregate coordination is done via direct application-service calls (medical↔expense, medication↔reminder) — works, but the planned decoupling never landed.
6. **Schedulers lack distributed locks** — running 2+ instances would double-push reminders / double-generate content. Redisson is already a dependency; add ShedLock/Redisson locks or leader election.
7. **MQ send failure only logged** (`AiTaskProducer`) → media can stay `pending` silently; mitigation is the manual `reanalyze` endpoint, but no dead-letter/retry/outbox mechanism exists.
8. **Qdrant deletion is eventual** — semantic search compensates by re-filtering MySQL (`is_deleted`); documented, but a reconciliation job would be cleaner.

### 12.3 Consistency & hygiene
9. **PO style split**: `PetPO`/`ChatMessagePO` (field-declared, `IdType.AUTO`, some without updatedAt/isDeleted) vs `BaseDO` convention (snowflake ids) — two persistence idioms coexist; table PKs are presumably BIGINT AUTO_INCREMENT while the global config says assign_id, so behavior differs per entity.
10. **API path drift**: `/auth`, `/user` lack the `/api` prefix; 3 controllers share `/api/pets`, 2 share `/api/foods`; springdoc group `record` points to nonexistent `/api/records/**` and misses `/api/feeding-records/**`, `/api/daily-records/**`, `/api/weight-records/**` → some endpoints may not appear in Swagger groups.
11. **Docs drift**: `docs/dev/功能模块.md` describes superseded endpoints (separate avatar/3-view upload), pet limit “10” vs config 999; `README.md` is empty.
12. `FoodType` enum duplicated in `domain/model/food` and `domain/model/record`; `PetMemberRole`/`InvitationRole` are structurally identical parallel enums (deliberate but worth noting).
13. Flyway numbering starts at `V1.1` with no `V1` — works via baseline but is fragile for fresh environments.

### 12.4 Quality & operability
14. **Test coverage is domain-only**; zero coverage of 37 application services (quota除外), 31 controllers, callbacks, schedulers — the highest-risk orchestration logic (idempotency, quota rollback, transaction boundaries) is untested.
15. No CI/CD pipeline files, no Dockerfile/compose for fafa-java in repo, no code-style/coverage tooling (guidelines recommend CheckStyle/SpotBugs/JaCoCo but none configured).
16. `ReminderPushService` push is log-only — the user-facing notification feature is not yet real (WeChat subscription messages pending; requires template config + user consent flow).
17. Chat/report sync HTTP timeouts (45–90s) hold virtual threads rather than DB connections (good), but client-facing UX for long AI waits may need async/SSE in future — currently fully synchronous from the miniapp's perspective.
18. `MediaApplicationService` and `ReportApplicationService` are the two largest orchestration classes; further growth should extract sub-flows.

---

## 13. Where to Plug In (guidance for upcoming changes)

- **Adding a feature**: follow the vertical slice — DTO/Command in `application/dto/<module>`, port if external I/O, application service, domain aggregate/service, repository interface + Impl + PO + mapper, Flyway migration `V{n}__...`, controller + springdoc group + (if AI-related) callback/internal endpoints with `X-Internal-Token`.
- **Auth-sensitive endpoints**: do NOT add per-method Sa-Token annotations (convention is centralized interceptor); add new public paths to the whitelist consciously.
- **AI tasks**: prefer afterCommit + virtual threads for dispatch; always pair acquire/release for quotas; make callbacks idempotent with terminal-state protection; version prompts (`promptVersion`) and policy (`policyVersion`/`calculationVersion`) on every AI/statistical output.
- **Compliance**: any AI-generated health-adjacent content must pass the banned-medical-terms check and carry disclaimers (established pattern in `ReportContentValidator`, `FoodSafetyScoreService`, `FeedingAnalysis`).
- **Range/page caps**: default to ≤366-day ranges and ≤50-page sizes for any new historical query.
