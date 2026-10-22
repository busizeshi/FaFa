package com.fafa.biz.testing;

import cn.dev33.satoken.dao.SaTokenDao;
import cn.dev33.satoken.dao.SaTokenDaoDefaultImpl;
import com.fafa.common.web.CommonExceptionHandler;
import com.fafa.common.web.CommonWebMvcConfiguration;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.*;

@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan({"com.fafa.biz.identity", "com.fafa.biz.pet", "com.fafa.biz.usage", "com.fafa.biz.operation", "com.fafa.biz.admin"})
@Import({CommonExceptionHandler.class, CommonWebMvcConfiguration.class,
        com.fafa.biz.admin.interfaces.rpc.AdminBusinessRpcProvider.class})
public class IdentityTestApplication {
    /** 该适配器仅用于测试，不能证明 Redis 集成可用。 */
    @Bean
    @Primary
    public SaTokenDao testSessionDao() { return new SaTokenDaoDefaultImpl(); }
}
