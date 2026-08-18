# FaFa Pet Assistant - Java Service

宠物生活助手 Java 业务服务，基于 Spring Boot 3.x + DDD 架构。

## 技术栈

- **框架**: Spring Boot 3.2.5 + JDK 21
- **数据访问**: MyBatis-Plus 3.5.6
- **鉴权**: Sa-Token 1.37.0
- **缓存**: Redisson 3.27.2
- **消息队列**: RocketMQ 2.3.0
- **API 文档**: Knife4j 4.5.0

## 项目结构（DDD 分层）

```
src/main/java/com/fafa/
├── interfaces/          # 用户接口层
│   ├── controller/     # REST API 控制器
│   └── dto/            # 数据传输对象
├── application/        # 应用服务层
│   └── service/        # 应用服务（协调领域对象）
├── domain/            # 领域层
│   ├── model/         # 领域实体和值对象
│   ├── service/       # 领域服务
│   ├── repository/    # 仓储接口
│   └── event/         # 领域事件
├── infrastructure/    # 基础设施层
│   ├── persistence/   # 持久化实现
│   ├── config/        # 配置类
│   └── external/      # 外部服务适配器
└── common/           # 公共模块
    ├── exception/    # 异常类
    └── result/       # 统一响应
```

## 快速开始

### 1. 配置数据库

修改 `application.yml` 或设置环境变量：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/fafa
    username: root
    password: password
```

### 2. 构建项目

```bash
mvn clean package
```

### 3. 运行

```bash
java -jar target/fafa-java-1.0.0.jar
```

### 4. 访问 API 文档

http://localhost:8080/doc.html

## 开发规范

### DDD 设计原则

1. **领域层独立**：业务逻辑在 domain 层，不依赖框架
2. **Repository 模式**：接口在 domain 层定义，实现在 infrastructure 层
3. **充血模型**：实体包含业务行为，避免贫血模型
4. **领域事件**：使用事件解耦聚合之间的交互

### 代码示例

**创建宠物（工厂方法）**
```java
Pet pet = Pet.create(userId, name, species);
petRepository.save(pet);
```

**更新体重（业务行为）**
```java
Pet pet = petRepository.findById(petId);
pet.updateWeight(newWeight);  // 业务逻辑在实体内
petRepository.save(pet);
```

## 测试

```bash
mvn test
```

## 构建 Docker 镜像

```bash
docker build -t fafa-java:1.0.0 .
```
