# FaFa 项目 - 快速启动指南

> 从零到运行的完整步骤

---

## 📋 前置要求

- ✅ Java 21+
- ✅ Maven 3.8+
- ✅ Python 3.10+
- ✅ Docker & Docker Compose
- ✅ Git

---

## 🚀 快速启动（5分钟）

### 第一步：克隆项目（如已克隆，跳过）

```bash
cd D:\dev\Java\FaFa
```

### 第二步：配置环境变量

```powershell
# 1. 复制配置模板
Copy-Item .env.example .env
Copy-Item fafa-python\.env.example fafa-python\.env

# 2. 编辑配置文件
notepad .env
notepad fafa-python\.env
```

**必须填写的配置项**：
```ini
# 在 .env 和 fafa-python/.env 中填写
DASHSCOPE_API_KEY=sk-your_api_key_here
```

获取 API Key：访问 [DashScope 控制台](https://dashscope.console.aliyun.com/)

### 第三步：启动中间件

```powershell
# 启动所有中间件服务
docker-compose up -d

# 检查服务状态
docker-compose ps

# 应该看到以下服务：
# - fafa-mysql
# - fafa-redis
# - fafa-minio
# - fafa-qdrant
# - fafa-rocketmq-namesrv
# - fafa-rocketmq-broker
```

### 第四步：初始化数据库

```powershell
# 连接 MySQL
docker exec -it fafa-mysql mysql -ufafa -pfafa_123456 fafa

# 或使用 MySQL 客户端工具连接
# Host: localhost
# Port: 3306
# Database: fafa
# Username: fafa
# Password: fafa_123456
```

执行数据库建表 SQL（在 `docs/数据库设计.md` 中）

### 第五步：检查配置

```powershell
# 运行配置检查脚本
.\check-config.ps1

# 如果有失败项，根据提示修复
```

### 第六步：启动 Java 服务

```powershell
# 方式一：使用 Maven（推荐开发）
cd fafa-java
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package -DskipTests
java -jar target/fafa-java-1.0.0.jar --spring.profiles.active=dev
```

**验证**：访问 http://localhost:8080/api/health
应返回：`{"status":"ok","service":"java",...}`

**API 文档**：http://localhost:8080/api/doc.html

### 第七步：启动 Python 服务

```powershell
# 1. 创建虚拟环境（首次）
cd fafa-python
python -m venv venv

# 2. 激活虚拟环境
.\venv\Scripts\Activate.ps1

# 3. 安装依赖（首次）
pip install -r requirements.txt

# 4. 启动服务
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

**验证**：访问 http://localhost:8000/health
应返回：`{"status":"ok","service":"python",...}`

**API 文档**：http://localhost:8000/docs

---

## ✅ 验证服务

### 中间件服务检查

```powershell
# MySQL
docker exec -it fafa-mysql mysql -ufafa -pfafa_123456 -e "SELECT 1"

# Redis
docker exec -it fafa-redis redis-cli ping

# MinIO 控制台
# 访问 http://localhost:9001
# 用户名: minioadmin
# 密码: minioadmin123

# Qdrant Dashboard
# 访问 http://localhost:6333/dashboard

# RocketMQ Dashboard
# 访问 http://localhost:8080
```

### 服务健康检查

```powershell
# Java 服务
curl http://localhost:8080/api/health

# Python 服务
curl http://localhost:8000/health
```

### 服务间通信测试

```powershell
# Java 调用 Python
curl http://localhost:8080/api/ai/health

# Python 调用 Java
curl http://localhost:8000/api/test/java-service
```

---

## 📁 项目结构

```
D:\dev\Java\FaFa
├── .env                      # 环境变量（已配置）
├── docker-compose.yml        # 中间件编排
├── check-config.ps1          # 配置检查脚本
│
├── fafa-java/               # Java 服务
│   ├── src/main/
│   │   ├── java/com/fafa/
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── application-prod.yml
│   └── pom.xml
│
├── fafa-python/             # Python 服务
│   ├── .env                 # Python 环境变量（已配置）
│   ├── app/
│   │   ├── core/config.py   # 配置模块
│   │   ├── api/             # API 路由
│   │   ├── service/         # 业务逻辑
│   │   └── main.py          # 启动入口
│   └── requirements.txt
│
├── web/                     # 前端小程序
│
└── docs/                    # 文档
    ├── 中间件配置说明.md
    ├── 配置文件清单.md
    ├── 产品功能设计.md
    ├── 数据库设计.md
    └── 后端开发计划.md
```

---

## 🔧 常用命令

### 中间件管理

```powershell
# 启动所有服务
docker-compose up -d

# 停止所有服务
docker-compose stop

# 重启所有服务
docker-compose restart

# 查看日志
docker-compose logs -f [service_name]

# 停止并删除容器
docker-compose down
```

### Java 服务管理

```powershell
# 开发模式（热重载）
cd fafa-java
mvn spring-boot:run

# 编译打包
mvn clean package -DskipTests

# 运行打包后的 jar
java -jar target/fafa-java-1.0.0.jar

# 指定环境
java -jar target/fafa-java-1.0.0.jar --spring.profiles.active=dev
```

### Python 服务管理

```powershell
# 开发模式（热重载）
cd fafa-python
uvicorn app.main:app --reload

# 生产模式
uvicorn app.main:app --host 0.0.0.0 --port 8000

# 使用 gunicorn（生产推荐）
gunicorn app.main:app -w 4 -k uvicorn.workers.UvicornWorker
```

---

## 🐛 常见问题

### Q1: 端口被占用

**错误**：`Address already in use: bind`

**解决**：
```powershell
# 查看端口占用
netstat -ano | findstr "8080"
netstat -ano | findstr "8000"

# 停止占用进程（替换 PID）
taskkill /F /PID <PID>
```

### Q2: Docker 服务启动失败

**解决**：
```powershell
# 查看具体错误
docker-compose logs [service_name]

# 删除旧容器重新创建
docker-compose down -v
docker-compose up -d
```

### Q3: Python 依赖安装失败

**解决**：
```powershell
# 使用国内镜像
pip install -r requirements.txt -i https://pypi.tuna.tsinghua.edu.cn/simple

# 升级 pip
python -m pip install --upgrade pip
```

### Q4: 数据库连接失败

**解决**：
1. 检查 MySQL 容器是否运行：`docker ps | findstr mysql`
2. 检查用户名密码是否正确
3. 检查数据库是否已创建：`fafa`
4. 重启 MySQL 容器：`docker-compose restart mysql`

### Q5: MinIO 上传失败

**解决**：
1. 访问 MinIO 控制台：http://localhost:9001
2. 登录后创建名为 `fafa` 的 Bucket
3. 设置 Bucket 权限为 `public`（可选）

---

## 📊 服务端口一览

| 服务 | 端口 | 访问地址 | 说明 |
|------|------|---------|------|
| Java API | 8080 | http://localhost:8080/api | 业务 API |
| Java API Doc | 8080 | http://localhost:8080/api/doc.html | Knife4j 文档 |
| Python API | 8000 | http://localhost:8000 | AI 服务 API |
| Python API Doc | 8000 | http://localhost:8000/docs | FastAPI 文档 |
| MySQL | 3306 | localhost:3306 | 数据库 |
| Redis | 6379 | localhost:6379 | 缓存 |
| MinIO API | 9000 | http://localhost:9000 | 对象存储 API |
| MinIO Console | 9001 | http://localhost:9001 | MinIO 管理控制台 |
| Qdrant API | 6333 | http://localhost:6333 | 向量数据库 API |
| Qdrant Dashboard | 6333 | http://localhost:6333/dashboard | Qdrant 控制台 |
| RocketMQ NameServer | 9876 | localhost:9876 | 消息队列服务发现 |
| RocketMQ Dashboard | 8080 | http://localhost:8080 | RocketMQ 控制台 |

---

## 📚 下一步

1. **开发新功能**：参考 `docs/后端开发计划.md`
2. **查看 API 文档**：
   - Java: http://localhost:8080/api/doc.html
   - Python: http://localhost:8000/docs
3. **阅读设计文档**：
   - 产品设计：`docs/产品功能设计.md`
   - 数据库设计：`docs/数据库设计.md`
   - 技术架构：`docs/技术架构与实现方案.md`
4. **开发前端**：查看 `web/` 目录

---

## 🆘 获取帮助

- **配置问题**：查看 `docs/中间件配置说明.md`
- **部署问题**：查看 `docs/deploy/README.md`
- **开发问题**：查看 `docs/后端开发计划.md`
- **运行配置检查**：`.\check-config.ps1`

---

**祝开发顺利！** 🎉
