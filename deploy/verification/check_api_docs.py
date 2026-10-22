"""检查每个已实现接口均有完整文档，并验证 JSON 示例与已提交 Schema 一致。"""
from __future__ import annotations

import json
import re
from pathlib import Path
from typing import Any

import yaml

ROOT = Path(__file__).resolve().parents[2]
SECTIONS = ("传参 JSON 示例", "传参字段说明", "响应 JSON 示例", "响应字段说明", "示例 curl")


def validate(value: Any, schema: dict[str, Any], schemas: dict[str, Any], path: str) -> None:
    if value is None and schema.get("nullable"):
        return
    if "$ref" in schema:
        validate(value, schemas[schema["$ref"].rsplit("/", 1)[-1]], schemas, path)
        return
    for fragment in schema.get("allOf", []):
        validate(value, fragment, schemas, path)
    kind = schema.get("type")
    if kind is not None:
        checks = {"object": isinstance(value, dict), "array": isinstance(value, list),
                  "string": isinstance(value, str), "boolean": isinstance(value, bool),
                  "integer": isinstance(value, int) and not isinstance(value, bool),
                  "number": isinstance(value, (int, float)) and not isinstance(value, bool)}
        if not checks[kind]:
            raise ValueError(f"{path} 不符合 {kind} 类型：{value!r}")
    if "enum" in schema and value not in schema["enum"]:
        raise ValueError(f"{path} 不符合枚举")
    if isinstance(value, dict):
        missing = set(schema.get("required", [])) - set(value)
        if missing:
            raise ValueError(f"{path} 缺少必填字段：{missing}")
        properties = schema.get("properties", {})
        for key, item in value.items():
            if key in properties:
                validate(item, properties[key], schemas, f"{path}.{key}")
            elif schema.get("additionalProperties") is False:
                raise ValueError(f"{path} 存在未定义字段：{key}")
            elif isinstance(schema.get("additionalProperties"), dict):
                validate(item, schema["additionalProperties"], schemas, f"{path}.{key}")
    if isinstance(value, list) and "items" in schema:
        for index, item in enumerate(value):
            validate(item, schema["items"], schemas, f"{path}[{index}]")
    if isinstance(value, str):
        if len(value) > schema.get("maxLength", float("inf")) or len(value) < schema.get("minLength", 0):
            raise ValueError(f"{path} 不符合长度限制")
        if "pattern" in schema and not re.fullmatch(schema["pattern"], value):
            raise ValueError(f"{path} 不符合格式限制")


def main() -> None:
    examples = json.loads((ROOT / "contracts" / "examples" / "http-operations.json").read_text(encoding="utf-8"))
    documents = "\n".join(path.read_text(encoding="utf-8") for path in (ROOT / "docs" / "api").glob("*接口.md"))
    operations: set[str] = set()
    for name in ("app-v1.yaml", "admin-v1.yaml"):
        contract = yaml.safe_load((ROOT / "contracts" / "openapi" / name).read_text(encoding="utf-8"))
        for path, methods in contract["paths"].items():
            for method, operation in methods.items():
                identifier = operation["operationId"]
                if identifier in operations:
                    raise ValueError(f"重复 operationId：{identifier}")
                operations.add(identifier)
                example = examples[identifier]
                matching = [block for block in documents.split("\n## ") if f"（{identifier}）" in block]
                if len(matching) != 1 or any(section not in matching[0] for section in SECTIONS):
                    raise ValueError(f"接口文档章节缺失：{identifier}")
                if not operation.get("x-pages"):
                    raise ValueError(f"缺少页面关联：{identifier}")
                body = operation.get("requestBody", {}).get("content", {}).get("application/json", {}).get("schema")
                if body:
                    validate(example["requestBody"], body, contract["components"]["schemas"], f"{identifier}.request")
                status = next(code for code in operation["responses"] if code.startswith("2"))
                schema = operation["responses"][status]["content"]["application/json"]["schema"]
                validate(example["response"], schema, contract["components"]["schemas"], f"{identifier}.response")
    if operations != set(examples):
        raise ValueError("示例与接口数量不一致")
    print(f"已验证 {len(operations)} 个接口：文档章节、页面关联、请求与响应 JSON 示例均匹配。")


if __name__ == "__main__":
    main()
