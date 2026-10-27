package com.fafa.admin.interfaces;

import com.fafa.common.api.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口（fafa-admin 脚手架示例）。
 *
 * <p>验证链路：网关 {@code /admin/api/v1/**} 路由 → Nacos 发现 → 本服务响应。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@RestController
@RequestMapping("/admin/api/v1")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /**
     * 健康检查。
     *
     * @return 服务标识、状态与服务器 UTC 时间
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        log.info("[健康检查] admin 服务收到健康检查请求");

        Map<String, String> body = new LinkedHashMap<>();
        body.put("service", "fafa-admin");
        body.put("status", "UP");
        body.put("serverTime", OffsetDateTime.now().toString());

        return ApiResponse.ok(body);
    }
}
