# FaFa Docker 部署

## 部署到已有服务器

目标服务器已有 MySQL、Redis、RocketMQ、Qdrant 等基础服务。使用 `docker-compose.apps.yml` 只构建并启动 Java 与 Python，保留 `/home/busizeshi/docker/docker-compose.yml`、现有容器和数据目录。

将 `docker-compose.apps.yml`、`fafa-java/` 和 `fafa-python/` 放到 `/home/busizeshi/docker/fafa-app/`。在该目录创建 `.env.production`，按 `.env.apps.production.example` 填写应用数据库账号、微信小程序密钥、千问 API key、COS SecretId/SecretKey 和服务间令牌，并设置权限：

```sh
chmod 600 .env.production
mkdir -p logs/java logs/python
chown -R 10001:10001 logs/java logs/python
```

服务器现有 Docker 网络名为 `docker_fafa-network`，网络内基础服务别名为 `mysql`、`redis`、`rocketmq-namesrv` 和 `qdrant`。Qdrant 未启用 API key；Redis 当前未启用密码，故 `REDIS_PASSWORD` 留空。Java API 映射到宿主机 `192.168.1.15:8081`，因为 `8080` 已由 RocketMQ Dashboard 使用。

检查配置并启动：

```sh
cd /home/busizeshi/docker/fafa-app
docker compose -p fafa-app --env-file .env.production -f docker-compose.apps.yml config --quiet
docker compose -p fafa-app --env-file .env.production -f docker-compose.apps.yml up -d --build
docker compose -p fafa-app --env-file .env.production -f docker-compose.apps.yml ps
docker compose -p fafa-app --env-file .env.production -f docker-compose.apps.yml logs --tail=200 fafa-java fafa-python
```

Java 连接既有 `fafa` 数据库并由 Flyway 执行待应用迁移；Python 连接既有 Qdrant 与千问服务。AI 任务通过 Java 到 Python 的内网 HTTP 调用派发，Python 的 RocketMQ 消费者保持关闭。小程序开发环境 API 地址应为 `http://192.168.1.15:8081`。真机正式环境需要 HTTPS 域名，并在微信公众平台配置合法 request/uploadFile 域名。

## 全新服务器

`docker-compose.yml` 包含 MySQL、Redis、RocketMQ、Python 和 Java，适用于尚无基础服务的主机。运行它前先按 `.env.production.example` 配置环境变量，并确认目标主机没有占用 Compose 暴露的端口或数据路径。

不要提交实际 `.env.production` 文件。COS、千问、微信、数据库与内部服务凭据都通过运行环境注入。
