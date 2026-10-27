package com.fafa.biz.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * Outbox 事件 DO（表 outbox_event，架构规范 §6.3）。
 * 仅由 infrastructure.mq 包内部的发布器与业务服务（同事务插入）使用。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@TableName("outbox_event")
public class OutboxEventDO {

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 全局唯一事件 ID（ULID） */
    private String eventId;

    /** 事件类型，如 user.registered */
    private String eventType;

    /** 消息 schema 版本 */
    private Integer schemaVersion;

    /** 聚合类型，如 user */
    private String aggregateType;

    /** 聚合 ID */
    private String aggregateId;

    /** 关联任务 ID（任务消息时必填） */
    private String jobId;

    /** 事件载荷（JSON 字符串，禁止敏感原文） */
    private String payload;

    /** 链路追踪 ID */
    private String traceId;

    /** 已尝试发布次数 */
    private Integer publishAttempts;

    /** 确认发布时间（收到 publisher confirm 后写入） */
    private LocalDateTime publishedAt;

    /** 创建时间（UTC，应用层填充） */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间（UTC，应用层填充） */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getSchemaVersion() {
        return schemaVersion;
    }

    public void setSchemaVersion(Integer schemaVersion) {
        this.schemaVersion = schemaVersion;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public void setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(String aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getPublishAttempts() {
        return publishAttempts;
    }

    public void setPublishAttempts(Integer publishAttempts) {
        this.publishAttempts = publishAttempts;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
