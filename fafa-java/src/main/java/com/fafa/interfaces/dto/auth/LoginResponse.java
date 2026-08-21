package com.fafa.interfaces.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 登录响应
 */
@Data
@AllArgsConstructor
public class LoginResponse {

    /** Sa-Token 令牌，后续请求放入 Authorization: Bearer {token} */
    private String token;

    private Long userId;

    /** 是否新注册用户（前端据此引导创建宠物档案） */
    private Boolean isNewUser;
}
