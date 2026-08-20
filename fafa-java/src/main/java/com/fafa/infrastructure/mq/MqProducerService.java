package com.fafa.infrastructure.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;

/**
 * MQ 生产者服务
 *
 * @author FaFa
 * @since 2026-08-18
 */
@Slf4j
@Service
public class MqProducerService {

    @Resource
    private RocketMQTemplate rocketMQTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 照片分析 Topic
     */
    private static final String PHOTO_ANALYSIS_TOPIC = "photo-analysis";

    /**
     * 发送照片分析消息
     */
    public void sendPhotoAnalysisMessage(PhotoAnalysisMessage message) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(message);

            rocketMQTemplate.syncSend(
                    PHOTO_ANALYSIS_TOPIC,
                    MessageBuilder.withPayload(jsonMessage).build()
            );

            log.info("发送照片分析消息成功，photoId={}, petId={}", message.getPhotoId(), message.getPetId());
        } catch (JsonProcessingException e) {
            log.error("序列化照片分析消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        } catch (Exception e) {
            log.error("发送照片分析消息失败", e);
            throw new RuntimeException("发送消息失败", e);
        }
    }
}
