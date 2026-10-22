# FaFa 开发约定

## 范围与架构

- 按 `docs/dev/第一阶段实施方案.md` 的工作包实施，当前先完成后端，再接安卓 APP。
- 沿用 Gateway、Admin、Biz、Python 工程。Biz 拥有业务事实与事务；Admin 使用独立管理数据库，经明确的签名 Dubbo 契约调用 Biz。
- 不覆盖用户未提交修改，不修改已经在共享环境执行的 Flyway 迁移；数据库变化通过新版本迁移交付。
- 使用中文文档和中文 Javadoc；代码标识、接口字段、命令和技术名称保持原文。

## 每一步的接口交付

- 每新增或修改一个接口，必须在同一次交付中更新对应的 `contracts/openapi/*.yaml`、`contracts/examples/http-operations.json` 和 `docs/api/` 文档。
- 每个接口必须具备：接口名称、接口定义、传参 JSON 示例、传参字段说明、响应 JSON 示例、响应字段说明、示例 curl。
- 同时说明 HTTP 状态、认证/权限、幂等、版本、重要业务限制和 pageKey。GET/无请求体接口应明确标注参数位置，不伪造 JSON 请求体。
- 只记录实际已实现的接口；待开发能力留在规划中，不能写成可调用接口。
- 更新 `deploy/verification/api_examples.py` 中的业务示例与说明，使用 `export_contracts.py` 和 `document_api.py` 生成契约与文档，再执行 `python deploy/verification/check_api_docs.py`。
- SpringDoc 导出来自 Maven 契约测试，输出在各模块的 `target/*-openapi.json`。导出文件与示例中的凭证均为虚构值，不提交环境密钥。

## 验证与交付

- 提交前执行适用的 Maven 测试、契约/文档检查和 `git diff --check`；业务并发和事务变化需要真实 MySQL 验证。
- 区分本地替身测试、真实数据库验证、真实网络/RPC 验证和供应商验证，未执行的项目如实记录。
- 交付说明包含实际实现范围、验证结果、待补测项及下一步；工作包完成不等于整个 P0 完成。
