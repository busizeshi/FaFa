# fafa-java

后端已实现账号及第二批宠物、存储额度和后台最小能力。完整请求/响应、字段与 curl 见[接口文档总览](../docs/api/接口文档总览.md)，启动与验收见[第二批清单](../docs/dev/后端第二批开发与验收清单.md)。Biz/Admin 须注入相同的 FAFA_INTERNAL_RPC_SECRET，Admin 另需独立管理库。

这是 FaFa 的 Maven 聚合工程，包含三个可独立运行的应用和一个共享类库：

- `fafa-gateway`：对外 HTTP 入口和路由网关。
- `fafa-admin`：运营管理 API，通过共享 Dubbo 契约调用 Biz。
- `fafa-biz`：业务服务，按照 DDD 领域上下文组织代码。
- `fafa-common`：共享响应、错误、请求 ID 约定和 RPC 契约，不单独部署。

## 前置要求

- JDK 21。
- Maven Wrapper（优先使用）或 Maven 3.9 及以上版本。

## 构建

```powershell
.\mvnw.cmd -DskipTests package
```

版本矩阵维护在根目录 `pom.xml`。Spring Boot 和 Spring Cloud 版本遵循兼容版本线；调整版本时需要同步更新相关 BOM，并重新构建全部模块。

## 本地运行

WP00/WP01 的环境变量、服务器中间件连接信息和补测状态见[后端第一批开发与补测清单](../docs/dev/后端第一批开发与补测清单.md)。Biz 运行需要 MySQL、Redis、HMAC 密钥和邮件发送器配置。

Gateway、Admin、Biz 的运行配置由 Nacos 提供。将 [`docs/nacos`](../docs/nacos) 中的配置发布到 `fafa` 命名空间、`FAFA_GROUP` 分组后，再通过环境变量注入 Nacos 登录凭证、数据库/Redis 密码和 HMAC/RPC 密钥。独立启动服务时先设置 `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_NAMESPACE=fafa` 及各服务必需的凭证。先构建聚合工程，再在不同终端运行打包后的应用：

```powershell
.\mvnw.cmd -DskipTests package
java -jar fafa-biz/target/fafa-biz-0.1.0-SNAPSHOT.jar --spring.profiles.active=nacos
java -jar fafa-gateway/target/fafa-gateway-0.1.0-SNAPSHOT.jar --spring.profiles.active=nacos
```

`/actuator/health` 是服务私有探针。Gateway 将 APP 的 `/api/v1/**` 转发到 Biz 的 `/v1/**`，将管理端 `/api/admin/v1/**` 转发到 Admin 的 `/v1/**`。管理端 v1 业务 API 会在后续批次实现，现有内部 ping 示例保持私有；旧版透传路由默认关闭。数据库、Redis、Nacos 和邮件凭证必须放在 Git 仓库之外。Nacos 导入使用必需配置，Data ID 缺失或 Nacos 不可用时服务不会带着空配置启动。

## 离线测试

```powershell
.\mvnw.cmd -q test
```

测试使用 H2、内存 Sa-Token 测试适配器、SMTP/限流模拟对象和进程内 HTTP 下游服务。测试通过不等于真实 MySQL、Redis 或 SMTP 联调通过。提交到仓库的客户端契约见 [`app-v1.yaml`](../contracts/openapi/app-v1.yaml)。

## Java API 文档（Javadoc）

`fafa-common` 中的公开契约必须维护中文 Javadoc。生成项目 API 文档：

```powershell
.\mvnw.cmd -pl fafa-common javadoc:javadoc
```
