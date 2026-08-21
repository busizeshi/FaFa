package com.fafa.infrastructure.dispatch;

import com.fafa.infrastructure.client.PythonAiClient;
import com.fafa.infrastructure.mq.PhotoAnalysisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * HTTP 直推策略（MQ 不可用时的兜底，Windows 开发环境主链路）
 *
 * 请求体 snake_case，与 fafa-python AnalyzeMediaRequest 对齐；
 * Python 侧 MQ 消费与 HTTP 直推复用同一处理入口，两条链路语义一致。
 */
@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class HttpDispatchStrategy implements AiTaskDispatchStrategy {

    private final PythonAiClient pythonAiClient;

    @Override
    public boolean supports() {
        return true;
    }

    @Override
    public boolean dispatch(PhotoAnalysisMessage message) {
        Map<String, Object> body = new HashMap<>();
        body.put("photo_id", message.getPhotoId());
        body.put("user_id", message.getUserId());
        body.put("pet_id", message.getPetId());
        body.put("url", message.getUrl());
        body.put("media_type", message.getMediaType());
        body.put("tags", message.getTags());
        body.put("trace_id", message.getTraceId());
        pythonAiClient.analyzeMedia(body);
        log.info("AI 任务已 HTTP 直推: photoId={}", message.getPhotoId());
        return true;
    }
}
