package com.fafa.biz.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 异步任务 DO（表 operation_job，M02 起接入业务使用）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@TableName("operation_job")
public class OperationJobDO {

    /** 自增主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 对外任务 ID（ULID） */
    private String jobId;

    /** 任务类型 */
    private String jobType;

    /** 状态：QUEUED / RUNNING / SUCCEEDED / FAILED / CANCELLED */
    private String status;

    /** 进度 0-100 */
    private Integer progress;

    /** 稳定错误码 */
    private String errorCode;

    /** 失败是否可重试 */
    private Boolean retryable;

    /** 所属用户（查询隔离） */
    private Long ownerUserId;

    /** 开始时间（UTC） */
    private LocalDateTime startedAt;

    /** 结束时间（UTC） */
    private LocalDateTime finishedAt;

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

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getJobType() {
        return jobType;
    }

    public void setJobType(String jobType) {
        this.jobType = jobType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getProgress() {
        return progress;
    }

    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Boolean getRetryable() {
        return retryable;
    }

    public void setRetryable(Boolean retryable) {
        this.retryable = retryable;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
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
