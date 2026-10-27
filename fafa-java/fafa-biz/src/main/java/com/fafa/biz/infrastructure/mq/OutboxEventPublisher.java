package com.fafa.biz.infrastructure.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafa.biz.infrastructure.persistence.entity.OutboxEventDO;
import com.fafa.biz.infrastructure.persistence.mapper.OutboxEventMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

/**
 * Outbox 事件发布器（架构规范 §6.3）。
 *
 * <p>流程：</p>
 * <ol>
 *   <li>业务事务内调用 {@link #append} 与业务数据同库同事务写入事件；</li>
 *   <li>本发布器每秒扫描未发布事件，发送到 topic 交换机（路由键 = 事件类型）；</li>
 *   <li>等待 publisher confirm，确认后才标记 published_at；失败累加 attempts 留待下轮。</li>
 * </ol>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@Component
public class OutboxEventPublisher {

    /** 事件交换机（topic，durable；队列绑定由各消费者模块声明） */
    public static final String EVENT_EXCHANGE = "fafa.events";

    /** 单轮最大扫描条数：避免积压时单轮占用过久 */
    private static final int BATCH_SIZE = 50;

    /** confirm 等待超时（秒） */
    private static final long CONFIRM_TIMEOUT_SECONDS = 5;

    private static final Logger log = LoggerFactory.getLogger(OutboxEventPublisher.class);

    private final OutboxEventMapper outboxEventMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    /**
     * @param outboxEventMapper outbox Mapper
     * @param rabbitTemplate    已开启 publisher-confirm 的模板（见 application-dev.yml）
     * @param objectMapper      JSON 序列化
     */
    public OutboxEventPublisher(OutboxEventMapper outboxEventMapper,
                                RabbitTemplate rabbitTemplate,
                                ObjectMapper objectMapper) {
        this.outboxEventMapper = outboxEventMapper;
        this.rabbitTemplate = rabbitTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 业务事务内登记事件（与业务写操作同库同事务，杜绝"提交成功但消息丢失"）。
     *
     * @param event 事件记录（eventId / eventType / payload 由调用方准备）
     */
    public void append(OutboxEventDO event) {
        if (event.getPublishAttempts() == null) {
            event.setPublishAttempts(0);
        }
        outboxEventMapper.insert(event);
        log.info("[Outbox] 事件已登记 eventId={} eventType={}", event.getEventId(), event.getEventType());
    }

    /**
     * 定时扫描未发布事件并投递（单实例部署；多实例部署时应改为分片/抢占查询）。
     */
    @Scheduled(fixedDelay = 1000, initialDelay = 5000)
    public void publishPending() {
        LambdaQueryWrapper<OutboxEventDO> wrapper = new LambdaQueryWrapper<OutboxEventDO>()
                .isNull(OutboxEventDO::getPublishedAt)
                .orderByAsc(OutboxEventDO::getId)
                .last("LIMIT " + BATCH_SIZE);
        List<OutboxEventDO> pending = outboxEventMapper.selectList(wrapper);
        for (OutboxEventDO event : pending) {
            publishOne(event);
        }
    }

    /**
     * 发布单条事件并等待 broker 确认。
     *
     * @param event 未发布事件
     */
    private void publishOne(OutboxEventDO event) {
        try {
            CorrelationData correlation = new CorrelationData(event.getEventId());
            rabbitTemplate.convertAndSend(
                    EVENT_EXCHANGE,
                    event.getEventType(),
                    event.getPayload().getBytes(StandardCharsets.UTF_8),
                    message -> {
                        // 消息信封：持久化 + messageId 供消费者幂等去重（架构规范 §9.3）
                        message.getMessageProperties().setMessageId(event.getEventId());
                        message.getMessageProperties().setHeader("eventType", event.getEventType());
                        message.getMessageProperties().setHeader("schemaVersion", event.getSchemaVersion());
                        message.getMessageProperties().setHeader("traceId", event.getTraceId());
                        message.getMessageProperties().setHeader("jobId", event.getJobId());
                        return message;
                    },
                    correlation);

            // 同步等待 confirm：确认后才算投递成功（手册 §5.2 路由确认）
            CorrelationData.Confirm confirm = correlation.getFuture()
                    .get(CONFIRM_TIMEOUT_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
            if (confirm != null && confirm.isAck()) {
                OutboxEventDO patch = new OutboxEventDO();
                patch.setId(event.getId());
                patch.setPublishedAt(LocalDateTime.now(ZoneOffset.UTC));
                outboxEventMapper.updateById(patch);
                log.info("[Outbox] 事件发布成功 eventId={} routingKey={}", event.getEventId(), event.getEventType());
            } else {
                markRetry(event, "nack: " + (confirm == null ? "null" : confirm.getReason()));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markRetry(event, "interrupted");
        } catch (Exception e) {
            markRetry(event, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * 标记一次失败尝试（累加计数，留待下轮；持续失败由监控按 attempts 告警）。
     *
     * @param event 事件
     * @param reason 失败原因摘要
     */
    private void markRetry(OutboxEventDO event, String reason) {
        OutboxEventDO patch = new OutboxEventDO();
        patch.setId(event.getId());
        patch.setPublishAttempts((event.getPublishAttempts() == null ? 0 : event.getPublishAttempts()) + 1);
        outboxEventMapper.updateById(patch);
        log.warn("[Outbox] 事件发布失败 eventId={} attempts={} reason={}",
                event.getEventId(), patch.getPublishAttempts(), reason);
    }
}
