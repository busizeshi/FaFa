package com.fafa.biz.admin.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.common.rpc.RpcAuthentication;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InternalRpcConfiguration {
    @Bean public RpcAuthentication businessRpcAuthentication(@Value("${fafa.internal-rpc.secret}") String secret,
            @Value("${fafa.internal-rpc.max-skew-seconds:120}") long skew, Clock clock, ObjectMapper json) {
        return new RpcAuthentication(secret, clock, skew, json);
    }
}
