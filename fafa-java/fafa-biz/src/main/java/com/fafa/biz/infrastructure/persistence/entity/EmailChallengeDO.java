package com.fafa.biz.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 邮箱验证挑战 DO（表 email_challenge）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@TableName("email_challenge")
public class EmailChallengeDO {

    /** 主键，对外作为 challengeId */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用途：REGISTER_VERIFY / PASSWORD_RESET */
    private String purpose;

    /** 目标邮箱（小写） */
    private String email;

    /** 验证码哈希（bcrypt，明文不落库） */
    private String codeHash;

    /** 过期时间（UTC） */
    private LocalDateTime expiresAt;

    /** 已尝试次数 */
    private Integer attemptCount;

    /** 最大尝试次数 */
    private Integer maxAttempts;

    /** 消费时间（UTC，非空即已使用） */
    private LocalDateTime consumedAt;

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

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCodeHash() {
        return codeHash;
    }

    public void setCodeHash(String codeHash) {
        this.codeHash = codeHash;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(Integer maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getConsumedAt() {
        return consumedAt;
    }

    public void setConsumedAt(LocalDateTime consumedAt) {
        this.consumedAt = consumedAt;
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
