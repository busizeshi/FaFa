package com.fafa.application.dto;

/**
 * 登录结果
 *
 * @param token Sa-Token 会话令牌，小程序后续请求放入 Authorization 头
 * @param userId 用户 ID
 * @param newUser 是否本次登录新注册的用户
 * @author FaFa Team
 * @since 1.0
 */
public record LoginResult(String token, Long userId, boolean newUser) {
}
