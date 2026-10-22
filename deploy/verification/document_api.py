"""由已提交 OpenAPI 与业务示例生成逐接口中文文档，并校验示例覆盖范围。"""
from __future__ import annotations

import json
from pathlib import Path
from typing import Any
from urllib.parse import urlencode

import yaml

from api_examples import EXAMPLES, Example

ROOT = Path(__file__).resolve().parents[2]
DESTINATION = ROOT / "docs" / "api"
FIELDS = {
    "fileName": "原文件名，最多 200 字符；只校验并参与幂等摘要，本批不持久化、不用于构造对象键。",
    "mime": "声明或核验后的媒体类型；本批为 image/jpeg、image/png。",
    "bytes": "文件字节数的十进制字符串；最大值受服务端图片上限控制。",
    "sha256": "文件真实字节的 SHA-256 小写十六进制摘要，固定 64 字符。",
    "state": "上传状态 INITIATED、READY、CANCELLED、FAILED、EXPIRED；READY 之后才是可用素材。",
    "assetId": "READY 后生成的素材 ID；其他状态为 null。",
    "sessionId": "本次上传会话标识，后续查询/完成/取消使用。",
    "uploadUrl": "对象存储 POST 表单目标 URL，不携带业务 Bearer 令牌。",
    "formFields": "原样提交的短期签名表单键值；file 文件字段最后附加。",
    "mediaId": "同一宠物范围内的 READY 素材 ID；未设置头像时可为空。",
    "width": "服务端解码核验的原图片宽度，像素。",
    "height": "服务端解码核验的原图片高度，像素。",
    "url": "默认 60 秒有效的私有 PNG 预览地址，不能长期缓存或写日志。",
    "code": "稳定业务编码；成功为字符串 0，失败按 FAFA-* 分支。验证码字段例外，见请求用途。",
    "message": "可展示的安全提示；客户端不得解析消息文本判断业务状态。",
    "data": "本接口的业务数据；无返回数据或部分失败时为 null。",
    "requestId": "请求关联标识，用于串联 Gateway、服务与审计；不包含敏感凭证。",
    "id": "资源的稳定标识；按接口资源类型解释，不能作为授权凭据。",
    "email": "邮箱，最多 254 字符；服务端去首尾空白并规范化，所有账号入口规则一致。",
    "password": "明文密码仅通过受保护的请求传输，服务端只保存 Argon2id 摘要；移动端新密码为 8—128 码点且含字母和数字。",
    "confirmedPassword": "再次输入的新密码，必须与 password 完全一致，不持久化。",
    "purpose": "验证码用途：REGISTER 注册、RESET_PASSWORD 找回密码。",
    "challengeId": "服务端创建的邮件挑战标识，必须与邮箱、用途及验证码对应。",
    "resetTicket": "一次性重置票据；仅使用验证接口本次返回的实际值，过期/消费后失效。",
    "legalVersions": "实际同意的协议版本，必须包含 TERMS、PRIVACY 两项。",
    "TERMS": "已发布且生效的用户协议版本。",
    "PRIVACY": "已发布且生效的隐私政策版本。",
    "device": "本次登录的移动设备摘要。",
    "deviceId": "客户端持久化的设备标识，必填，最多 100 字符。",
    "deviceName": "供用户识别的设备名称，必填，最多 100 字符。",
    "user": "当前移动账号资料；匿名启动或未提供该分支时为空。",
    "session": "移动登录会话；注册已提交而会话写入失败时为空。",
    "token": "示意令牌；调用时必须替换为登录接口实际返回的值，不放入 URL 或日志。",
    "tokenType": "移动令牌类型，当前为 Bearer。",
    "expiresIn": "移动会话有效秒数，以服务端配置及实际响应为准。",
    "expiresAt": "UTC ISO-8601 过期时刻；不得按服务器本地时区推断。",
    "resendAfter": "建议再次发送验证码前等待的秒数。",
    "nextAction": "注册后动作：NO_PET 进入无宠物建档入口，LOGIN 进入登录恢复。",
    "nickname": "昵称；宠物昵称必填、最多 30 字符，创建和更新时去首尾空白；个人资料昵称按身份模块规则。",
    "signature": "用户签名，可为空；本批只读，编辑在后续设置功能实现。",
    "avatarMediaId": "头像媒体标识，可为空；本批暂未接入头像上传。",
    "timezone": "IANA 时区，例如 Asia/Hong_Kong；用户日期校验按其时区计算。",
    "version": "正整数乐观锁版本；写入时提交最近读取的对应资源版本，冲突需重新读取。",
    "authenticated": "当前是否有有效移动账号身份。",
    "pets": "启动首批活动宠物摘要；包含当前选择，完整列表另用分页宠物接口。",
    "currentPetId": "当前选择的本人活动宠物标识，可为空。",
    "defaultPetId": "启动回退使用的本人默认活动宠物标识，可为空；与当前选择分别保存。",
    "nextRoute": "启动目标：WELCOME、NO_PET、HOME。",
    "features": "后端已实现的功能开关，不替代服务端权限检查。",
    "ai": "AI 功能是否可用，本批为 false。",
    "community": "社区功能是否可用，本批为 false。",
    "petManagement": "宠物管理能力，本批完成后为 true。",
    "type": "协议类型，TERMS 或 PRIVACY。",
    "title": "已发布协议的标题。",
    "body": "协议正文；以实际发布内容为准。",
    "contentHash": "协议内容摘要，用于核对发布内容。",
    "effectiveAt": "协议生效时刻，UTC ISO-8601。",
    "species": "物种：CAT 猫、DOG 犬、OTHER 其他；品种必须匹配物种。",
    "breedId": "品种字典编码；可为空；新选择必须启用，已停用的原引用仍可保留。",
    "sex": "性别：MALE、FEMALE、UNKNOWN；省略创建时为 UNKNOWN。",
    "neuteredStatus": "绝育状态：YES、NO、UNKNOWN；省略创建时为 UNKNOWN。",
    "birthDate": "精确出生日期，YYYY-MM-DD；不晚于用户本地今天，与估算出生月份二选一。",
    "estimatedBirthMonth": "估算出生月份，YYYY-MM；不晚于用户本地当前月份，与精确生日二选一。",
    "arrivedOn": "到家日期，YYYY-MM-DD；不晚于用户本地今天，不能早于已知出生日期/月份。",
    "clearFields": "PATCH 显式清空字段：BREED、BIRTH_DATE、ESTIMATED_BIRTH_MONTH、ARRIVED_ON；同字段不能同时赋值与清空。",
    "ownerUserId": "主人账号标识；本期资源只允许该主人访问。",
    "userId": "额度申请所属的业务账号标识；普通用户仅能读取本人申请。",
    "createdBy": "创建者标识，便于以后接入协作审计。",
    "role": "当前用户与宠物的关系，本期为 OWNER。",
    "profile": "宠物的可编辑资料。",
    "status": "资源状态；具体枚举以本字段 Schema 和对应接口定义为准。",
    "deleteAfter": "删除申请到期的 UTC 时刻；不在等待期时为 null。本批未执行物理清理。",
    "acknowledgement": "是否确认了解删除影响，申请删除时必须为 true。",
    "petId": "被操作的宠物标识。",
    "waitingDays": "当前配置的删除等待天数，默认 15。",
    "resourceCounts": "当前已实现资源类型与实际数量的映射。",
    "pet": "当前宠物档案数量。",
    "items": "本页资源集合；无数据时为空数组。",
    "nextCursor": "下一页的不透明游标；为 null 表示已到末页。",
    "cursor": "上次响应的 nextCursor；首次查询省略。本期按稳定 ID 升序分页，不承诺按创建时间排序。",
    "limit": "每页数量；省略或 0 使用默认 20，范围 1—100；超出返回 400。",
    "q": "品种名称关键词，最多 80 字符；按字面量匹配，百分号与下划线不作为通配符。",
    "bucket": "额度类别，本期仅接受 STORAGE_BYTES。",
    "granted": "已赠送的永久存储字节数，十进制字符串。",
    "used": "已实际结算的存储字节数，十进制字符串。",
    "reserved": "尚未结算的上传预占字节数，十进制字符串。",
    "available": "可用字节，等于 granted-used-reserved，十进制字符串。",
    "amount": "正整数字节数的十进制字符串，最大为 Java long 上限；拒绝负数、小数、前导零或溢出值。",
    "reason": "申请、审批或调整原因，非空，最多 500 字符。",
    "state": "额度申请为 PENDING/APPROVED/REJECTED；审计为 PENDING/UNKNOWN/COMPLETED/REJECTED。",
    "grantedAmount": "审批实际赠送的字节串；待审或拒绝时为 null。",
    "decisionReason": "管理员给出的审批原因；待审时为 null。",
    "operatorId": "完成后台操作的管理员标识；尚未审批或系统事件时可为空。",
    "approved": "审批决定：true 批准且 amount 必填；false 拒绝且 amount 必须为空或省略。",
    "entryType": "流水类型：GRANT 赠送、RESERVE 预占、SETTLE 结算、RELEASE 释放；直接调整请求仅支持 GRANT。",
    "operationId": "服务端业务操作标识，用于幂等、账本与审计关联；客户端通过 Idempotency-Key 触发稳定操作标识。",
    "createdAt": "流水产生时刻，UTC ISO-8601。",
    "admin": "当前管理员身份及其有效权限。",
    "username": "管理员用户名，3—64 位，只允许字母、数字、点、下划线和连字符；区分大小写。",
    "permissions": "实时计算的权限编码集合；角色变更对已有令牌的后续请求立即生效。",
    "maskedEmail": "脱敏邮箱，仅保留必要识别片段。",
    "name": "品种显示名称，非空，最多 80 字符；同一物种下唯一。",
    "action": "管理动作：USER_STATUS、QUOTA_DECISION、QUOTA_GRANT、SAVE_BREED。",
    "targetId": "本次管理动作的目标账号、申请或字典编码。",
    "resultCode": "管理操作最终/最近确认的业务结果码，成功为 0，尚未确认时可能为空。",
    "fieldErrors": "字段错误集合；不回显提交的密码、验证码等敏感值。",
    "field": "失败字段路径。",
}
MODEL_FIELDS = {
    "fileName": "原文件名，最多 200 字符；只校验并参与幂等摘要，本批不持久化、不用于构造对象键。",
    "mime": "声明或核验后的媒体类型；本批为 image/jpeg、image/png。",
    "bytes": "文件字节数的十进制字符串；最大值受服务端图片上限控制。",
    "sha256": "文件真实字节的 SHA-256 小写十六进制摘要，固定 64 字符。",
    "state": "上传状态 INITIATED、READY、CANCELLED、FAILED、EXPIRED；READY 之后才是可用素材。",
    "assetId": "READY 后生成的素材 ID；其他状态为 null。",
    "sessionId": "本次上传会话标识，后续查询/完成/取消使用。",
    "uploadUrl": "对象存储 POST 表单目标 URL，不携带业务 Bearer 令牌。",
    "formFields": "原样提交的短期签名表单键值；file 文件字段最后附加。",
    "mediaId": "同一宠物范围内的 READY 素材 ID；未设置头像时可为空。",
    "width": "服务端解码核验的原图片宽度，像素。",
    "height": "服务端解码核验的原图片高度，像素。",
    "url": "默认 60 秒有效的私有 PNG 预览地址，不能长期缓存或写日志。",
    "Breed.id": "稳定的品种编码，1—64 位大写字母、数字和下划线，例如 CAT_UNKNOWN。",
    "Breed.status": "ACTIVE 启用、INACTIVE 停用；停用不删除旧引用。",
    "BreedWrite.id": "稳定品种编码；新增时唯一，修改时必须与路径一致。",
    "BreedWrite.version": "新增必须省略；修改时必填且大于 0。",
    "PetView.status": "ACTIVE 活跃、ARCHIVED 归档、DELETION_PENDING 删除等待、PURGING 清理中、DELETED 已删除。",
    "StatusChange.status": "账号目标状态，仅接受 ACTIVE、DISABLED。",
    "AdminUser.status": "业务账号状态，ACTIVE 或 DISABLED。",
    "UserSummary.status": "业务账号状态，ACTIVE 或 DISABLED。",
    "EmailChallengeRequest.code": "该请求没有 code 字段；实际验证码只能从邮件获得。",
    "RegistrationRequest.code": "本 challenge 对应邮件中的 6 位数字验证码；示例值不可用于真实环境。",
    "VerifyResetRequest.code": "找回密码邮件中的 6 位数字验证码。",
    "Credentials.password": "管理员密码，最长 128 字符；首次初始化要求 12—128 位且含字母和数字。",
}


def resolve(schema: dict[str, Any], schemas: dict[str, Any]) -> tuple[dict[str, Any], str]:
    if "$ref" in schema:
        name = schema["$ref"].rsplit("/", 1)[-1]
        return schemas[name], name
    if "allOf" in schema:
        resolved, name = resolve(schema["allOf"][0], schemas)
        return dict(resolved, nullable=schema.get("nullable", False)), name
    return schema, ""


def rows(schema: dict[str, Any], schemas: dict[str, Any], prefix: str = "", response: bool = False,
         seen: frozenset[str] = frozenset()) -> list[str]:
    resolved, model = resolve(schema, schemas)
    if model and model in seen:
        raise ValueError(f"不支持递归文档类型：{model}")
    seen = seen | ({model} if model else set())
    output: list[str] = []
    for name, value in resolved.get("properties", {}).items():
        current, current_model = resolve(value, schemas)
        path = f"{prefix}.{name}" if prefix else name
        type_name = current.get("type", "object")
        required = "—" if response else "是" if name in resolved.get("required", []) else "否"
        description = MODEL_FIELDS.get(f"{model}.{name}", FIELDS.get(name))
        if description is None:
            raise ValueError(f"缺少字段中文定义：{model}.{name}")
        limits = []
        if "enum" in current:
            limits.append("枚举：" + "、".join(str(item) for item in current["enum"]))
        for constraint, label in (("maxLength", "最大长度"), ("minLength", "最小长度"), ("minimum", "最小值"), ("maximum", "最大值")):
            if constraint in current:
                limits.append(f"{label} {current[constraint]}")
        if limits:
            description += " " + "；".join(limits) + "。"
        if current.get("nullable") or name in {"nextCursor", "deleteAfter", "signature", "avatarMediaId", "breedId", "birthDate", "estimatedBirthMonth", "arrivedOn", "currentPetId", "defaultPetId", "grantedAmount", "decisionReason", "operatorId"}:
            type_name += " / null"
        output.append(f"| `{path}` | {type_name} | {required} | {description.replace('|', '或')} |")
        if current.get("properties"):
            output.extend(rows(value, schemas, path, response, frozenset(seen)))
        elif current.get("type") == "array":
            item = current.get("items", {})
            inner, _ = resolve(item, schemas)
            if inner.get("properties"):
                output.extend(rows(item, schemas, path + "[]", response, frozenset(seen)))
    return output


def table(lines: list[str]) -> str:
    return "| 字段 | 类型 | 必填 | 说明 |\n|---|---|---|---|\n" + "\n".join(lines) + "\n"


def pretty(value: Any) -> str:
    return "```json\n" + json.dumps(value, ensure_ascii=False, indent=2) + "\n```\n"


def render(method: str, path: str, operation: dict[str, Any], document: dict[str, Any], example: Example, admin: bool) -> str:
    identifier = operation["operationId"]
    prefix = "/api/admin/v1" if admin else "/api/v1"
    authentication = "匿名" if operation.get("security") == [] else "管理员 Bearer 令牌" if admin else "移动端 Bearer 令牌"
    if identifier == "getBootstrap":
        authentication = "可匿名；有会话时携带移动端 Bearer 令牌"
    successful = next(code for code in operation["responses"] if code.startswith("2"))
    lines = [f"## {example.name}（{identifier}）\n", f"接口定义：`{method.upper()} {prefix}{path}`。{example.definition}\n",
             f"认证：{authentication}。成功 HTTP 状态：`{successful}`。\n"]
    if operation.get("x-permission"):
        lines.append(f"权限码：`{operation['x-permission']}`。\n")
    pages = operation.get("x-pages", [])
    if pages:
        lines.append("页面关联：" + "、".join(f"`{page}`" for page in pages) + "。\n")
    if example.notes:
        lines.append("\n".join(f"- {note}" for note in example.notes) + "\n")
    lines.append("### 传参 JSON 示例\n")
    if example.body is not None:
        lines.append(pretty(example.body))
    else:
        parameters = {}
        if example.path:
            parameters["path"] = example.path
        if example.query:
            parameters["query"] = example.query
        lines.append(pretty(parameters))
        lines.append("本接口没有 JSON 请求体。上方 `path`、`query` 仅解释路径和查询参数，实际按 URL 传递。\n" if parameters else "本接口没有 JSON 请求体或业务参数；实际只发送认证等必要请求头。\n")
    lines.append("### 传参字段说明\n")
    headers = ["| `X-Request-ID` | 请求头 / string | 否 | 安全关联标识，最多 80 位字母、数字、点、下划线、连字符；非法或缺失时生成新值。 |"]
    if authentication != "匿名":
        headers.append(f"| `Authorization` | 请求头 / string | {'否' if identifier == 'getBootstrap' else '是'} | `Bearer <实际令牌>`；{authentication}。 |")
    for parameter in operation.get("parameters", []):
        name = parameter["name"]
        description = ("幂等键，1—128 位字母、数字、点、下划线、冒号、连字符；网络重试复用原值，同键改内容返回 409。"
                       if name == "Idempotency-Key" else FIELDS.get(name, "路径资源标识，以接口目标类型解释。"))
        headers.append(f"| `{name}` | {parameter['in']} / {parameter.get('schema', {}).get('type', 'string')} | {'是' if parameter.get('required') else '否'} | {description} |")
    schemas = document["components"]["schemas"]
    request_schema = operation.get("requestBody", {}).get("content", {}).get("application/json", {}).get("schema")
    if request_schema:
        headers.extend(rows(request_schema, schemas))
    lines.append(table(headers))
    lines.append("### 响应 JSON 示例\n")
    envelope = {"code": "0", "message": "OK", "data": example.data, "requestId": "example-request-001"}
    lines.append(pretty(envelope))
    lines.append("示例使用虚构标识和凭证，表示一种成功状态；调用时以实际返回的 ID、版本、时间和令牌为准。\n")
    lines.append("### 响应字段说明\n")
    content = operation["responses"][successful]["content"]
    response_schema = content.get("application/json", content.get("*/*"))["schema"]
    lines.append(table(rows(response_schema, schemas, response=True)))
    lines.append("### 示例 curl（Bash / Git Bash / WSL）\n")
    actual_path = prefix + path
    for key, value in example.path.items():
        actual_path = actual_path.replace("{" + key + "}", value)
    if example.query:
        actual_path += "?" + urlencode(example.query)
    curl = [f'curl --request {method.upper()} "${{FAFA_BASE_URL}}{actual_path}"', '  --header "X-Request-ID: example-request-001"']
    if authentication != "匿名":
        variable = "FAFA_ADMIN_TOKEN" if admin else "FAFA_USER_TOKEN"
        curl.append(f'  --header "Authorization: Bearer ${{{variable}}}"')
    if operation.get("x-idempotency-required"):
        curl.append(f'  --header "Idempotency-Key: example-{identifier}-001"')
    if example.body is not None:
        curl.append('  --header "Content-Type: application/json"')
        curl.append("  --data-raw '" + json.dumps(example.body, ensure_ascii=False, separators=(",", ":")) + "'")
    lines.append("```bash\n" + " \\\n".join(curl) + "\n```\n")
    return "\n".join(lines)


def main() -> None:
    DESTINATION.mkdir(parents=True, exist_ok=True)
    documents: dict[str, list[str]] = {"账号与会话接口.md": [], "宠物与额度接口.md": [], "管理端接口.md": [], "媒体接口.md": []}
    index: list[str] = []
    collection: dict[str, Any] = {}
    for admin, contract in ((False, "app-v1.yaml"), (True, "admin-v1.yaml")):
        document = yaml.safe_load((ROOT / "contracts" / "openapi" / contract).read_text(encoding="utf-8"))
        for path, methods in document["paths"].items():
            for method, operation in methods.items():
                identifier = operation["operationId"]
                example = EXAMPLES[identifier]
                filename = "媒体接口.md" if operation.get("x-stage") == "WP04-A" else "管理端接口.md" if admin else "宠物与额度接口.md" if operation.get("x-stage") == "WP02-WP03" else "账号与会话接口.md"
                documents[filename].append(render(method, path, operation, document, example, admin))
                route = ("/api/admin/v1" if admin else "/api/v1") + path
                index.append(f"| {example.name} | `{method.upper()} {route}` | `{identifier}` | [{filename[:-3]}]({filename}) |")
                collection[identifier] = {"method": method.upper(), "path": route, "pathParameters": example.path,
                                          "query": example.query, "requestBody": example.body,
                                          "response": {"code": "0", "message": "OK", "data": example.data, "requestId": "example-request-001"}}
    if set(collection) != set(EXAMPLES):
        raise ValueError("业务示例与接口清单不一致")
    for filename, entries in documents.items():
        header = f"# {filename[:-3]}\n\n> 更新日期：2026-10-22。按当前已实现代码与 OpenAPI 生成；示例是脱敏的结构示意。\n\n"
        header += "通用约定、错误码和环境变量见[接口文档总览](接口文档总览.md)。以下每个接口均独立列出定义、参数、响应和 curl。\n\n"
        (DESTINATION / filename).write_text(header + "\n".join(entries), encoding="utf-8")
    (ROOT / "contracts" / "examples" / "http-operations.json").parent.mkdir(parents=True, exist_ok=True)
    (ROOT / "contracts" / "examples" / "http-operations.json").write_text(json.dumps(collection, ensure_ascii=False, indent=2), encoding="utf-8")
    (DESTINATION / "接口目录.md").write_text("# 已实现接口目录\n\n共 " + str(len(index)) + " 个业务 HTTP 接口。\n\n| 接口名称 | 定义 | operationId | 详细文档 |\n|---|---|---|---|\n" + "\n".join(index) + "\n", encoding="utf-8")


if __name__ == "__main__":
    main()
