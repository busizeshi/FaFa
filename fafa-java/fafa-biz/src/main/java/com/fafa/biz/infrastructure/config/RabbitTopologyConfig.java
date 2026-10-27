package com.fafa.biz.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 拓扑声明（架构规范 §9.3：Exchange/Queue 与代码同步维护）。
 *
 * <p>M00 阶段仅声明事件 topic 交换机；各业务队列与其绑定由对应消费者模块
 * （媒体/OCR/通知等）在实施时声明，避免"幽灵队列"。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Configuration
public class RabbitTopologyConfig {

    /**
     * 事件交换机：durable topic；routing key = 事件类型（如 user.registered）。
     *
     * @return topic 交换机
     */
    @Bean
    public TopicExchange fafaEventsExchange() {
        return new TopicExchange(OutboxEventPublisherExchangeHolder.EVENT_EXCHANGE, true, false);
    }

    /**
     * 交换机名称常量持有者（避免配置类与发布器相互依赖）。
     */
    public static final class OutboxEventPublisherExchangeHolder {

        /** 与 {@code com.fafa.biz.infrastructure.mq.OutboxEventPublisher#EVENT_EXCHANGE} 保持一致 */
        public static final String EVENT_EXCHANGE = "fafa.events";

        /** 工具类禁止实例化 */
        private OutboxEventPublisherExchangeHolder() {
        }
    }
}
