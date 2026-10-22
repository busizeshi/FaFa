# fafa-biz 的 DDD 模块边界

`fafa-biz` 是一个可部署服务。领域上下文以包组织，而不是提前拆成独立 Maven 模块或微服务。每个上下文都遵循由外向内的依赖方向：

- `interfaces`：HTTP 和 RPC 入口适配器。
- `application`：用例编排和事务边界。
- `domain`：业务规则和仓储抽象。
- `infrastructure`：持久化和外部服务适配器。

初始上下文包括 `identity`、`pet`、`care`、`reminder`、`media`、`sharing` 和 `community`。跨上下文调用应通过面向应用层的接口和稳定 ID 完成，不得直接访问其他上下文的 Repository 或数据表。当前包结构是工程约定；当真实模块和规则稳定后，再增加自动化架构约束。

第一批后端实现 `identity` 上下文：邮箱 challenge、注册、Sa-Token 会话、密码重置、已发布协议、bootstrap 和个人资料读取。MySQL 保存凭证和 challenge 状态，Redis 保存会话和限流数据。服务内部路由使用 `/v1`，对外通过 Gateway 暴露为 `/api/v1`。

环境准备和补测状态记录在[后端第一批开发与补测清单](../../docs/dev/后端第一批开发与补测清单.md)中。

第二批已实现 pet、usage 和后台专用业务用例：主人权限、生命周期、幂等、存储预占/结算、额度审批及签名 RPC。范围与事务见[第二批验收清单](../../docs/dev/后端第二批开发与验收清单.md)，已实现接口及 JSON/curl 见[接口文档总览](../../docs/api/接口文档总览.md)。
