package com.fafa.biz.interfaces.auth.dto;

import java.time.LocalDateTime;

/**
 * 会话视图（GET /me/sessions 列表项）。
 *
 * @param sessionId     会话 ID（token 摘要，非明文 token，可安全回传）
 * @param device        设备描述（登录时 User-Agent 摘要）
 * @param lastActiveAt  最近活跃时间（UTC）
 * @param current       是否为当前会话
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record SessionView(String sessionId, String device, LocalDateTime lastActiveAt, boolean current) {
}
