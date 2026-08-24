package com.fafa.infrastructure.ai.impl;

import com.fafa.application.port.FoodRecognitionGateway;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/** 食品包装识别 HTTP 适配器。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpFoodRecognitionGateway implements FoodRecognitionGateway {

    private final RestClient pythonRestClient;
    private final ObjectMapper objectMapper;

    @Override
    public FoodRecognitionResult recognize(FoodRecognitionRequest request) {
        try {
            JsonNode response = pythonRestClient.post()
                    .uri("/api/foods/recognize")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Trace-Id", request.traceId())
                    .body(request)
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null || !response.has("draft")) {
                throw new IllegalStateException("食品识别响应缺少 draft");
            }
            return new FoodRecognitionResult(response.get("draft"), response,
                    response.path("model").asText(null), response.path("promptVersion").asText(null),
                    readWarnings(response.get("warnings")));
        } catch (Exception ex) {
            log.error("食品包装识别调用失败: taskId={}, mediaId={}", request.taskId(), request.mediaId(), ex);
            throw new IllegalStateException("食品识别服务不可用", ex);
        }
    }

    private List<String> readWarnings(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, String.class));
    }
}
