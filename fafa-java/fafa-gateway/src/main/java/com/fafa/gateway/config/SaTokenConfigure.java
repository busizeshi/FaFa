package com.fafa.gateway.config;

import cn.dev33.satoken.context.SaHolder;
import cn.dev33.satoken.reactor.filter.SaReactorFilter;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.fafa.common.constant.HeaderConstants;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

/**
 * 网关会话校验过滤器（架构规范 §4.1：Gateway 先行校验 Sa-Token 会话）。
 *
 * <p>规则：</p>
 * <ul>
 *   <li>宠主 API {@code /api/v1/**}：除公开端点外统一校验会话；</li>
 *   <li>运营 API {@code /admin/api/v1/**}：M 阶段接入员工会话后启用（当前放行）；</li>
 *   <li>公开清单与 fafa-biz 的 SaTokenConfig 保持一致，修改必须两侧同步；</li>
 *   <li>校验失败返回 401 + 统一响应结构（与 biz 的错误结构一致）。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Configuration
public class SaTokenConfigure {

    /** M00 公开端点（与 biz SaTokenConfig.PUBLIC_PATHS 对齐） */
    private static final String[] PUBLIC_API_PATHS = {
            "/api/v1/bootstrap",
            "/api/v1/health",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/challenges/**",
            "/api/v1/auth/password-reset-challenges",
            "/api/v1/auth/password-reset-challenges/**",
            "/api/v1/auth/password-resets"
    };

    /**
     * 响应式会话校验过滤器。
     *
     * @return SaReactorFilter
     */
    @Bean
    public SaReactorFilter saReactorFilter() {
        return new SaReactorFilter()
                // 拦截宠主 API
                .addInclude("/api/v1/**")
                // 公开端点放行
                .addExclude(PUBLIC_API_PATHS)
                // 文档/探针放行
                .addExclude("/v3/api-docs/**", "/actuator/**", "/swagger-ui/**")
                // 鉴权动作：会话有效即放行（细粒度授权由下游 biz 执行）
                .setAuth(obj -> SaRouter.match("/**").check(r -> StpUtil.checkLogin()))
                // 401 响应：与 biz 全局异常处理器输出结构一致；
                // 必须显式设置 HTTP 状态码（Sa-Token 过滤器默认回 200）
                .setError(e -> {
                    SaHolder.getResponse().setStatus(401);
                    String body = "{\"code\":\"401-00-00\",\"message\":\"登录状态已失效，请重新登录\","
                            + "\"data\":null,\"requestId\":\"-\",\"retryable\":false}";
                    return new String(body.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
                });
    }
}
