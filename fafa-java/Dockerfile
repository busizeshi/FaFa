# Java 构建阶段
FROM maven:3.9-amazoncorretto-21 AS builder

WORKDIR /build

# 复制 pom.xml 并下载依赖（利用 Docker 缓存）
COPY pom.xml .
RUN mvn dependency:go-offline

# 复制源码并构建
COPY src ./src
RUN mvn clean package -DskipTests

# 运行阶段
FROM amazoncorretto:21-alpine

WORKDIR /app

# 复制构建产物
COPY --from=builder /build/target/fafa-java-*.jar app.jar

# 暴露端口
EXPOSE 8080

# 启动应用
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
