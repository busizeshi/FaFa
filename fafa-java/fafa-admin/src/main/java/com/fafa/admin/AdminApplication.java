package com.fafa.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * FaFa 运营后台服务启动类（fafa-admin）。
 *
 * <p>职责（架构规范 §4.2）：运营员工身份与角色、社区审核队列、举报/申诉处置、
 * 运营工单与后台操作审计。对业务数据的修改只能通过 biz 提供的后台 API 或
 * Dubbo 接口完成，本服务不直接写 biz 数据库。</p>
 *
 * <p><strong>身份隔离</strong>：员工会话与宠主会话是两套独立身份空间（不同 token 命名空间
 * 与 audience），宠主 token 不能访问运营 API，员工 token 也不被 biz 当成宠主身份。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@SpringBootApplication
@EnableDiscoveryClient
@MapperScan(basePackages = "com.fafa.admin", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }
}
