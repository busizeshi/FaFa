package com.fafa.biz.interfaces.auth.dto;

/**
 * 找回密码 verify 成功响应：一次性重置凭据（短时效，存 Redis）。
 *
 * @param resetCredential 一次性凭据（绑定用户，10 分钟内有效，仅可使用一次）
 * @author FaFa 项目组
 * @version 1.0.0
 */
public record ResetCredentialResponse(String resetCredential) {
}
