package com.fafa.biz.interfaces.auth.dto;

/**
 * 会话签发响应（注册验证成功 / 登录成功）。
 *
 * <p>tokenName 与 tokenValue 由 Sa-Token 签发；客户端将 tokenValue 存入系统安全区，
 * 后续请求以 header {@code fafa-token: <tokenValue>} 携带（架构规范 §4.1）。</p>
 *
 * @param tokenName 令牌请求头名称
 * @param tokenValue 令牌值
 * @param user      用户摘要
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record AuthSessionResponse(String tokenName, String tokenValue, UserSummary user) {

    /**
     * 用户摘要（白名单字段，禁止携带 passwordHash 等内部字段）。
     *
     * @param userId      用户 ID
     * @param email       邮箱
     * @param displayName 昵称（注册阶段可能为空，客户端显示邮箱前缀）
     */
    public record UserSummary(Long userId, String email, String displayName) {
    }
}
