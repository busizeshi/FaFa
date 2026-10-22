# FaFa Nacos 配置

本目录是 Nacos 配置的版本管理源。服务端当前使用命名空间 ID `fafa`、配置分组 `FAFA_GROUP`。上传到 Nacos 时文件名就是 Data ID，保持 `.properties` 格式；当前 Spring Cloud Alibaba 版本继续使用 properties，避免 YAML 配置动态刷新兼容问题。

| 文件/Data ID | 导入服务 | 作用 |
|---|---|---|
| `common.properties` | Gateway、Admin、Biz | 通用 Spring Boot、日志、健康检查，以及共享 MySQL/Redis 主机端口和连接池限制 |
| `fafa-gateway.properties` | Gateway | 监听端口和下游服务地址 |
| `fafa-admin.properties` | Admin | Admin 独立数据库、Dubbo 客户端、后台策略 |
| `fafa-biz.properties` | Biz | Biz 数据库、Redis、SMTP、Dubbo、认证和业务配置 |

先在 `fafa` 命名空间、`FAFA_GROUP` 分组创建并发布 `common.properties`，再发布三个服务配置。各应用只从 `common.properties` 和自己的 Data ID 导入配置；缺少必需配置或无法连接 Nacos 时，应用启动失败，避免误用空配置启动。

Admin 与 Biz 各自拥有独立数据库 schema 和账号。`common.properties` 只放共用的数据库/Redis地址与连接池设置，不存数据库用户名、密码或 RPC/HMAC 密钥。数据库密码、Redis 密码、Nacos 登录凭证、SMTP 凭证、RPC/HMAC 密钥由应用运行环境注入；配置中的 `${...}` 是环境变量占位符，不要替换成真实凭证后提交。Gateway 不需要数据库，因此不配置数据源，也不会读取 Admin/Biz 的数据库账号。

当前 Java 工程尚未引入 Spring AMQP，也没有 RabbitMQ 消费者/生产者，因此暂不放置无效的 `spring.rabbitmq.*` 配置。接入消息功能时，只把 broker 地址和 vhost 放入实际使用该队列的服务配置，用户名/密码仍由该服务的运行环境注入。

当前 Data ID 默认指向隔离的 WP02 验收库：`fafa_wp02_admin_test`、`fafa_wp02_biz_test`。本地 Compose 使用 `FAFA_MYSQL_HOST=mysql`、`FAFA_ADMIN_DB_NAME=fafa_admin`、`FAFA_BIZ_DB_NAME=fafa_biz` 和 Compose 网络内的 Redis 地址覆盖默认值。切换到正式/其他环境前，先核对 schema、最小权限账号、备份和凭证注入方式；不要把共享旧库 `fafa` 用作迁移目标。

Nacos 登录信息和 namespace ID 通过 `NACOS_SERVER_ADDR`、`NACOS_USERNAME`、`NACOS_PASSWORD`、`NACOS_NAMESPACE` 配置。namespace 默认值为 `fafa`；若 Nacos 控制台显示的 namespace ID 与名称不同，应将实际 ID 注入 `NACOS_NAMESPACE`。

WP04-A 配置加入 fafa-biz.properties。先建专用私有桶，设置 FAFA_MEDIA_*，再发布配置。到期清理默认开启，间隔 60 秒。缺少存储配置时签名/读取返回 503，不影响已有账号和宠物功能。
