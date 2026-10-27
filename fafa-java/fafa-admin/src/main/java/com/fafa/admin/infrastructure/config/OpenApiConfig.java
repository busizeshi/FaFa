package com.fafa.admin.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI 文档装配（fafa-admin）。
 *
 * <p>约定（架构规范 §3 / §9.1）：</p>
 * <ul>
 *   <li>OpenAPI 契约是接口事实来源：改接口先改契约与 DTO，再写实现；</li>
 *   <li>生产环境通过 {@code springdoc.api-docs.enabled=false} 关闭，
 *       或置于强认证 + 网段限制之后；</li>
 *   <li>认证方案为员工会话令牌头部，token 名称与 sa-token.token-name 保持一致。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Configuration
public class OpenApiConfig {

    /** 认证 scheme 名称：与 Sa-Token 员工会话 token-name 保持一致 */
    private static final String SECURITY_SCHEME_NAME = "fafa-admin-token";

    /**
     * 构建 OpenAPI 描述信息。
     *
     * @return OpenAPI 根对象
     */
    @Bean
    public OpenAPI adminOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("FaFa Admin API")
                        .description("""
                                FaFa 宠物照护平台运营后台接口。
                                客户端请统一通过网关（/admin/api/v1 前缀）访问，本服务不对公网开放。
                                """)
                        .version("v1"))
                // 调试用服务地址："/" 表示当前访问入口（网关聚合页即网关根路径，直连即本服务）
                .addServersItem(new Server().url("/").description("当前访问入口（网关或直连）"))
                .addServersItem(new Server().url("http://localhost:8082").description("本地直连 8082"))
                // 请求头令牌认证：调试点 Authorize 后统一携带
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.APIKEY)
                                        .in(SecurityScheme.In.HEADER)
                                        .name("fafa-admin-token")
                                        .description("Sa-Token 员工会话令牌（后台登录接口获取）")));
    }
}
