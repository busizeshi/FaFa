package com.fafa.infrastructure.client;

import com.fafa.common.BusinessException;
import com.fafa.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * fafa-python AI 服务客户端（仅限 Java 侧内网调用）
 *
 * 同步轻量链路（语义检索、对话、健康检查）走本客户端；
 * 重 AI 负载（照片理解、向量化）走 RocketMQ 异步链路，
 * Windows 开发环境无 MQ 消费能力时降级为直接调用 analyze 接口。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PythonAiClient {

    private final RestClient pythonRestClient;

    /**
     * 健康检查：Python 服务是否可用
     *
     * @return true 可用；false 不可用（不抛异常，供健康探针与降级判断）
     */
    public boolean health() {
        try {
            pythonRestClient.get().uri("/health").retrieve().toBodilessEntity();
            return true;
        } catch (Exception ex) {
            log.warn("fafa-python 健康检查失败: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 触发照片分析（Windows 开发环境的 MQ 替代链路）
     *
     * @throws BusinessException Python 服务不可用或调用失败
     */
    public void analyzePhoto(PhotoAnalyzeRequest request) {
        try {
            pythonRestClient.post()
                    .uri("/api/photos/analyze")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Trace-Id", currentTraceId())
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
            log.info("照片分析任务已提交: photoId={}, messageId={}", request.getPhotoId(), request.getMessageId());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("调用 Python 照片分析失败: photoId={}", request.getPhotoId(), ex);
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        }
    }

    /** 优先取当前链路 traceId（Micrometer Tracing 写入 MDC），无则生成 */
    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return (traceId == null || traceId.isBlank()) ? "no-trace" : traceId;
    }
}
