package com.fafa.biz.infrastructure.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

/**
 * Sa-Token 拦截规则与事务模板装配（fafa-biz）。
 *
 * <p>认证基线（架构规范 §4.1 / §4.3）：</p>
 * <ul>
 *   <li>除公开端点外，全部 {@code /api/v1/**} 需要有效宠主会话；</li>
 *   <li>网关已先行校验会话，本服务仍独立校验原始凭据（"网关校验不是唯一授权"）；</li>
 *   <li>公开清单必须与 Gateway 的 SaReactorFilter exclude 清单保持一致。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    /** M00 公开端点清单（auth 匿名链路 + 探针/文档） */
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/bootstrap",
            "/api/v1/health",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/challenges/**",
            "/api/v1/auth/password-reset-challenges",
            "/api/v1/auth/password-reset-challenges/**",
            "/api/v1/auth/password-resets",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/actuator/**",
            "/error");

    /** {@inheritDoc}：注册登录校验拦截器 */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handler -> StpUtil.checkLogin()))
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(PUBLIC_PATHS);
    }

    /**
     * 编程式事务模板：注册/激活等用例需要精确控制"事务提交后再发邮件/签发会话"。
     *
     * @param transactionManager Boot 自动装配的数据源事务管理器
     * @return 事务模板
     */
    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}
