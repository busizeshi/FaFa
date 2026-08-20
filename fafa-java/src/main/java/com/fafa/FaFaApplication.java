package com.fafa;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * FaFa 宠物生活助手 - 主启动类
 * 
 * @author FaFa Team
 * @since 1.0
 */
@SpringBootApplication
@MapperScan("com.fafa.infrastructure.persistence.mapper")
@EnableScheduling
public class FaFaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaFaApplication.class, args);
    }
}
