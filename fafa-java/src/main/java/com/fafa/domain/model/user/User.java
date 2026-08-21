package com.fafa.domain.model.user;

import lombok.Getter;
import lombok.Setter;

/**
 * 用户聚合根
 *
 * 微信登录即注册，初始资料为空，由用户逐步补全。
 * 资料变更走 updateProfile 领域行为，setter 仅供持久化转换填充使用
 */
@Getter
@Setter
public class User {

    private Long id;
    private String openid;
    private String nickname;
    private String avatarUrl;
    private String phone;
    /** 0未知 1男 2女 */
    private Integer gender;
    private String city;
    /** 0普通用户 1会员 */
    private Integer memberLevel;

    /**
     * 微信首次登录创建新用户
     */
    public static User register(String openid) {
        User user = new User();
        user.openid = openid;
        user.memberLevel = 0;
        return user;
    }

    /**
     * 补全/更新用户资料（仅允许覆盖非空字段）
     */
    public void updateProfile(String nickname, String avatarUrl, String phone, Integer gender, String city) {
        if (nickname != null && !nickname.isBlank()) {
            this.nickname = nickname;
        }
        if (avatarUrl != null && !avatarUrl.isBlank()) {
            this.avatarUrl = avatarUrl;
        }
        if (phone != null && !phone.isBlank()) {
            this.phone = phone;
        }
        if (gender != null) {
            this.gender = gender;
        }
        if (city != null && !city.isBlank()) {
            this.city = city;
        }
    }

    public boolean isMember() {
        return memberLevel != null && memberLevel > 0;
    }
}
