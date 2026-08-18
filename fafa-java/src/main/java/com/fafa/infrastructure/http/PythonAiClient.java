package com.fafa.infrastructure.http;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Python AI 服务客户端
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAiClient {

    private final RestTemplate restTemplate;

    @Value("${fafa.ai-service.base-url}")
    private String baseUrl;

    /**
     * 健康检查
     * 
     * @return 健康状态
     */
    public Map<String, Object> healthCheck() {
        String url = baseUrl + "/health";
        return restTemplate.getForObject(url, Map.class);
    }

    /**
     * 调用 AI 服务（通用方法）
     * 
     * @param endpoint 端点路径
     * @param request 请求体
     * @param responseType 响应类型
     * @return 响应结果
     */
    public <T> T callAiService(String endpoint, Object request, Class<T> responseType) {
        String url = baseUrl + endpoint;
        log.debug("调用 Python AI 服务: {}", url);
        return restTemplate.postForObject(url, request, responseType);
    }
}
