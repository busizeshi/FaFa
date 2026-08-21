package com.fafa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * FaFa 业务服务启动类
 *
 * 单体架构 + DDD 分层（interfaces / application / domain / infrastructure）
 * 依赖方向：interfaces -> application -> domain <- infrastructure
 */
@SpringBootApplication
public class FaFaApplication {

    public static void main(String[] args) {
        SpringApplication.run(FaFaApplication.class, args);
    }
}
