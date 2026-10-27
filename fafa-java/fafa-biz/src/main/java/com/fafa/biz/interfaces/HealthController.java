package com.fafa.biz.interfaces;

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
 * 健康检查接口（脚手架示例：演示统一响应结构与日志规范）。
 *
 * <p>说明：</p>
 * <ul>
 *   <li>实例级探针由 Actuator 提供（/actuator/health/liveness、/readiness）；</li>
 *   <li>本接口面向前端联调与链路验证，返回服务标识与服务器 UTC 时间，
 *       可用于验证网关路由 → Nacos 发现 → biz 响应的完整链路；</li>
 *   <li>业务模块就绪后本控制器可保留或按需移除。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/v1")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    /**
     * 健康检查：验证网关路由与 Nacos 注册链路。
     *
     * @return 服务名、状态与服务器当前时间（ISO 8601 带时区）
     */
    @GetMapping("/health")
    public ApiResponse<Map<String, String>> health() {
        log.info("[健康检查] biz 服务收到健康检查请求");

        Map<String, String> body = new LinkedHashMap<>();
        body.put("service", "fafa-biz");
        body.put("status", "UP");
        // 统一输出带时区的 ISO 8601 时间（架构规范 §9.1：时间一律带时区交换）
        body.put("serverTime", OffsetDateTime.now().toString());

        return ApiResponse.ok(body);
    }
}
