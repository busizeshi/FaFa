package com.fafa.infrastructure.ai.impl;

import com.fafa.infrastructure.ai.AiTaskDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 异步消息 AI 任务分发实现（生产环境）
 *
 * 消息体只含 URL，不含二进制文件，Python 消费者拉取后调用 qwen3-vl-embedding。
 * 适用于生产环境削峰填谷，避免高并发时直接调用 Python 导致雪崩。
 *
 * 向量清理为轻量幂等操作，不走消息队列，直接 HTTP 调用 Python。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.ai.dispatcher", havingValue = "rocketmq")
public class RocketMqAiTaskDispatcher implements AiTaskDispatcher {

    private static final String TOPIC_PET_PHOTO_VECTORIZATION = "fafa_pet_photo_vectorization";
    private static final String TOPIC_PHOTO_ANALYSIS = "fafa_photo_analysis_topic";

    private final RocketMQTemplate rocketMQTemplate;

    /** 按名称注入 pythonRestClient，用于轻量幂等的向量清理 HTTP 调用 */
    private final RestClient pythonRestClient;

    @Override
    public void dispatchPetPhotoVectorization(Long petId, Long userId,
                                              String avatarUrl, String frontPhotoUrl, String sidePhotoUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("petId", petId);
            message.put("userId", userId);
            message.put("avatarUrl", avatarUrl);
            message.put("frontUrl", frontPhotoUrl);
            message.put("sideUrl", sidePhotoUrl);
            message.put("timestamp", System.currentTimeMillis());

            rocketMQTemplate.convertAndSend(TOPIC_PET_PHOTO_VECTORIZATION, message);

            log.info("RocketMQ 宠物照片向量化任务已分发: petId={}, userId={}, topic={}",
                petId, userId, TOPIC_PET_PHOTO_VECTORIZATION);

        } catch (Exception ex) {
            log.error("RocketMQ 分发宠物照片向量化失败: petId={}, userId={}", petId, userId, ex);
            // 不抛异常，避免影响主流程
        }
    }

    @Override
    public void cleanupPetVectors(Long petId) {
        try {
            pythonRestClient.delete()
                    .uri("/api/pets/{petId}/profile-vectors", petId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("HTTP 宠物向量清理任务已分发: petId={}", petId);

        } catch (Exception ex) {
            log.error("HTTP 分发宠物向量清理失败: petId={}", petId, ex);
            // 不抛异常，避免影响删除主流程（残留向量可由对账任务兜底）
        }
    }

    @Override
    public void dispatchMediaAnalysis(Long mediaId, Long userId, String url,
                                      String mediaType, java.util.List<String> tags, Long petId,
                                      java.time.LocalDateTime takenAt) {
        try {
            // 消息体 camelCase（对齐既有宠物向量化消息风格），messageId 幂等键
            Map<String, Object> message = new HashMap<>();
            message.put("messageId", "media-" + mediaId);
            message.put("traceId", MDC.get("traceId"));
            message.put("userId", userId);
            message.put("photoId", mediaId);
            message.put("url", url);
            message.put("mediaType", mediaType);
            message.put("tags", tags);
            message.put("petId", petId);
            message.put("takenAt", takenAt);
            message.put("timestamp", System.currentTimeMillis());

            rocketMQTemplate.convertAndSend(TOPIC_PHOTO_ANALYSIS + ":photo", message);

            log.info("RocketMQ 素材分析任务已分发: mediaId={}, userId={}, topic={}",
                mediaId, userId, TOPIC_PHOTO_ANALYSIS);

        } catch (Exception ex) {
            log.error("RocketMQ 分发素材分析失败: mediaId={}, userId={}", mediaId, userId, ex);
            // 不抛异常，避免影响上传主流程（素材停留 pending，人工兜底）
        }
    }

    @Override
    public void deleteMediaVector(Long mediaId) {
        try {
            pythonRestClient.delete()
                    .uri("/api/vector/media_{mediaId}", mediaId)
                    .retrieve()
                    .toBodilessEntity();

            log.info("HTTP 素材向量删除任务已分发: mediaId={}", mediaId);

        } catch (Exception ex) {
            log.error("HTTP 分发素材向量删除失败: mediaId={}", mediaId, ex);
            // 不抛异常，残留 point 由 Java 侧 is_deleted 二次过滤免疫
        }
    }
}
