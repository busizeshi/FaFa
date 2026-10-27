# ==================================================================
# FaFa Java 服务运行镜像（gateway / biz / admin 三服务共用）
#
# 构建方式：由 Jenkins 在 Maven 容器中产出 jar 后，以仓库根为构建上下文执行：
#   docker build -f deploy/java/Dockerfile \
#     --build-arg JAR_FILE=fafa-java/fafa-biz/target/fafa-biz-1.0.0-SNAPSHOT.jar .
#
# 说明：
#   1. 只放运行时（JRE 21），构建产物由流水线提供，镜像内不含源码与 Maven；
#   2. 安装 curl 仅供 compose healthcheck 与排障使用；
#   3. JVM 内存按容器限额百分比分配，避免写死 -Xmx 与宿主机内存不匹配。
# ==================================================================
FROM docker.m.daocloud.io/library/eclipse-temurin:21-jre

# 构建参数：jar 相对仓库根目录的路径（三个服务复用同一 Dockerfile）
ARG JAR_FILE

ENV TZ=Asia/Shanghai \
    JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError" \
    SPRING_PROFILES_ACTIVE=dev

RUN apt-get update \
 && apt-get install -y --no-install-recommends curl tzdata \
 && rm -rf /var/lib/apt/lists/* \
 && ln -snf /usr/share/zoneinfo/$TZ /etc/localtime \
 && echo $TZ > /etc/timezone

WORKDIR /app

# 只拷贝 jar：COPY 源路径相对构建上下文（仓库根）
COPY ${JAR_FILE} /app/app.jar

# 默认端口（各服务在 compose 中通过 server.port 覆盖，无需 EXPOSE 生效）
EXPOSE 8080

# exec 让 java 成为 PID 1，正确接收 SIGTERM 实现优雅停机
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
