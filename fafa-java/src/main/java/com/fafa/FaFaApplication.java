package com.fafa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * FaFa 业务服务启动类
 *
 * @author FaFa Team
 * @since 1.0
 */
@SpringBootApplication
@MapperScan("com.fafa.infrastructure.persistence")
@ConfigurationPropertiesScan("com.fafa.infrastructure.config")
public class FaFaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaFaApplication.class, args);
    }
}
