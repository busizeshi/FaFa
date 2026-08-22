package com.fafa.infrastructure.ai.impl;

import com.fafa.infrastructure.ai.AiTaskDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 异步消息 AI 任务分发实现（生产环境）
 *
 * 消息体只含 URL，不含二进制文件，Python 消费者拉取后调用 qwen3-vl-embedding。
 * 适用于生产环境削峰填谷，避免高并发时直接调用 Python 导致雪崩。
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

    private final RocketMQTemplate rocketMQTemplate;

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
}
