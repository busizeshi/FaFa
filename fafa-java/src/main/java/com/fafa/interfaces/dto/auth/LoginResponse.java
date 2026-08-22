package com.fafa.interfaces.dto.auth;

import lombok.Data;

/**
 * 登录响应
 *
 * @author FaFa Team
 * @since 1.0
 */
@Data
public class LoginResponse {

    /** 会话令牌，后续请求放入 Authorization 头 */
    private String token;

    /** 用户 ID */
    private Long userId;

    /** 是否新注册用户（前端可借此引导填写资料） */
    private boolean newUser;
}
