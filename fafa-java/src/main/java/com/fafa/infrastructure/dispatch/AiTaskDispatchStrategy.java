package com.fafa.infrastructure.dispatch;

import com.fafa.infrastructure.mq.PhotoAnalysisMessage;

/**
 * AI 任务投递策略
 *
 * 环境差异（Windows 开发机无 RocketMQ / Linux 部署启用 MQ）通过策略链抹平：
 * 业务层只调用 AiTaskDispatcher，不感知底层走 MQ 还是 HTTP。
 */
public interface AiTaskDispatchStrategy {

    /** 该策略在当前环境是否可用 */
    boolean supports();

    /**
     * 投递任务
     *
     * @return true 投递成功；false 本策略不可用或投递未完成（调度器尝试下一策略）
     */
    boolean dispatch(PhotoAnalysisMessage message);
}
