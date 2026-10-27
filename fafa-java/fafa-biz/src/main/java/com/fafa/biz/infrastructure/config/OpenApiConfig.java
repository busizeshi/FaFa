package com.fafa.biz.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * springdoc-openapi 文档装配（fafa-biz）。
 *
 * <p>约定（架构规范 §3 / §9.1）：</p>
 * <ul>
 *   <li>OpenAPI 契约是接口事实来源：改接口先改契约与 DTO，再写实现；</li>
 *   <li>生产环境通过 {@code springdoc.api-docs.enabled=false} 关闭，
 *       或置于强认证 + 网段限制之后；</li>
 *   <li>认证方案占位为 Sa-Token 头部令牌，认证模块实施后统一对齐 token 名称。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /** 认证 scheme 名称：与 Sa-Token token-name 保持一致（见 application.yml） */
    private static final String SECURITY_SCHEME_NAME = "fafa-token";

    /**
     * 构建 OpenAPI 描述信息。
     *
     * @return OpenAPI 根对象
     */
    @Bean
    public OpenAPI bizOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FaFa Biz API")
                        .description("""
                                FaFa 宠物照护平台业务服务接口。
                                客户端请统一通过网关（/api/v1 前缀）访问，本服务不对公网开放。
                                所有写操作建议携带 Idempotency-Key；并发修改需携带 revision。
                                """)
                        .version("v1"))
                // 文档页调试用的本地地址（生产不暴露）
                .addServersItem(new Server().url("http://localhost:8081").description("本地开发"))
                // 请求头令牌认证：调试点 Authorize 后统一携带
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("fafa-token")
                                        .description("Sa-Token 会话令牌（登录接口获取）")));
    }
}
