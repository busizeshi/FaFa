package com.fafa.infrastructure.dispatch;

import cn.hutool.core.util.IdUtil;
import com.fafa.infrastructure.mq.PhotoAnalysisMessage;
import com.fafa.infrastructure.web.TraceIdFilter;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 任务调度器（策略工厂）
 *
 * 按注入顺序（@Order）尝试可用策略：MQ 优先、HTTP 兜底；
 * 某策略投递异常时自动降级到下一策略。业务层只依赖本类，不感知环境差异。
 */
@Slf4j
@Service
public class AiTaskDispatcher {

    /** Spring 注入 List 时按 @Order 排序：MqDispatchStrategy(1) -> HttpDispatchStrategy(2) */
    private final List<AiTaskDispatchStrategy> strategies;

    public AiTaskDispatcher(List<AiTaskDispatchStrategy> strategies) {
        this.strategies = strategies;
    }

    /**
     * 投递媒体分析任务
     *
     * @return true 表示任一策略投递成功
     */
    public boolean dispatch(PhotoAnalysisMessage message) {
        // 公共补充：幂等键 + 链路追踪
        if (message.getMessageId() == null) {
            message.setMessageId(IdUtil.fastSimpleUUID());
        }
        if (message.getTraceId() == null) {
            message.setTraceId(MDC.get(TraceIdFilter.TRACE_ID_MDC_KEY));
        }
        for (AiTaskDispatchStrategy strategy : strategies) {
            if (!strategy.supports()) {
                continue;
            }
            try {
                if (strategy.dispatch(message)) {
                    return true;
                }
            } catch (Exception e) {
                log.error("AI 任务投递失败，降级到下一策略: strategy={}, photoId={}",
                        strategy.getClass().getSimpleName(), message.getPhotoId(), e);
            }
        }
        log.error("AI 任务所有投递策略均失败: photoId={}", message.getPhotoId());
        return false;
    }
}
