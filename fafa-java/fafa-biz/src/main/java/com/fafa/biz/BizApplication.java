package com.fafa.biz;

import com.fafa.common.web.CommonExceptionHandler;
import com.fafa.common.web.CommonWebMvcConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableDubbo
@Import({CommonExceptionHandler.class, CommonWebMvcConfiguration.class})
public class BizApplication {
    public static void main(String[] args) {
        SpringApplication.run(BizApplication.class, args);
    }
}
