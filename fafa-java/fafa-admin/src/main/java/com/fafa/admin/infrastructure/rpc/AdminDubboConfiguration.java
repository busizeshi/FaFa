package com.fafa.admin.infrastructure.rpc;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.context.annotation.*;

@Configuration(proxyBeanMethods = false)
@Profile("!admin-init & !test")
@EnableDubbo
public class AdminDubboConfiguration { }
