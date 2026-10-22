"""接口文档使用的脱敏业务示例；这些值不对应任何真实账号、令牌或验证码。"""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

USER_ID = "11000000-0000-4000-8000-000000000001"
PET_ID = "22000000-0000-4000-8000-000000000001"
ADMIN_ID = "33000000-0000-4000-8000-000000000001"
REQUEST_ID = "44000000-0000-4000-8000-000000000001"
CHALLENGE_ID = "55000000-0000-4000-8000-000000000001"
USER = {"id": USER_ID, "email": "owner@example.com", "nickname": "铲屎官A1B2", "signature": None,
        "avatarMediaId": None, "timezone": "Asia/Hong_Kong", "version": 1}
DEVICE = {"deviceId": "android-example-001", "deviceName": "安卓测试设备"}
SESSION = {"token": "00000000-0000-4000-8000-000000000001", "tokenType": "Bearer", "expiresIn": 604800}
PROFILE = {"nickname": "雪球", "species": "CAT", "breedId": None, "sex": "UNKNOWN", "neuteredStatus": "UNKNOWN",
           "birthDate": None, "estimatedBirthMonth": None, "arrivedOn": None}
PET = {"id": PET_ID, "ownerUserId": USER_ID, "role": "OWNER", "profile": PROFILE, "status": "ACTIVE", "version": 1, "deleteAfter": None}
QUOTA = {"bucket": "STORAGE_BYTES", "granted": "1000", "used": "0", "reserved": "0", "available": "1000", "version": 2}
QUOTA_REQUEST = {"id": REQUEST_ID, "userId": USER_ID, "bucket": "STORAGE_BYTES", "amount": "1000", "reason": "申请开发测试空间",
                 "state": "PENDING", "grantedAmount": None, "decisionReason": None, "operatorId": None, "version": 1}
BREED = {"id": "CAT_UNKNOWN", "species": "CAT", "name": "未知品种", "status": "ACTIVE", "version": 1}
ADMIN_USER = {"id": USER_ID, "maskedEmail": "o***@example.com", "nickname": "铲屎官A1B2", "status": "ACTIVE", "version": 2}
PERMISSIONS = ["USER_READ", "USER_WRITE", "QUOTA_READ", "QUOTA_WRITE", "BREED_READ", "BREED_WRITE", "AUDIT_READ"]
PRINCIPAL = {"id": ADMIN_ID, "username": "example-admin", "permissions": PERMISSIONS}


@dataclass(frozen=True)
class Example:
    name: str
    definition: str
    data: Any
    body: dict[str, Any] | None = None
    query: dict[str, Any] = field(default_factory=dict)
    path: dict[str, str] = field(default_factory=dict)
    notes: tuple[str, ...] = ()


EXAMPLES = {
    "listLegalDocuments": Example("查询已发布协议", "按协议类型和可选版本读取已生效的正式发布内容。省略类型时返回各类型最新发布版本。",
        [{"type": "TERMS", "version": "v1", "title": "用户服务协议", "body": "示意正文；以实际发布文本为准。", "contentHash": "a" * 64, "effectiveAt": "2026-10-22T00:00:00Z"}],
        query={"type": "TERMS", "version": "v1"}, notes=("version 必须与 type 一起传；开发夹具不代表正式法律协议。",)),
    "createEmailChallenge": Example("发送邮箱验证码", "为注册或找回密码创建独立用途的邮件挑战。服务端保存验证码摘要，发送成功后才允许验证。",
        {"challengeId": CHALLENGE_ID, "expiresAt": "2026-10-22T04:10:00Z", "resendAfter": 60},
        {"purpose": "REGISTER", "email": "owner@example.com"}, notes=("REGISTER、RESET_PASSWORD 不可混用；默认 6 位、10 分钟、重发间隔 60 秒、最多 5 次错误。", "找回不存在邮箱时返回相同结构但不实际发送；SMTP 未配置时，实际需要发信的请求返回 503。", "响应从不返回验证码。示例验证码仅用于说明 JSON 结构。")),
    "registerAccount": Example("验证码注册账号", "校验协议、邮箱和验证码，在一个数据库事务中创建账号、资料和同意记录并消费挑战；提交后建立移动会话。",
        {"user": USER, "session": SESSION, "nextAction": "NO_PET"},
        {"email": "owner@example.com", "password": "ExamplePassword123", "confirmedPassword": "ExamplePassword123", "challengeId": CHALLENGE_ID,
         "code": "123456", "legalVersions": {"TERMS": "v1", "PRIVACY": "v1"}, "device": DEVICE},
        notes=("密码按 Unicode 码点计数，8—128 个字符，必须同时含字母和数字；不保存确认密码。", "legalVersions 必须恰好包含 TERMS、PRIVACY，版本来自协议查询。", "账号已提交但会话存储不可用时，仍返回 201，session=null、nextAction=LOGIN；客户端直接进入登录恢复。", "响应丢失后使用刚设置的密码登录，不重复注册。")),
    "login": Example("邮箱密码登录", "校验凭证后建立新移动会话并推进认证代际；同账号旧设备令牌失效。", {"user": USER, "session": SESSION},
        {"email": "owner@example.com", "password": "ExamplePassword123", "device": DEVICE}, notes=("邮箱统一去除首尾空白并按既有邮箱规范化规则查询。", "旧设备后续请求返回 FAFA-40102 或撤销相关的 401 编码，应清除会话并进入登录页。")),
    "logout": Example("退出移动会话", "撤销当前账号会话代际并退出当前令牌。", None, notes=("请求不含 JSON 请求体；需要有效移动端令牌。",)),
    "verifyPasswordReset": Example("验证找回密码验证码", "验证 RESET_PASSWORD 挑战并一次消费，返回短时、一次性的密码重置票据。",
        {"resetTicket": "example-ticket-id.example-one-time-secret", "expiresAt": "2026-10-22T04:10:00Z"},
        {"challengeId": CHALLENGE_ID, "code": "123456"}, notes=("resetTicket 只返回一次，按敏感凭证处理，不写日志或长时间持久化。",)),
    "resetPassword": Example("通过票据重置密码", "消费重置票据、更新密码摘要和认证代际；成功后旧移动会话失效，用户需重新登录。", None,
        {"resetTicket": "example-ticket-id.example-one-time-secret", "password": "NewExamplePassword123", "confirmedPassword": "NewExamplePassword123"},
        notes=("票据过期或已消费返回 FAFA-41002。", "即使 Redis 撤销清理失败，数据库认证代际仍拒绝旧会话。")),
    "getBootstrap": Example("读取启动状态", "匿名读取引导状态；已登录时读取用户和活动宠物摘要、当前选择及可用功能。",
        {"authenticated": True, "user": USER, "pets": [PET], "currentPetId": PET_ID, "nextRoute": "HOME",
         "features": {"ai": False, "community": False, "petManagement": True}},
        notes=("匿名：user=null、pets=[]、currentPetId=null、nextRoute=WELCOME。", "有效账号没有活动宠物：nextRoute=NO_PET；有活动宠物：HOME。", "pets 返回首批默认页大小的活动宠物，并确保包含当前选择；完整列表使用 GET /pets 分页。", "提供了过期或被撤销的令牌时会返回认证错误，不静默冒充匿名状态。")),
    "getMyProfile": Example("读取本人资料", "从认证上下文读取本人基础资料，不接受客户端传 userId 指定他人身份。", USER),
    "listMySessions": Example("读取当前移动设备", "返回当前有效移动会话对应的设备摘要。", [DEVICE]),
    "listPetSpecies": Example("查询宠物物种", "读取本期支持的宠物物种编码；本期字典查询需要登录。", ["CAT", "DOG", "OTHER"]),
    "listPetBreeds": Example("查询宠物品种", "按物种、名称关键词和游标读取处于启用状态的服务端品种字典。", {"items": [BREED], "nextCursor": None},
        query={"species": "CAT", "q": "未知", "limit": 20}, notes=("品种编码来自字典，不接受客户端随意编造；停用品种不再出现在新增选择中，但旧档案仍可读取。",)),
    "listAccessiblePets": Example("查询本人宠物列表", "按状态和稳定 ID 游标查询当前用户拥有的宠物。", {"items": [PET], "nextCursor": None}, query={"status": "ACTIVE", "limit": 20},
        notes=("省略 status 时为 ACTIVE；允许显式查询 ARCHIVED、DELETION_PENDING，禁止查询 PURGING、DELETED。", "本期仅支持主人数据；共同照护授权在后续工作包实现。",)),
    "createPet": Example("创建宠物档案", "只填写昵称和物种即可建档；相同用户、操作、幂等键与相同请求返回首次创建结果。", PET,
        {"nickname": "雪球", "species": "CAT"}, notes=("头像、三视图、初次体重暂未接入本批写接口；传入未知字段返回 400。", "没有当前选择时，首次创建会设置当前宠物；没有默认宠物时同时设为默认。", "昵称创建时去除首尾空白；sex、neuteredStatus 省略时为 UNKNOWN。")),
    "getPet": Example("读取宠物详情", "检查主人归属后读取档案；主人可读取归档或删除等待中的状态，已删除或他人资源返回 404。", PET, path={"id": PET_ID}),
    "updatePet": Example("修改宠物档案", "仅允许更新 ACTIVE 宠物；按提交的 version 检查并发修改。省略字段保持原值，显式清空使用 clearFields。",
        dict(PET, profile=dict(PROFILE, nickname="雪球宝宝"), version=2), {"version": 1, "nickname": "雪球宝宝"}, path={"id": PET_ID},
        notes=("清空字段可选 BREED、BIRTH_DATE、ESTIMATED_BIRTH_MONTH、ARRIVED_ON；同字段同时赋值与清空返回 400。", "birthDate 和 estimatedBirthMonth 二选一；改为估算月份时需同时清空 BIRTH_DATE。", "归档状态只读，版本过旧返回 FAFA-40910。")),
    "archivePet": Example("归档宠物", "将 ACTIVE 宠物转为 ARCHIVED，并清除该宠物在当前/默认选择中的引用。", dict(PET, status="ARCHIVED", version=2),
        {"version": 1}, path={"id": PET_ID}, notes=("归档没有自动删除期限；当前提醒模块尚未接入，未来接入时需在此生命周期边界停止任务展开。",)),
    "restorePet": Example("恢复归档宠物", "将 ARCHIVED 档案恢复为 ACTIVE；不自动恢复以前的偏好和后续暂停任务。", dict(PET, version=3),
        {"version": 2}, path={"id": PET_ID}),
    "previewPetDeletion": Example("预览宠物删除影响", "返回当前真实已建模块中的资源计数和删除等待策略。", {"petId": PET_ID, "status": "ACTIVE", "waitingDays": 15, "resourceCounts": {"pet": 1}}, path={"id": PET_ID},
        notes=("本批仅统计 pet；媒体、记录、提醒等模块尚未实现，因此不伪造它们的零值计数。",)),
    "requestPetDeletion": Example("申请删除宠物", "对 ACTIVE 或 ARCHIVED 宠物创建删除申请，记录原状态并进入 DELETION_PENDING，清除当前/默认选择。",
        dict(PET, status="DELETION_PENDING", version=2, deleteAfter="2026-11-06T04:00:00Z"),
        {"version": 1, "acknowledgement": True}, path={"id": PET_ID}, notes=("acknowledgement 必须为 true；等待期默认 15 天，响应以实际配置计算。", "本批交付申请和撤回；到期物理清理作业安排在 WP09，不能将申请成功理解为文件已清除。")),
    "withdrawPetDeletion": Example("撤回宠物删除申请", "仅在等待期内、尚未开始清理时撤回，并恢复申请前的 ACTIVE 或 ARCHIVED 状态。", dict(PET, version=3),
        {"version": 2}, path={"id": PET_ID}, notes=("deleteAfter 到期即拒绝撤回；与清理作业竞争的状态迁移在 WP09 补齐。",)),
    "getPetPreferences": Example("读取宠物选择偏好", "读取本人显式保存的当前宠物、默认宠物和偏好版本。", {"currentPetId": PET_ID, "defaultPetId": PET_ID, "version": 2},
        notes=("当前宠物供本次使用切换，默认宠物用于启动回退，两者分别保存。尚无偏好时字段为空、version=1。",)),
    "updatePetPreferences": Example("设置宠物选择偏好", "整体替换两个偏好字段；所选资源必须属于本人且为 ACTIVE，使用偏好自身的 version。",
        {"currentPetId": PET_ID, "defaultPetId": PET_ID, "version": 3}, {"version": 2, "currentPetId": PET_ID, "defaultPetId": PET_ID},
        notes=("这是 PUT：省略或传 null 会清除对应偏好；与 PATCH 宠物资料的省略语义不同。",)),
    "getMyUsage": Example("查询本人存储额度", "读取本人额度账户；不存在时按初始空间配置初始化。", [QUOTA], query={"bucket": "STORAGE_BYTES"},
        notes=("默认初始赠送量为 0；示例展示审批赠送 1000 字节后的账户。", "available=granted-used-reserved；所有字节数使用十进制字符串。", "本期只支持永久 STORAGE_BYTES 额度；暂不支持 AI_POINTS、购买、到期赠送及负数调整。")),
    "listMyUsageLedger": Example("查询本人额度流水", "分页返回本人的赠送、预占、结算和释放流水，不接受其他用户标识。",
        {"items": [{"id": "66000000-0000-4000-8000-000000000001", "operationId": "request:" + REQUEST_ID,
                    "entryType": "GRANT", "amount": "1000", "reason": "批准开发测试空间", "createdAt": "2026-10-22T04:00:00Z"}], "nextCursor": None},
        query={"bucket": "STORAGE_BYTES", "limit": 20}),
    "requestFreeQuota": Example("申请免费存储额度", "创建待管理员审批的存储额度申请；幂等键防止重复点击创建多条申请。", QUOTA_REQUEST,
        {"bucket": "STORAGE_BYTES", "amount": "1000", "reason": "申请开发测试空间"}, notes=("申请不会立即增加可用额度。",)),
    "listMyQuotaRequests": Example("查询本人额度申请", "分页查询本人申请及审批结果。", {"items": [QUOTA_REQUEST], "nextCursor": None}, query={"limit": 20}),
    "createAdminSession": Example("管理员登录", "使用管理库中的独立账号登录；返回随机管理令牌，数据库仅保存令牌摘要。",
        {"admin": PRINCIPAL, "token": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", "expiresAt": "2026-10-22T05:00:00Z"},
        {"username": "example-admin", "password": "ExampleAdminPassword123"}, notes=("示例账号并非系统预置账号；首次管理员必须通过初始化命令创建。", "默认会话 3600 秒；默认每账号及直连 IP 在 900 秒窗口最多 10 次尝试。经同一 Gateway 的管理请求共享该入口 IP 限额。", "管理令牌与移动端令牌完全隔离；角色权限在每次请求查询。")),
    "deleteAdminSession": Example("管理员退出", "删除当前管理令牌对应的服务器会话。", None),
    "getAdminProfile": Example("读取管理员及权限", "读取当前有效管理员身份及实时生效的权限集合。", PRINCIPAL),
    "listAdminUsers": Example("查询脱敏用户列表", "经签名 RPC 查询业务账号摘要，不读取私人宠物、记录或媒体正文。", {"items": [ADMIN_USER], "nextCursor": None}, query={"limit": 20}),
    "getAdminUser": Example("查询脱敏用户详情", "经签名 RPC 按用户标识读取脱敏账号、昵称、状态和账号版本。", ADMIN_USER, path={"id": USER_ID},
        notes=("此 version 为账号版本；与移动端 /me 返回的个人资料版本分别维护。",)),
    "setAdminUserStatus": Example("限制或恢复用户账号", "校验 USER_WRITE，经 RPC 修改账号状态及认证代际；管理操作意图先审计，业务和操作收据一起提交。",
        dict(ADMIN_USER, status="DISABLED", version=3), {"status": "DISABLED", "reason": "开发环境风控验收", "version": 2}, path={"id": USER_ID},
        notes=("ACTIVE 与 DISABLED 两种目标状态；恢复账号也不复活旧移动令牌。",)),
    "listAdminQuotaRequests": Example("查询额度申请列表", "经 RPC 按申请状态和游标读取待审或已审申请。", {"items": [QUOTA_REQUEST], "nextCursor": None}, query={"state": "PENDING", "limit": 20}),
    "decideAdminQuotaRequest": Example("审批存储额度申请", "批准时在同一 Biz 事务中增加额度、写赠送流水并更新申请；拒绝时只记录审批决定。",
        dict(QUOTA_REQUEST, state="APPROVED", grantedAmount="1000", decisionReason="批准开发测试空间", operatorId=ADMIN_ID, version=2),
        {"approved": True, "amount": "1000", "reason": "批准开发测试空间", "version": 1}, path={"id": REQUEST_ID},
        notes=("approved=true 时 amount 必填且为正整数字节串；false 时 amount 必须省略或为 null。", "不同审批人再次处理已审批申请返回冲突；相同操作幂等重试返回首次结果，不重复赠送。",)),
    "grantAdminUserQuota": Example("直接赠送存储额度", "校验 QUOTA_WRITE 后，经 RPC 向指定用户发放永久存储额度并写双方审计。", QUOTA,
        {"bucket": "STORAGE_BYTES", "amount": "1000", "entryType": "GRANT", "reason": "开发测试赠送"}, path={"id": USER_ID},
        notes=("entryType 仅接受 GRANT，amount 为正数；不支持直接改余额、负数冲正或支付。", "网络结果未知时审计为 UNKNOWN；客户端保留原幂等键重试，不另开操作。")),
    "listAdminPetBreeds": Example("后台查询品种字典", "按物种、名称与游标查询字典，包含 INACTIVE 项。", {"items": [BREED], "nextCursor": None}, query={"species": "CAT", "limit": 20}),
    "createAdminPetBreed": Example("创建品种字典项", "校验 BREED_WRITE 后经 RPC 创建稳定编码的品种字典项。",
        {"id": "CAT_BRITISH_SHORTHAIR", "species": "CAT", "name": "英国短毛猫", "status": "ACTIVE", "version": 1},
        {"id": "CAT_BRITISH_SHORTHAIR", "species": "CAT", "name": "英国短毛猫", "status": "ACTIVE", "reason": "维护基础品种字典"},
        notes=("新增时不得提供 version；相同物种下名称唯一，字典编码唯一。",)),
    "updateAdminPetBreed": Example("修改或停用品种字典项", "校验当前字典版本后修改名称或状态；历史已引用的品种仍可显示。",
        {"id": "CAT_BRITISH_SHORTHAIR", "species": "CAT", "name": "英国短毛猫", "status": "INACTIVE", "version": 2},
        {"id": "CAT_BRITISH_SHORTHAIR", "species": "CAT", "name": "英国短毛猫", "status": "INACTIVE", "version": 1, "reason": "暂停新增选择"},
        path={"id": "CAT_BRITISH_SHORTHAIR"}, notes=("请求体 id 必须等于路径 id；species 不可改成其他物种。", "请求必须包含完整字典字段；本接口不是只传一个 status 的局部补丁。")),
    "listAdminAuditLogs": Example("查询管理操作审计", "分页读取操作者、操作、目标、原因、执行状态、结果码和请求标识；不返回签名、密码或令牌。",
        {"items": [{"operationId": "admin:" + "a" * 64, "operatorId": ADMIN_ID, "action": "QUOTA_GRANT", "targetId": USER_ID,
                    "reason": "开发测试赠送", "state": "COMPLETED", "resultCode": "0", "requestId": "example-request-001"}], "nextCursor": None},
        query={"limit": 20}, notes=("PENDING 表示已记录意图，UNKNOWN 表示网络故障导致结果待确认；同键重试后可转为 COMPLETED。",)),
}
