package com.fafa.infrastructure.mq;

import lombok.Builder;

import java.util.List;

/**
 * 照片分析异步任务消息体（Java → Python，经 RocketMQ 投递）
 *
 * 字段 camelCase（与 Java 序列化一致），Python 消费端做 camelCase → snake_case 映射。
 *
 * @param messageId 幂等键
 * @param traceId   全链路追踪 ID
 * @author FaFa Team
 * @since 1.0
 */
@Builder
public record PhotoAnalysisMessage(
        String messageId,
        String traceId,
        Long userId,
        Long photoId,
        String url,
        String mediaType,
        List<String> tags,
        Long petId
) {
}
