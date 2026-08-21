package com.fafa.infrastructure.client;

import com.fafa.infrastructure.web.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Python AI 服务客户端
 *
 * 服务间契约（与 fafa-python 路由逐字节对齐，变更须双侧同步）：
 * - POST /api/photos/analyze        媒体理解与向量化（Windows 开发链路直推）
 * - POST /api/photos/search         语义检索
 * - POST /api/pets/profile-photos   三视图向量化
 * - DELETE /api/pets/{petId}/profile-vectors  清理宠物向量
 * - POST /api/chat/send             AI 对话
 */
@Slf4j
@Service
public class PythonAiClient {

    private final RestClient restClient;
    private final String internalToken;

    public PythonAiClient(RestClient.Builder builder,
                          @Value("${fafa.python.base-url}") String baseUrl,
                          @Value("${fafa.internal-token}") String internalToken) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.internalToken = internalToken;
    }

    /**
     * 触发媒体分析（Windows 开发环境无 MQ 时的直推链路）
     */
    public void analyzeMedia(Map<String, Object> requestBody) {
        post("/api/photos/analyze", requestBody);
    }

    /**
     * 语义检索照片（同步，供搜索接口转发）
     */
    public Map<String, Object> searchPhotos(Map<String, Object> requestBody) {
        return postForBody("/api/photos/search", requestBody);
    }

    /**
     * 三视图向量化（宠物创建/更新后触发）
     */
    public void uploadPetProfilePhotos(Map<String, Object> requestBody) {
        post("/api/pets/profile-photos", requestBody);
    }

    /**
     * 删除宠物的三视图向量（宠物删除时尽力清理）
     */
    public void deletePetProfileVectors(Long petId) {
        try {
            restClient.delete()
                    .uri("/api/pets/{petId}/profile-vectors", petId)
                    .headers(this::applyInternalHeaders)
                    .retrieve()
                    .toBodilessEntity();
            log.info("宠物向量清理请求已发送: petId={}", petId);
        } catch (Exception e) {
            log.warn("宠物向量清理失败（尽力而为，不阻断）: petId={}", petId, e);
        }
    }

    /**
     * AI 对话转发（流式场景由控制器直连，此处供非流式调用）
     */
    public Map<String, Object> sendChat(Map<String, Object> requestBody) {
        return postForBody("/api/chat/send", requestBody);
    }

    // ------------------------------------------------------------------

    private void post(String path, Map<String, Object> body) {
        try {
            restClient.post()
                    .uri(path)
                    .headers(this::applyInternalHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("调用 Python 服务失败: path={}", path, e);
            throw new com.fafa.domain.exception.BusinessException(
                    com.fafa.domain.common.ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> postForBody(String path, Map<String, Object> body) {
        try {
            return restClient.post()
                    .uri(path)
                    .headers(this::applyInternalHeaders)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (Exception e) {
            log.error("调用 Python 服务失败: path={}", path, e);
            throw new com.fafa.domain.exception.BusinessException(
                    com.fafa.domain.common.ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 内部令牌 + traceId 透传，Python 侧日志可串联同一链路
     */
    private void applyInternalHeaders(HttpHeaders headers) {
        headers.set("X-Internal-Token", internalToken);
        String traceId = MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY);
        if (traceId != null) {
            headers.set(TraceIdFilter.TRACE_ID_HEADER, traceId);
        }
    }

    /** 预留：供需要原始 entity 的场景使用 */
    private HttpEntity<Map<String, Object>> buildEntity(Map<String, Object> body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        applyInternalHeaders(headers);
        return new HttpEntity<>(body, headers);
    }
}
