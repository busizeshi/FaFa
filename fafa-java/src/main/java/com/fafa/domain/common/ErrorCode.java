package com.fafa.domain.common;

/**
 * 全局错误码
 *
 * 分段规则：10xxx 用户/认证，20xxx 宠物，30xxx 日常记录，
 * 40xxx 素材，50xxx 提醒，60xxx 健康管理（就医/用药），70xxx 记账
 */
public enum ErrorCode {

    /** 通用 */
    SYSTEM_ERROR(500, "系统异常，请稍后重试"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权访问"),

    /** 用户/认证 10xxx */
    USER_NOT_FOUND(10001, "用户不存在"),
    WECHAT_LOGIN_FAILED(10002, "微信登录失败"),
    USER_DELETED(10003, "账号已注销"),

    /** 宠物 20xxx */
    PET_NOT_FOUND(20001, "宠物不存在"),
    PET_LIMIT_EXCEEDED(20002, "宠物数量已达上限"),

    /** 素材 40xxx */
    PHOTO_NOT_FOUND(40001, "照片不存在"),
    PHOTO_UPLOAD_FAILED(40002, "照片上传失败"),
    AI_SERVICE_UNAVAILABLE(40003, "AI 服务暂时不可用");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
