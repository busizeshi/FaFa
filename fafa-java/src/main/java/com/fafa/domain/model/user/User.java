package com.fafa.domain.model.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户聚合根
 * 
 * @author FaFa Team
 * @since 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    /**
     * 用户 ID
     */
    private UserId userId;
    
    /**
     * 微信 OpenID
     */
    private String openid;
    
    /**
     * 微信 UnionID
     */
    private String unionid;
    
    /**
     * 昵称
     */
    private String nickname;
    
    /**
     * 头像
     */
    private String avatar;
    
    /**
     * 手机号
     */
    private String phone;
    
    /**
     * 性别：0-未知, 1-男, 2-女
     */
    private Integer gender;
    
    /**
     * 状态
     */
    private UserStatus status;
    
    /**
     * 注册日期
     */
    private LocalDate registerDate;
    
    /**
     * 最后登录时间
     */
    private LocalDateTime lastLoginAt;
    
    /**
     * 注销请求时间
     */
    private LocalDateTime deletionRequestedAt;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;
    
    /**
     * 是否正常状态
     */
    public boolean isNormal() {
        return UserStatus.NORMAL.equals(this.status);
    }
    
    /**
     * 是否待注销状态
     */
    public boolean isPendingDeletion() {
        return UserStatus.PENDING_DELETION.equals(this.status);
    }
    
    /**
     * 请求注销账号（进入冷静期）
     */
    public void requestDeletion() {
        this.status = UserStatus.PENDING_DELETION;
        this.deletionRequestedAt = LocalDateTime.now();
    }
    
    /**
     * 取消注销（恢复账号）
     */
    public void cancelDeletion() {
        this.status = UserStatus.NORMAL;
        this.deletionRequestedAt = null;
    }
    
    /**
     * 更新最后登录时间
     */
    public void updateLastLogin() {
        this.lastLoginAt = LocalDateTime.now();
    }
}
