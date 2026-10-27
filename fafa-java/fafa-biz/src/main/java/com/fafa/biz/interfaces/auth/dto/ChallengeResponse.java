package com.fafa.biz.interfaces.auth.dto;

import java.time.LocalDateTime;

/**
 * 验证挑战受理响应（注册 / 找回密码第一步）。
 *
 * <p>email 返回脱敏格式（a***@163.com）便于用户确认收件箱；
 * expiresAt 为服务端权威过期时间，客户端倒计时仅作展示。</p>
 *
 * @param challengeId 挑战 ID（verify 接口路径参数）
 * @param email       脱敏邮箱
 * @param expiresAt   过期时间（UTC，ISO 8601）
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record ChallengeResponse(Long challengeId, String email, LocalDateTime expiresAt) {

    /**
     * 邮箱脱敏：保留首字符与域名，如 a***@163.com。
     *
     * @param email 原始邮箱
     * @return 脱敏结果
     */
    public static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }
}
