package com.fafa.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * FaFa 网关服务启动类。
 *
 * <p>外部统一 HTTPS/HTTP 入口，路由规则与安全过滤见：
 * {@code application-dev.yml}（路由）、
 * {@link com.fafa.gateway.filter.HeaderSanitizeGlobalFilter}（剥离伪造身份头）、
 * {@link com.fafa.gateway.filter.RequestIdGlobalFilter}（requestId 生成与访问日志）。</p>
 *
 * <p><strong>职责红线</strong>（架构规范 §4.1）：本服务不访问业务数据库、不判断宠物/家庭资源权限、
 * 不计算任何业务规则；身份仅做会话有效性校验，细粒度授权由下游 fafa-biz / fafa-admin 完成。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
