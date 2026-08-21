package com.fafa.interfaces.dto.auth;

import lombok.Data;

/**
 * 用户信息响应
 */
@Data
public class UserInfoResponse {

    private Long userId;

    private String nickname;

    private String avatarUrl;

    private String phone;

    /** 0未知 1男 2女 */
    private Integer gender;

    private String city;

    /** 0普通用户 1会员 */
    private Integer memberLevel;
}
