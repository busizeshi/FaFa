# FaFa 部署与持续交付（开发服务器）

> 适用环境：`192.168.1.254`（Ubuntu 22.04 + Docker 29.6.1）
> 配套文档：[FaFa_中间件环境与账号信息_v1.0](../docs/FaFa_中间件环境与账号信息_v1.0.md)

## 1. 部署结构

```
/home/busizeshi/docker/
├── docker-compose.yml            # 中间件第一组：MySQL / Redis / MinIO
├── middleware/                   # 中间件第二组：Nacos / RabbitMQ / PowerJob
├── jenkins/                      # CI 服务（本目录 docker-compose.yml 的拷贝运行位置）
│   ├── docker-compose.yml
│   ├── plugins.txt
│   ├── init.groovy.d/
│   ├── ssh/                      # Gitee 部署私钥 + known_hosts（挂到容器 /root/.ssh）
│   └── data/                     # JENKINS_HOME（勿手工改）
├── ci-workspace/                 # Jenkins 工作区（容器内外同路径，供 docker -v 使用）
└── dev/fafa/                     # 应用运行目录预留
```

应用容器（本次新增，均由 Jenkins 管理）：

| 容器 | 镜像 | 端口 | 说明 |
|---|---|---|---|
| fafa-gateway | `fafa/fafa-gateway` | **9090 → 宿主机（唯一对外）** | 统一入口，路由配置在 Nacos |
| fafa-biz | `fafa/fafa-biz` | 仅内网 8081 | 宠主业务 API |
| fafa-admin | `fafa/fafa-admin` | 仅内网 8082 | 运营后台 API |
| fafa-python | `fafa/fafa-python` | 仅内网 8090 | AI 服务（FastAPI） |

四个应用容器都加入 `docker_fafa-network`，通过服务名访问中间件（`mysql` / `redis` / `fafa-nacos` / `fafa-rabbitmq`），应用配置仍来自 Nacos 配置中心（namespace=`fafa`）。

## 2. 首次准备（一次性）

### 2.1 Jenkins 服务

```bash
# 在服务器上执行（把仓库 deploy/jenkins/ 下三个文件先放到位）
mkdir -p /home/busizeshi/docker/jenkins /home/busizeshi/docker/ci-workspace /home/busizeshi/docker/dev/fafa
cd /home/busizeshi/docker/jenkins
docker compose up -d
docker logs -f fafa-jenkins      # 等待 "Jenkins is fully up and running"
```

访问 `http://192.168.1.254:9095`，默认账号 `admin`，密码 `FafaJenkins2026!`（可用 `JENKINS_ADMIN_PASSWORD` 环境变量覆盖，改后需删除 `data/` 重建或手工改密）。

### 2.2 Gitee 拉取授权（部署公钥）

代码托管已迁移到 Gitee 私有仓库：`git@gitee.com:without-thinking-it-is/FaFa.git`（`github` 远端保留作备份）。

Jenkins 容器内 `HOME=/root`，采用**挂载部署私钥**的方式授权（不用 Jenkins 凭据，因为容器 OpenSSH 10 读取凭据里存的 ed25519 会报 `error in libcrypto`）：

```bash
cd /home/busizeshi/docker/jenkins
ssh-keygen -t ed25519 -N '' -C 'fafa-jenkins-deploy' -f ssh/id_ed25519
cat ssh/id_ed25519.pub        # 整行贴到 Gitee 仓库 → 管理 → 部署公钥管理
ssh-keyscan -t rsa,ecdsa,ed25519 gitee.com > ssh/known_hosts
docker compose up -d --force-recreate
docker exec fafa-jenkins ssh -T git@gitee.com   # 出现 successfully authenticated 即可
```

`ssh/` 目录以 `./ssh:/root/.ssh:ro` 挂进容器，`id_ed25519`（部署公钥，只读权限）+ `known_hosts`（Gitee 主机指纹）随容器重建持久生效。

### 2.3 流水线任务（已创建）

任务 `fafa-deploy` 已通过 Jenkins API 创建，等价于在界面上：

- 定义：`Pipeline script from SCM`
- SCM：`Git` → `git@gitee.com:without-thinking-it-is/FaFa.git`，凭据留空（走挂载私钥）
- 分支：`*/main`；脚本路径：`Jenkinsfile`

### 2.4 触发方式

- **SCM 轮询（当前生效）**：任务级 `SCMTrigger` 为 `H/5 * * * *`，Gitee 有新提交 5 分钟内自动构建。
- **Gitee Webhook（需公网可达）**：当前 Jenkins 在内网，Gitee 无法回调。具备公网地址（如 frp / 端口映射 / 域名）后：
  1. 任务配置勾选「触发远程构建」，设置身份验证令牌 `fafa-ci-token`；
  2. Gitee 仓库 → 管理 → WebHooks → 添加：
     `http://<公网地址>:9095/job/fafa-deploy/build?token=fafa-ci-token`
     事件勾选 Push，勾选「激活」。

## 3. 日常使用

- 一键部署：Jenkins → `fafa-deploy` → Build with Parameters（可改镜像标签、跳过测试、仅构建不部署）
- 查看状态：`docker ps --filter name=fafa-`（Jenkins 流水线末尾也会打印）
- 查看日志：`docker logs -f fafa-gateway` / `docker logs -f fafa-biz`
- 冒烟验证：`curl http://192.168.1.254:9090/api/v1/bootstrap`

## 4. 本目录文件说明

| 文件 | 作用 |
|---|---|
| `deploy/java/Dockerfile` | Java 运行镜像（三服务共用，仅 `JAR_FILE` 不同） |
| `fafa-python/Dockerfile` | Python 服务两阶段构建镜像 |
| `deploy/docker-compose.yml` | 四个应用容器编排（仅网关暴露 9090） |
| `deploy/.env.example` | 部署变量模板（compose 默认读取；`.env` 不入库） |
| `deploy/maven/settings.xml` | CI 用 Maven 配置（阿里云镜像加速） |
| `deploy/jenkins/*` | Jenkins 服务编排、插件清单、初始化脚本、Gitee 部署私钥目录 `ssh/` |
| `Jenkinsfile` | 流水线定义（构建 → 测试 → 镜像 → 部署 → 健康检查） |

## 5. 已知环境约束（排障先看这里）

| 现象 | 原因 | 处理 |
|---|---|---|
| 拉取镜像超时 | 服务器无法直连 `registry-1.docker.io` / `download.docker.com` | 统一使用 `docker.m.daocloud.io` 前缀（Dockerfile 与 Jenkinsfile 已固化） |
| Jenkins 内 `docker run -v $WORKSPACE/...` 看不到源码 | docker.sock 模式下路径必须宿主机可见 | 工作区固定 `/home/busizeshi/docker/ci-workspace/<job>`，容器内外同路径 |
| 网关 503 | 下游未注册到 Nacos 或未启动 | 先看 `fafa-biz` / `fafa-admin` 健康状态，再看 Nacos 服务列表 |
| 网关路由不生效 | 本地 yml 里同时存在 `spring.cloud.nacos.*` 与 `gateway.routes` 会打断绑定 | 路由只放 Nacos `fafa-gateway.yml`（见该文件注释） |
| 容器内存不足 | 宿主机 20G，Nacos/PowerJob 已占较多 | Java 容器 `-XX:MaxRAMPercentage=75`，如需更紧可加 `mem_limit` |
| 流水线报 `Load key ...: error in libcrypto` | 容器 OpenSSH 10 + OpenSSL 3.5 读不了 Jenkins 凭据里存的 ed25519 文本 | 不给 SCM 配凭据，改用 `./ssh:/root/.ssh:ro` 挂载私钥（见 2.2） |
| 流水线报 `No ED25519 host key is known for gitee.com` | git 插件严格校验 host key，且容器 `HOME=/root` | `ssh-keyscan gitee.com > ssh/known_hosts` 后重建容器 |
