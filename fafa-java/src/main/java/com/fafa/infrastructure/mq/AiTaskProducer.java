package com.fafa.infrastructure.mq;

import cn.hutool.core.util.IdUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.MDC;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * AI 异步任务生产者
 *
 * 重 AI 负载（照片理解、向量化、个体识别）通过 RocketMQ 投递给 fafa-python 消费，
 * 上传接口发完消息即返回，处理结果经回调异步补齐。
 *
 * Topic/Tag 命名规范见技术文档 6.3。
 *
 * @author FaFa Team
 * @since 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiTaskProducer {

    /** 照片分析 Topic */
    public static final String TOPIC_PHOTO_ANALYSIS = "fafa_photo_analysis_topic";
    private static final String TAG_PHOTO = "photo";

    private final RocketMQTemplate rocketMQTemplate;

    /**
     * 投递照片分析任务
     *
     * @return 消息 ID（幂等键），投递失败抛出异常由上层决定是否降级为同步调用
     */
    public String sendPhotoAnalysis(Long userId, Long photoId, String url,
                                    String mediaType, List<String> tags, Long petId) {
        String messageId = IdUtil.fastSimpleUUID();
        PhotoAnalysisMessage message = PhotoAnalysisMessage.builder()
                .messageId(messageId)
                .traceId(MDC.get("traceId"))
                .userId(userId)
                .photoId(photoId)
                .url(url)
                .mediaType(mediaType)
                .tags(tags)
                .petId(petId)
                .build();
        try {
            rocketMQTemplate.syncSend(
                    TOPIC_PHOTO_ANALYSIS + ":" + TAG_PHOTO,
                    MessageBuilder.withPayload(message).build(),
                    5000);
            log.info("照片分析任务已投递: photoId={}, messageId={}", photoId, messageId);
            return messageId;
        } catch (Exception ex) {
            log.error("照片分析任务投递失败: photoId={}", photoId, ex);
            throw ex;
        }
    }
}
