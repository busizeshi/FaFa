package com.fafa.biz.domain.model;

import com.fafa.biz.domain.UserStatus;

import java.time.LocalDateTime;

/**
 * 宠主账号领域对象（identity 上下文）。
 *
 * <p>纯领域对象：不依赖 Spring / MyBatis-Plus / HTTP DTO；
 * 与数据库 DO 的映射由 infrastructure 层仓储适配器完成（架构规范 §8.1）。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public class User {

    /** 数据库主键；未持久化前为 null */
    private Long id;

    /** 登录邮箱（小写归一化，唯一） */
    private String email;

    /** 密码哈希（bcrypt，含盐），永不回传到接口层 */
    private String passwordHash;

    /** 显示昵称；注册阶段为空，由 M08 我的资料维护 */
    private String displayName;

    /** 账号状态 */
    private UserStatus status;

    /** 创建时间（UTC） */
    private LocalDateTime createdAt;

    /** 更新时间（UTC） */
    private LocalDateTime updatedAt;

    /**
     * @return 账号是否处于可登录状态
     */
    public boolean isLoginable() {
        return status == UserStatus.ACTIVE;
    }

    /**
     * 邮箱验证通过后激活账号（注册流程终点）。
     */
    public void activate() {
        this.status = UserStatus.ACTIVE;
    }

    /**
     * 找回密码成功后替换密码哈希。
     *
     * @param newPasswordHash 新的 bcrypt 哈希
     */
    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    // ==================== Getter / Setter ====================

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

    public UserStatus getStatus() {
        return status;
    }

    public void setStatus(UserStatus status) {
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
