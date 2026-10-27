package com.fafa.biz.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/**
 * 宠主账号 DO（表 user_account）。
 *
 * <p>DO 仅在本包及仓储适配器内使用，与领域对象 {@code com.fafa.biz.domain.model.User}
 * 显式映射，禁止跨层泄露（架构规范 §8.2）。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
@TableName("user_account")
public class UserDO {

    /** 主键（自增） */
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 登录邮箱（小写，唯一索引兜底并发重复注册） */
    private String email;

    /** 密码哈希（bcrypt） */
    private String passwordHash;

    /** 显示昵称（可空） */
    private String displayName;

    /** 账号状态：PENDING_VERIFICATION / ACTIVE / SUSPENDED */
    private String status;

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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
