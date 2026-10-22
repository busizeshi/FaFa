"""将测试导出的真实 SpringDoc 接口合并为可导入的中文契约。"""
from __future__ import annotations

import copy
import json
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[2]
CONTRACTS = ROOT / "contracts" / "openapi"
APP_METADATA = {
    "listPetSpecies": ("查询支持的物种", ["pet.create", "pet.edit"]),
    "listPetBreeds": ("按物种搜索品种字典", ["pet.breed"]),
    "listAccessiblePets": ("查询本人宠物列表", ["pet.list", "pet.picker"]),
    "createPet": ("创建宠物档案", ["pet.create"]),
    "getPet": ("查询宠物档案", ["pet.detail"]),
    "updatePet": ("修改宠物资料", ["pet.edit"]),
    "archivePet": ("归档宠物", ["pet.list", "pet.detail"]),
    "restorePet": ("恢复归档宠物", ["pet.list"]),
    "previewPetDeletion": ("预览本批已有资源的删除影响", ["pet.deletion"]),
    "requestPetDeletion": ("申请删除宠物并进入等待期", ["pet.deletion"]),
    "withdrawPetDeletion": ("等待期内撤回删除申请", ["pet.deletion"]),
    "getPetPreferences": ("查询当前和默认宠物偏好", ["pet.picker", "me.preferences"]),
    "updatePetPreferences": ("整体设置当前和默认宠物；空值清除对应选择", ["pet.picker", "me.preferences"]),
    "getMyUsage": ("查询本人的存储额度", ["me.usage"]),
    "listMyUsageLedger": ("查询本人的存储账本", ["me.usage"]),
    "requestFreeQuota": ("提交免费存储额度申请", ["me.grantRequest"]),
    "listMyQuotaRequests": ("查询本人的额度申请状态", ["me.usage"]),
}
ADMIN_METADATA = {
    "createAdminSession": ("管理员登录", ["admin.login"], None),
    "deleteAdminSession": ("管理员退出", ["admin.logout"], None),
    "getAdminProfile": ("读取当前管理员及有效权限", ["admin.profile"], None),
    "listAdminUsers": ("分页查询脱敏用户", ["admin.users"], "USER_READ"),
    "getAdminUser": ("读取脱敏用户详情", ["admin.users"], "USER_READ"),
    "setAdminUserStatus": ("限制或恢复用户并撤销旧会话", ["admin.users"], "USER_WRITE"),
    "listAdminQuotaRequests": ("查询额度申请", ["admin.quotaRequests"], "QUOTA_READ"),
    "decideAdminQuotaRequest": ("批准或拒绝存储额度申请", ["admin.quotaRequests"], "QUOTA_WRITE"),
    "grantAdminUserQuota": ("赠送永久存储字节额度", ["admin.quotaAdjustments"], "QUOTA_WRITE"),
    "listAdminPetBreeds": ("查询包含停用品种的字典", ["admin.breeds"], "BREED_READ"),
    "createAdminPetBreed": ("创建品种字典项", ["admin.breeds"], "BREED_WRITE"),
    "updateAdminPetBreed": ("按版本修改或停用品种字典项", ["admin.breeds"], "BREED_WRITE"),
    "listAdminAuditLogs": ("查询管理操作及未知结果审计", ["admin.audit"], "AUDIT_READ"),
}
WP01_SCHEMAS = {"Device", "EmailChallengeRequest", "LegalVersions", "RegistrationRequest", "LoginRequest",
                "VerifyResetRequest", "ResetPasswordRequest", "User", "Grant", "ChallengeReceipt", "RegistrationResponse",
                "LoginResponse", "ResetTicketResponse", "VoidResponse", "UserResponse", "SessionsResponse", "BootstrapResponse",
                "LegalDocument", "LegalResponse", "FieldError", "ErrorResponse"}


def enrich(operation: dict[str, Any], metadata: tuple[Any, ...], stage: str) -> None:
    operation["summary"], operation["x-pages"] = metadata[:2]
    operation["x-stage"] = stage
    operation["x-idempotency-required"] = any(
        parameter["name"] == "Idempotency-Key" for parameter in operation.get("parameters", [])
    )
    for response in operation.get("responses", {}).values():
        if "content" in response and "*/*" in response["content"]:
            response["content"]["application/json"] = response["content"].pop("*/*")
        response["description"] = "操作成功"
    for status in ("400", "401", "403", "404", "409", "410", "429", "500", "503"):
        operation["responses"][status] = {"$ref": "#/components/responses/TooManyRequests" if status == "429" else "#/components/responses/Error"}
    if len(metadata) == 3 and metadata[2]:
        operation["x-permission"] = metadata[2]


def runtime(kind: str) -> dict[str, Any]:
    path = ROOT / "fafa-java" / f"fafa-{kind}" / "target" / f"{'app' if kind == 'biz' else 'admin'}-openapi.json"
    return json.loads(path.read_text(encoding="utf-8"))


def remove_unused_schemas(document: dict[str, Any]) -> None:
    schemas = document["components"]["schemas"]
    used: set[str] = set()

    def visit(value: Any) -> None:
        if isinstance(value, dict):
            reference = value.get("$ref", "")
            if reference.startswith("#/components/schemas/"):
                name = reference.rsplit("/", 1)[-1]
                if name not in schemas:
                    raise ValueError(f"缺失契约类型：{name}")
                if name not in used:
                    used.add(name)
                    visit(schemas[name])
            for child in value.values():
                visit(child)
        elif isinstance(value, list):
            for child in value:
                visit(child)

    visit(document["paths"])
    visit(document["components"].get("responses", {}))
    document["components"]["schemas"] = {name: schema for name, schema in schemas.items() if name in used}


def main() -> None:
    app_path = CONTRACTS / "app-v1.yaml"
    app = yaml.safe_load(app_path.read_text(encoding="utf-8"))
    actual = runtime("biz")
    old_schemas = {key: copy.deepcopy(value) for key, value in app["components"]["schemas"].items() if key in WP01_SCHEMAS}
    app["components"]["schemas"].update(actual["components"]["schemas"])
    app["components"]["schemas"].update(old_schemas)
    app["info"] = {"title": "FaFa 移动端后端接口", "version": "0.2.0", "description": "WP00—WP03 后端契约。字节额度使用十进制字符串；宠物资料支持版本控制与幂等。密码按 Unicode 码点计数，至少 8 位且包含字母和数字。"}
    for path, methods in actual["paths"].items():
        if not path.startswith("/v1/"):
            continue
        for method, operation in methods.items():
            identifier = operation["operationId"]
            if identifier in APP_METADATA:
                enrich(operation, APP_METADATA[identifier], "WP02-WP03")
                app["paths"].setdefault(path[3:], {})[method] = operation
    bootstrap = app["components"]["schemas"]["BootstrapResponse"]["properties"]["data"]["properties"]
    bootstrap["pets"]["items"] = {"$ref": "#/components/schemas/PetView"}
    bootstrap["nextRoute"]["enum"] = ["WELCOME", "NO_PET", "HOME"]
    bootstrap["features"] = {"type": "object", "properties": {name: {"type": "boolean"} for name in ("ai", "community", "petManagement")},
                             "required": ["ai", "community", "petManagement"], "additionalProperties": False}
    for name in ("Patch", "Version", "PreferenceChange"):
        app["components"]["schemas"][name]["required"] = ["version"]
    app["components"]["schemas"]["Delete"]["required"] = ["version", "acknowledgement"]
    app["components"]["schemas"]["Impact"]["properties"]["resourceCounts"] = {
        "type": "object", "properties": {"pet": {"type": "integer", "format": "int64"}},
        "required": ["pet"], "additionalProperties": {"type": "integer", "format": "int64"}}
    app["components"]["responses"]["Error"]["description"] = "失败响应：按稳定 FAFA-* 编码处理，data 可为空。"
    app["components"]["responses"]["TooManyRequests"]["description"] = "请求过于频繁；请按 Retry-After 等待后重试。"
    app["components"]["responses"]["TooManyRequests"]["headers"]["Retry-After"]["description"] = "建议等待秒数"
    admin_actual = runtime("admin")
    admin = {"openapi": "3.0.3", "info": {"title": "FaFa 管理端后端接口", "version": "0.2.0", "description": "独立管理员认证与 RBAC。业务写入经签名 Dubbo 契约，所有管理写入要求幂等键和原因。"},
             "servers": [{"url": "/api/admin/v1"}], "paths": {}, "components": copy.deepcopy(admin_actual["components"])}
    admin["components"]["schemas"]["ErrorResponse"] = copy.deepcopy(app["components"]["schemas"]["ErrorResponse"])
    admin["components"]["schemas"]["FieldError"] = copy.deepcopy(app["components"]["schemas"]["FieldError"])
    admin["components"]["responses"] = copy.deepcopy(app["components"]["responses"])
    admin["components"]["securitySchemes"] = copy.deepcopy(app["components"]["securitySchemes"])
    for name in ("StatusChange", "Decision"):
        schema = admin["components"]["schemas"][name]
        schema["required"] = sorted(set(schema.get("required", [])) | {"version"})
    for path, methods in admin_actual["paths"].items():
        if not path.startswith("/v1/"):
            continue
        for method, operation in methods.items():
            enrich(operation, ADMIN_METADATA[operation["operationId"]], "WP03")
            operation["security"] = [] if operation["operationId"] == "createAdminSession" else [{"bearerAuth": []}]
            admin["paths"].setdefault(path[3:], {})[method] = operation
    for name, document in (("app-v1.yaml", app), ("admin-v1.yaml", admin)):
        nullable_fields = {"signature", "avatarMediaId", "birthDate", "estimatedBirthMonth", "arrivedOn", "breedId",
                           "deleteAfter", "currentPetId", "defaultPetId", "nextCursor", "grantedAmount", "decisionReason", "operatorId", "resultCode"}
        for schema in document["components"]["schemas"].values():
            if {"code", "message", "data", "requestId"}.issubset(schema.get("properties", {})):
                schema["required"] = ["code", "message", "data", "requestId"]
                schema["properties"]["data"]["nullable"] = True
            for field_name, property_schema in schema.get("properties", {}).items():
                if field_name in nullable_fields:
                    property_schema["nullable"] = True
        remove_unused_schemas(document)
        (CONTRACTS / name).write_text(yaml.safe_dump(document, allow_unicode=True, sort_keys=False, width=110), encoding="utf-8")


if __name__ == "__main__":
    main()
