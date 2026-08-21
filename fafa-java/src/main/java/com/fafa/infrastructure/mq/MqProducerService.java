package com.fafa.infrastructure.mq;

import cn.hutool.core.util.IdUtil;
import com.fafa.infrastructure.web.TraceIdFilter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * MQ 消息生产者
 *
 * Topic 命名：fafa_{业务}_topic；生产者组：fafa_java_producer。
 * Windows 开发环境未配置 RocketMQ 时模板 bean 不存在，发送静默跳过（走 HTTP 直推链路）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqProducerService {

    public static final String PHOTO_ANALYSIS_TOPIC = "fafa_photo_analysis_topic";

    /** ObjectProvider：未配置 name-server 时 RocketMQTemplate bean 不存在，避免启动失败 */
    private final ObjectProvider<RocketMQTemplate> rocketMQTemplateProvider;

    /**
     * 发送照片/视频分析消息
     *
     * @return true 表示已投递；false 表示 MQ 未启用（调用方需走 HTTP 兜底）
     */
    public boolean sendPhotoAnalysis(PhotoAnalysisMessage message) {
        RocketMQTemplate template = rocketMQTemplateProvider.getIfAvailable();
        if (template == null) {
            log.debug("RocketMQ 未启用，跳过消息投递: photoId={}", message.getPhotoId());
            return false;
        }
        if (message.getMessageId() == null) {
            message.setMessageId(IdUtil.fastSimpleUUID());
        }
        message.setTraceId(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
        try {
            template.convertAndSend(PHOTO_ANALYSIS_TOPIC, message);
            log.info("照片分析消息已投递: messageId={}, photoId={}", message.getMessageId(), message.getPhotoId());
            return true;
        } catch (Exception e) {
            log.error("照片分析消息投递失败: photoId={}", message.getPhotoId(), e);
            return false;
        }
    }
}
