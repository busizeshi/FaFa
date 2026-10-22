package com.fafa.admin;

import com.fafa.common.web.CommonExceptionHandler;
import com.fafa.common.web.CommonWebMvcConfiguration;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({CommonExceptionHandler.class, CommonWebMvcConfiguration.class})
public class AdminApplication {
    public static void main(String[] args) {
        var context = SpringApplication.run(AdminApplication.class, args);
        if (context.getEnvironment().matchesProfiles("admin-init")) {
            System.exit(SpringApplication.exit(context));
        }
    }
}
