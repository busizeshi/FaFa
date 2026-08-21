package com.fafa.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Knife4j / OpenAPI 文档配置
 *
 * dev/test 环境开启，prod 通过 knife4j.production=true 关闭
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI fafaOpenApi() {
        return new OpenAPI().info(new Info()
                .title("FaFa 业务服务 API")
                .description("FaFa 宠物生命周期管理小程序后端接口")
                .version("0.1.0"));
    }
}
