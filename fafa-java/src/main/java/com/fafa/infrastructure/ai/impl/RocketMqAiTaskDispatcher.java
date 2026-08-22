package com.fafa.infrastructure.ai.impl;

import com.fafa.infrastructure.ai.AiTaskDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

/**
 * RocketMQ 异步消息 AI 任务分发实现（生产环境）
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "fafa.ai.dispatcher", havingValue = "rocketmq")
public class RocketMqAiTaskDispatcher implements AiTaskDispatcher {

    private static final String TOPIC_PROFILE_PHOTO_VECTORIZATION = "fafa_profile_photo_vectorization";

    private final RocketMQTemplate rocketMQTemplate;

    @Override
    public void dispatchProfilePhotoVectorization(Long petId, MultipartFile frontPhoto,
                                                 MultipartFile sidePhoto, String frontUrl,
                                                 String sideUrl) {
        try {
            Map<String, Object> message = new HashMap<>();
            message.put("petId", petId);
            message.put("frontUrl", frontUrl);
            message.put("sideUrl", sideUrl);
            message.put("timestamp", System.currentTimeMillis());

            rocketMQTemplate.convertAndSend(TOPIC_PROFILE_PHOTO_VECTORIZATION, message);

            log.info("RocketMQ dispatched profile photo vectorization: petId={}, topic={}", 
                petId, TOPIC_PROFILE_PHOTO_VECTORIZATION);

        } catch (Exception ex) {
            log.error("Failed to dispatch profile photo vectorization via RocketMQ: petId={}", petId, ex);
            throw new RuntimeException("AI task dispatch failed", ex);
        }
    }
}
