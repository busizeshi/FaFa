# 本地 Docker Compose

第二批已加入 Admin 独立数据库和签名 RPC。详细启动、初始化和验收见[后端第二批清单](../../docs/dev/后端第二批开发与验收清单.md)，接口、字段和 curl 见[接口文档总览](../../docs/api/接口文档总览.md)。

1. 将 `.env.example` 复制为 `.env`。请分别生成至少 32 字符的 FAFA_AUTH_HMAC_SECRET 与 FAFA_INTERNAL_RPC_SECRET，并设置独立 MYSQL_ADMIN_PASSWORD；这些示例只适用于本地开发。
2. 启动基础设施：`docker compose -f compose.dev.yaml --profile core up -d`。
3. 打开 Nacos 控制台 `http://localhost:18080`，将 `nacos/configs/` 下的文件逐个发布，文件名作为 Data ID，分组使用 `FAFA_GROUP`。
4. 启动带开发邮件捕获的应用：`docker compose -f compose.dev.yaml --profile core --profile mail --profile apps up --build`。

本地端口：Gateway `8080`、Nacos API `8848`、Nacos 控制台 `18080`、MySQL `3306`、Redis `6379`。MySQL 和 Redis 仅绑定回环地址。Java 服务和 Python 服务只在 Compose 网络内部可访问，外部请求应通过 Gateway 进入。

`mail` profile 会加入 Mailpit，SMTP 监听回环地址的 `1025`，邮件查看页面使用 `8025`。Biz 启动时执行 Flyway 迁移并连接 MySQL/Redis。账号测试需要在迁移完成后手动对测试 Biz 数据库执行 `mysql/dev-only-legal.sql`；该文件不会自动发布为正式协议。

服务器端 MySQL/Redis 已使用独立验收库完成补测；SMTP 和完整 Gateway/Apifox 链路仍待执行。证据和限制见[补测清单](../../docs/dev/后端第一批开发与补测清单.md)。

本地 Nacos 为减少配置成本而关闭认证；共享或生产环境不得复用该设置。生产凭证必须由部署环境注入，管理类服务必须保持内网访问。

SQL 初始化脚本会创建独立的 `fafa_admin` 和 `fafa_biz` schema，并向本地开发账号授权。生产环境应为不同服务分配独立账号，并遵循最小权限原则。

Profiles 用于控制本地资源消耗：只调试基础设施或 Java 配置时启动 `core`，需要端到端服务开发时再加入 `apps`。Nacos、RabbitMQ 和 PowerJob 由 `middleware.yml` 单独部署；Qdrant 和 RocketMQ 不属于当前环境。
