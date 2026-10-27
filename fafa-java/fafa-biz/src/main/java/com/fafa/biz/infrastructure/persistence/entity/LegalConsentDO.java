package com.fafa.biz.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 协议同意记录 DO（表 legal_consent，追加式写入）。
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@TableName("legal_consent")
public class LegalConsentDO {

    /** 主键 */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户 ID */
    private Long userId;

    /** 文档类型：USER_AGREEMENT / PRIVACY_POLICY */
    private String documentType;

    /** 同意的版本号（客户端内置文本版本） */
    private String documentVersion;

    /** 同意时间（UTC） */
    private LocalDateTime consentedAt;

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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentVersion() {
        return documentVersion;
    }

    public void setDocumentVersion(String documentVersion) {
        this.documentVersion = documentVersion;
    }

    public LocalDateTime getConsentedAt() {
        return consentedAt;
    }

    public void setConsentedAt(LocalDateTime consentedAt) {
        this.consentedAt = consentedAt;
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
