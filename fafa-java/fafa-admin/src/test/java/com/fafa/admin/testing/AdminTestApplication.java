package com.fafa.admin.testing;

import com.fafa.common.web.CommonExceptionHandler;
import com.fafa.common.web.CommonWebMvcConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;

/** 使用独立管理库测试上下文，不启动注册中心或真实 RPC 客户端。 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan({"com.fafa.admin.security", "com.fafa.admin.audit", "com.fafa.admin.interfaces.http", "com.fafa.admin.infrastructure.rpc"})
@Import({CommonExceptionHandler.class, CommonWebMvcConfiguration.class})
public class AdminTestApplication { }
