// ==================================================================
// FaFa CI/CD 流水线（Jenkins 声明式 Pipeline）
//
// 目标：一次构建把 3 个 Java 服务 + 1 个 Python 服务构建成镜像并部署到
//       开发服务器（/home/busizeshi/docker/dev/fafa），仅网关 9090 对外。
//
// 关键设计（务必保留，改动前先读）：
//   1. 工作区固定为 /home/busizeshi/docker/ci-workspace/<job>：
//      Jenkins 容器通过挂载的 /var/run/docker.sock 调用宿主机 dockerd，
//      `docker run -v $WORKSPACE/...` 里的路径必须先被宿主机认识，否则会
//      在宿主机上建出空目录、构建容器看不到源码；
//   2. 构建工具全部用官方镜像（maven / python），Jenkins 容器只装 docker CLI；
//   3. 镜像拉取走 docker.m.daocloud.io 镜像站（服务器无法直连 Docker Hub）。
//
// 触发方式：
//   - SCM 轮询（默认，每 5 分钟）—— 内网 Jenkins 无公网入口时唯一可用方案；
//   - Gitee Webhook —— 待 Jenkins 具备公网可达地址后，在 Gitee 仓库
//     「管理 → WebHooks」指向 JENKINS_URL/job/<job>/build?token=<TOKEN>。
// ==================================================================
pipeline {
    agent {
        node {
            label 'built-in'
            customWorkspace "/home/busizeshi/docker/ci-workspace/${env.JOB_NAME}"
        }
    }

    options {
        timestamps()
        timeout(time: 40, unit: 'MINUTES')
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
        skipDefaultCheckout(true)
    }

    parameters {
        booleanParam(name: 'DEPLOY', defaultValue: true, description: '构建成功后部署（取消勾选则只构建镜像）')
        string(name: 'IMAGE_TAG', defaultValue: '', description: '镜像标签，留空自动使用 build-<构建号>')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: '跳过单元测试（仅紧急联调时使用）')
    }

    environment {
        // 服务器无法直连 Docker Hub，统一走镜像站
        REGISTRY      = 'docker.m.daocloud.io'
        MAVEN_IMAGE   = "${REGISTRY}/library/maven:3.9-eclipse-temurin-21"
        PYTHON_IMAGE  = "${REGISTRY}/library/python:3.12-slim"
        COMPOSE_CMD   = 'docker compose -f deploy/docker-compose.yml --env-file deploy/.env.example'
        PIP_INDEX     = 'https://mirrors.aliyun.com/pypi/simple/'
        IMAGE_TAG     = "${params.IMAGE_TAG?.trim() ? params.IMAGE_TAG.trim() : 'build-' + env.BUILD_NUMBER}"
    }

    stages {
        stage('代码检出') {
            steps {
                checkout scm
                sh 'git log -1 --pretty="%h %s (%an, %ad)" --date=iso'
                sh 'git rev-parse --abbrev-ref HEAD'
            }
        }

        stage('构建 Java（3 个服务）') {
            steps {
                sh """
                    docker run --rm \\
                      -v "\${WORKSPACE}/fafa-java":/src \\
                      -v fafa-maven-repo:/root/.m2/repository \\
                      -v "\${WORKSPACE}/deploy/maven/settings.xml":/root/.m2-settings.xml:ro \\
                      -w /src \\
                      ${MAVEN_IMAGE} \\
                      mvn -B -ntp -s /root/.m2-settings.xml clean package ${params.SKIP_TESTS ? '-DskipTests' : ''}
                """
                sh 'ls -lh fafa-java/*/target/*.jar'
            }
        }

        stage('测试 Python 服务') {
            steps {
                sh """
                    docker run --rm \\
                      -v "\${WORKSPACE}/fafa-python":/app \\
                      -w /app \\
                      -e PIP_INDEX_URL=${PIP_INDEX} \\
                      -e PIP_TRUSTED_HOST=mirrors.aliyun.com \\
                      ${PYTHON_IMAGE} \\
                      bash -lc "pip install -q uv && uv sync --frozen && uv run ruff check . && uv run pytest -q"
                """
            }
        }

        stage('构建镜像') {
            steps {
                sh """
                    ${COMPOSE_CMD} build --parallel
                """
                sh """
                    docker images --format 'table {{.Repository}}\\t{{.Tag}}\\t{{.Size}}' | grep -E 'fafa/(fafa-)?(gateway|biz|admin|python)' || true
                """
            }
        }

        stage('部署到开发服务器') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                sh """
                    ${COMPOSE_CMD} up -d --remove-orphans --no-build
                """
                sh """
                    docker ps --filter 'name=fafa-' --format 'table {{.Names}}\\t{{.Image}}\\t{{.Status}}\\t{{.Ports}}'
                """
            }
        }

        stage('健康检查与冒烟测试') {
            when {
                expression { return params.DEPLOY }
            }
            steps {
                script {
                    def services = ['fafa-biz', 'fafa-admin', 'fafa-python', 'fafa-gateway']
                    def healthy = false
                    for (int i = 0; i < 36; i++) {
                        healthy = services.every { svc ->
                            def out = sh(
                                returnStdout: true,
                                script: "docker inspect -f '{{if .State.Health}}{{.State.Health.Status}}{{else}}{{.State.Status}}{{end}}' ${svc} 2>/dev/null || echo missing"
                            ).trim()
                            echo "${svc} -> ${out}"
                            return out == 'healthy'
                        }
                        if (healthy) {
                            break
                        }
                        sleep(time: 5, unit: 'SECONDS')
                    }
                    if (!healthy) {
                        sh "${COMPOSE_CMD} ps"
                        sh "${COMPOSE_CMD} logs --tail=120"
                        error '服务未在 3 分钟内全部进入 healthy 状态，请查看上方容器日志'
                    }
                }
                // 冒烟：经网关调用 biz 的 bootstrap 接口（验证 Nacos 发现 + 路由 + 下游）
                sh """
                    docker exec fafa-gateway curl -fsS -o /dev/null -w 'GET /api/v1/bootstrap -> %{http_code}\\n' http://127.0.0.1:9090/api/v1/bootstrap
                """
            }
        }
    }

    post {
        success {
            echo "构建完成：镜像标签 ${env.IMAGE_TAG}"
            archiveArtifacts artifacts: 'fafa-java/*/target/*.jar', allowEmptyArchive: true, fingerprint: true
        }
        failure {
            sh 'docker compose -f deploy/docker-compose.yml --env-file deploy/.env.example ps || true'
        }
        always {
            // 清理悬空镜像，避免服务器磁盘被旧图层占满
            sh 'docker image prune -f >/dev/null 2>&1 || true'
        }
    }
}
