package com.fafa.common;

import lombok.Getter;

/**
 * 业务错误码
 *
 * 分段规则：0xxx 通用 / 10xxx 用户 / 20xxx 宠物 / 30xxx 记录 / 40xxx 照片 / 50xxx 提醒
 * / 60xxx 就医 / 70xxx 记账 / 90xxx 基础设施
 *
 * @author FaFa Team
 * @since 1.0
 */
@Getter
public enum ErrorCode {

    SUCCESS(0, "成功"),

    // 通用 0xxx
    PARAM_INVALID(1001, "参数校验失败"),
    UNAUTHORIZED(1002, "未登录或登录已过期"),
    FORBIDDEN(1003, "无权限访问"),
    NOT_FOUND(1004, "资源不存在"),
    SYSTEM_ERROR(9999, "系统异常，请稍后重试"),

    // 用户模块 10xxx
    USER_LOGIN_FAILED(10001, "登录失败，请稍后重试"),
    WECHAT_CODE_INVALID(10002, "微信登录凭证无效"),
    USER_NOT_FOUND(10003, "用户不存在"),

    // 基础设施 90xxx
    AI_SERVICE_UNAVAILABLE(90001, "AI 服务暂时不可用，请稍后重试"),
    STORAGE_ERROR(90002, "文件存储服务异常");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
