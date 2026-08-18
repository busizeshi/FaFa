# FaFa 项目 Docker 部署文档

## 📦 服务清单

本部署方案包含以下服务：

| 服务 | 版本 | 端口 | 用途 |
|------|------|------|------|
| MySQL | 8.0 | 3306 | 关系型数据库 |
| Redis | 7.2 | 6379 | 缓存数据库 |
| MinIO | latest | 9000, 9001 | 对象存储 |
| Qdrant | latest | 6333, 6334 | 向量数据库 |
| RocketMQ NameServer | 4.9.4 | 9876 | 消息队列-服务发现 |
| RocketMQ Broker | 4.9.4 | 10909, 10911, 10912 | 消息队列-消息存储 |
| RocketMQ Dashboard | latest | 8080 | 消息队列-管理界面 |

## 📁 目录结构

服务器部署目录：`/home/busizeshi/docker`

```
/home/busizeshi/docker/
├── docker-compose.yml          # Docker Compose 配置文件
├── mysql/
│   ├── data/                   # MySQL 数据文件
│   ├── config/                 # MySQL 配置文件
│   └── logs/                   # MySQL 日志文件
├── redis/
│   ├── data/                   # Redis 数据文件
│   └── config/
│       └── redis.conf          # Redis 配置文件
├── minio/
│   ├── data/                   # MinIO 数据文件
│   └── config/                 # MinIO 配置文件
├── qdrant/
│   ├── data/                   # Qdrant 数据文件
│   └── config/                 # Qdrant 配置文件
└── rocketmq/
    ├── namesrv/
    │   ├── logs/               # NameServer 日志
    │   └── store/              # NameServer 数据
    └── broker/
        ├── logs/               # Broker 日志
        └── store/              # Broker 数据
```

## 🚀 部署步骤

### 1. 准备工作

确保服务器已安装 Docker 和 Docker Compose：

```bash
# 检查 Docker 版本
docker --version

# 检查 Docker Compose 版本
docker compose version
```

### 2. 创建部署目录

```bash
# 创建主目录
mkdir -p /home/busizeshi/docker
cd /home/busizeshi/docker

# 创建子目录
mkdir -p mysql/{data,config,logs}
mkdir -p redis/{data,config}
mkdir -p minio/{data,config}
mkdir -p qdrant/{data,config}
mkdir -p rocketmq/{namesrv/{logs,store},broker/{logs,store}}
```

### 3. 上传配置文件

将以下文件上传到服务器：
- `docker-compose.yml` → `/home/busizeshi/docker/`
- `redis/config/redis.conf` → `/home/busizeshi/docker/redis/config/`

### 4. 启动所有服务

```bash
cd /home/busizeshi/docker

# 启动所有服务（后台运行）
docker compose up -d

# 查看服务状态
docker compose ps

# 查看服务日志
docker compose logs -f
```

### 5. 验证服务

```bash
# 查看所有容器状态
docker compose ps

# 查看特定服务日志
docker compose logs mysql
docker compose logs redis
docker compose logs minio
docker compose logs qdrant
docker compose logs rocketmq-namesrv
docker compose logs rocketmq-broker
docker compose logs rocketmq-dashboard
```

## 🔧 服务管理

### 启动服务

```bash
# 启动所有服务
docker compose up -d

# 启动指定服务
docker compose up -d mysql redis
```

### 停止服务

```bash
# 停止所有服务
docker compose stop

# 停止指定服务
docker compose stop mysql
```

### 重启服务

```bash
# 重启所有服务
docker compose restart

# 重启指定服务
docker compose restart mysql
```

### 删除服务

```bash
# 停止并删除所有容器（保留数据）
docker compose down

# 停止并删除所有容器和数据卷（谨慎操作）
docker compose down -v
```

### 查看日志

```bash
# 查看所有服务日志
docker compose logs

# 实时查看日志
docker compose logs -f

# 查看指定服务日志
docker compose logs -f mysql

# 查看最近 100 行日志
docker compose logs --tail=100
```

## 🔐 默认账号信息

### MySQL
- **主机**: `localhost:3306`
- **Root 密码**: `fafa_root_123456`
- **数据库**: `fafa`
- **用户名**: `fafa`
- **密码**: `fafa_123456`

### Redis
- **主机**: `localhost:6379`
- **密码**: 无（可在 redis.conf 中设置）

### MinIO
- **Console**: `http://localhost:9001`
- **API**: `http://localhost:9000`
- **用户名**: `minioadmin`
- **密码**: `minioadmin123`

### Qdrant
- **REST API**: `http://localhost:6333`
- **gRPC**: `localhost:6334`
- **Dashboard**: `http://localhost:6333/dashboard`

### RocketMQ
- **NameServer**: `localhost:9876`
- **Dashboard**: `http://localhost:8080`
- **Broker Ports**: 10909, 10911, 10912

## 📊 监控和维护

### 检查服务健康状态

```bash
# 查看容器状态
docker compose ps

# 查看容器资源使用情况
docker stats

# 查看特定容器详情
docker inspect fafa-mysql
```

### 进入容器

```bash
# 进入 MySQL 容器
docker exec -it fafa-mysql bash

# 进入 Redis 容器
docker exec -it fafa-redis sh

# 连接 MySQL 客户端
docker exec -it fafa-mysql mysql -uroot -pfafa_root_123456

# 连接 Redis 客户端
docker exec -it fafa-redis redis-cli
```

### 数据备份

#### MySQL 备份

```bash
# 备份数据库
docker exec fafa-mysql mysqldump -uroot -pfafa_root_123456 fafa > backup_$(date +%Y%m%d).sql

# 恢复数据库
docker exec -i fafa-mysql mysql -uroot -pfafa_root_123456 fafa < backup_20260818.sql
```

#### Redis 备份

```bash
# 触发 RDB 快照
docker exec fafa-redis redis-cli BGSAVE

# 备份 RDB 文件
cp /home/busizeshi/docker/redis/data/dump.rdb /backup/redis_dump_$(date +%Y%m%d).rdb
```

#### MinIO 备份

```bash
# 直接备份数据目录
tar -czf minio_backup_$(date +%Y%m%d).tar.gz /home/busizeshi/docker/minio/data/
```

#### RocketMQ 备份

```bash
# 备份 RocketMQ 日志
./manage.sh backup rocketmq

# 手动备份日志
tar -czf rocketmq-backup-$(date +%Y%m%d).tar.gz rocketmq/

# 注意：Broker 消息数据在容器内部，需要使用 docker cp 备份
# 备份 Broker 数据
docker cp fafa-rocketmq-broker:/home/rocketmq/store ./rocketmq-store-backup-$(date +%Y%m%d)
tar -czf rocketmq-store-backup-$(date +%Y%m%d).tar.gz rocketmq-store-backup-$(date +%Y%m%d)
rm -rf rocketmq-store-backup-$(date +%Y%m%d)
```

### 清理日志

```bash
# 清理 Docker 日志
docker compose logs --tail=0 -f > /dev/null

# 清理 RocketMQ 日志（保留最近7天）
find /home/busizeshi/docker/rocketmq/*/logs -name "*.log" -mtime +7 -delete
```

## 🛠️ 故障排查

### 服务无法启动

```bash
# 查看详细错误日志
docker compose logs <service_name>

# 检查端口占用
netstat -tulpn | grep <port>

# 检查磁盘空间
df -h
```

### 数据库连接失败

```bash
# 检查 MySQL 是否正在运行
docker compose ps mysql

# 查看 MySQL 日志
docker compose logs mysql

# 测试连接
docker exec -it fafa-mysql mysql -uroot -pfafa_root_123456 -e "SELECT 1"
```

### RocketMQ Broker 重启问题

```bash
# 查看 Broker 日志
docker compose logs rocketmq-broker

# 清理 Broker 数据（谨慎操作）
docker compose stop rocketmq-broker
rm -rf /home/busizeshi/docker/rocketmq/broker/store/*
docker compose up -d rocketmq-broker

# 检查 NameServer 连接
docker exec fafa-rocketmq-broker sh -c "telnet rocketmq-namesrv 9876"
```

## ⚠️ 注意事项

1. **生产环境安全**
   - 修改所有默认密码
   - 配置防火墙规则，限制端口访问
   - 启用 Redis 密码认证
   - 定期备份数据

2. **资源配置**
   - 根据实际负载调整 JVM 内存参数
   - 监控磁盘空间，及时清理日志
   - 配置日志轮转策略

3. **网络配置**
   - 确保防火墙开放必要端口
   - 如需外网访问，配置安全组规则

4. **数据持久化**
   - 所有重要数据已挂载到宿主机
   - 定期备份 `/home/busizeshi/docker` 目录

## 📞 技术支持

如遇问题，请检查：
1. Docker 和 Docker Compose 版本是否符合要求
2. 服务器资源（CPU、内存、磁盘）是否充足
3. 网络连接是否正常
4. 日志文件中的错误信息

---

**文档版本**: 1.0  
**更新日期**: 2026-08-18  
**适用环境**: Linux (建议 Ubuntu 20.04+/CentOS 7+)
