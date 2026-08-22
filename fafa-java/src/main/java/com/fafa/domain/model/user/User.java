package com.fafa.domain.model.user;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 用户聚合根
 *
 * 微信一键登录场景下，用户由 openid 唯一标识，首次登录自动注册，
 * 不强制填写资料（昵称、头像等后续由用户自行补充）。
 *
 * 业务规则：
 * 1. 用户通过微信 openId 唯一标识
 * 2. 首次登录自动注册，初始为普通用户
 * 3. 用户资料（昵称、手机、性别等）可后续补充
 * 4. 会员到期后自动降级为普通用户
 *
 * @author FaFa Team
 * @since 1.0
 */
@Getter
@Slf4j
public class User {

    private Long id;
    private String openId;
    
    // 基本信息
    private String nickname;
    private String phone;
    private String gender;
    private Integer age;
    private String avatarUrl;
    private String city;
    
    // 会员信息
    private boolean vip;
    private LocalDateTime vipExpireTime;
    
    // 审计字段
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User(Long id, String openId, String nickname, String phone, String gender, 
                 Integer age, String avatarUrl, String city, boolean vip, 
                 LocalDateTime vipExpireTime, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.openId = openId;
        this.nickname = nickname;
        this.phone = phone;
        this.gender = gender;
        this.age = age;
        this.avatarUrl = avatarUrl;
        this.city = city;
        this.vip = vip;
        this.vipExpireTime = vipExpireTime;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /**
     * 注册新用户：微信首次登录时以 openid 创建
     * 
     * @param openId 微信 openId
     * @return 新注册的用户
     */
    public static User register(String openId) {
        Objects.requireNonNull(openId, "openId 不能为空");
        
        log.info("注册新用户: openId={}", openId);
        
        LocalDateTime now = LocalDateTime.now();
        return new User(
            null, openId, null, null, null, 
            null, null, null, false, 
            null, now, now
        );
    }

    /**
     * 重建已有用户（仓储加载时使用）
     */
    public static User restore(Long id, String openId, String nickname, String phone, 
                               String gender, Integer age, String avatarUrl, String city,
                               boolean vip, LocalDateTime vipExpireTime,
                               LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(
            id, openId, nickname, phone, gender, 
            age, avatarUrl, city, vip, 
            vipExpireTime, createdAt, updatedAt
        );
    }

    /**
     * 更新用户基本资料
     * 
     * 业务规则：
     * - 昵称、手机号可多次修改
     * - 性别、年龄可选填
     */
    public void updateProfile(String nickname, String phone, String gender, 
                             Integer age, String city) {
        log.info("更新用户资料: userId={}, nickname={}", this.id, nickname);
        
        this.nickname = nickname;
        this.phone = phone;
        this.gender = gender;
        this.age = age;
        this.city = city;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 更新用户头像
     */
    public void updateAvatar(String avatarUrl) {
        Objects.requireNonNull(avatarUrl, "头像URL不能为空");
        
        log.info("更新用户头像: userId={}", this.id);
        
        this.avatarUrl = avatarUrl;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 升级为会员
     * 
     * @param expireTime 会员到期时间
     */
    public void upgradeToVip(LocalDateTime expireTime) {
        Objects.requireNonNull(expireTime, "会员到期时间不能为空");
        
        if (expireTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("会员到期时间不能早于当前时间");
        }
        
        log.info("用户升级为会员: userId={}, expireTime={}", this.id, expireTime);
        
        this.vip = true;
        this.vipExpireTime = expireTime;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查会员是否有效
     * 
     * 业务规则：
     * - vip = true 且 vipExpireTime 晚于当前时间，则为有效会员
     * - 会员过期后需要手动调用 expireVip() 更新状态
     */
    public boolean isVipValid() {
        if (!vip) {
            return false;
        }
        
        if (vipExpireTime == null) {
            return false;
        }
        
        return vipExpireTime.isAfter(LocalDateTime.now());
    }

    /**
     * 会员到期处理
     * 
     * 注意：此方法应由定时任务或登录时检查调用
     */
    public void expireVip() {
        if (vip && vipExpireTime != null && vipExpireTime.isBefore(LocalDateTime.now())) {
            log.info("用户会员到期: userId={}, expireTime={}", this.id, this.vipExpireTime);
            
            this.vip = false;
            this.updatedAt = LocalDateTime.now();
        }
    }

    /**
     * 续费会员
     * 
     * @param additionalDays 续费天数
     */
    public void renewVip(int additionalDays) {
        if (additionalDays <= 0) {
            throw new IllegalArgumentException("续费天数必须大于0");
        }
        
        LocalDateTime newExpireTime;
        if (isVipValid()) {
            // 如果当前会员有效，在原到期时间基础上续费
            newExpireTime = vipExpireTime.plusDays(additionalDays);
        } else {
            // 如果已过期，从当前时间开始计算
            newExpireTime = LocalDateTime.now().plusDays(additionalDays);
        }
        
        log.info("用户续费会员: userId={}, additionalDays={}, newExpireTime={}", 
                this.id, additionalDays, newExpireTime);
        
        this.vip = true;
        this.vipExpireTime = newExpireTime;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 仓储保存后回填 ID
     */
    public void assignId(Long id) {
        this.id = id;
    }

    /**
     * 获取用户显示名称
     * 
     * 优先级：nickname > phone > "用户{id}"
     */
    public String getDisplayName() {
        if (nickname != null && !nickname.isBlank()) {
            return nickname;
        }
        if (phone != null && !phone.isBlank()) {
            return phone;
        }
        return "用户" + (id != null ? id : "");
    }
}
