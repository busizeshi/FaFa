package com.fafa.biz.pet.infrastructure;

import com.fafa.biz.pet.application.PetPolicy;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({PetPolicy.class, com.fafa.biz.operation.Paging.class})
public class PetConfiguration { }
