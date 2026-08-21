package com.fafa.infrastructure.dispatch;

import com.fafa.infrastructure.mq.PhotoAnalysisMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * MQ 投递策略（优先）
 *
 * 未配置 rocketmq.name-server（Windows 开发环境）时 RocketMQTemplate bean 不存在，
 * supports() 返回 false，调度器自动降级到 HTTP 直推，业务无感知。
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class MqDispatchStrategy implements AiTaskDispatchStrategy {

    /** 与 fafa-python 消费者 TOPIC 常量保持一致 */
    public static final String TOPIC = "fafa_photo_analysis_topic";

    /** ObjectProvider：MQ 未配置时 bean 缺失不报错，启动不受影响 */
    private final ObjectProvider<RocketMQTemplate> templateProvider;

    @Override
    public boolean supports() {
        return templateProvider.getIfAvailable() != null;
    }

    @Override
    public boolean dispatch(PhotoAnalysisMessage message) {
        RocketMQTemplate template = templateProvider.getIfAvailable();
        if (template == null) {
            return false;
        }
        template.convertAndSend(TOPIC, message);
        log.info("AI 任务已投递 MQ: messageId={}, photoId={}", message.getMessageId(), message.getPhotoId());
        return true;
    }
}
