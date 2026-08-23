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

    // 记录模块 30xxx
    RECORD_NOT_FOUND(30001, "记录不存在"),
    RECORD_DATE_IN_FUTURE(30002, "记录日期不能晚于今天"),
    RECORD_NO_PREVIOUS(30003, "没有可复用的历史记录"),
    DAILY_DIMENSION_MISSING(30004, "必填记录维度缺失"),
    FEEDING_TIME_IN_FUTURE(30005, "喂食时间不能晚于当前时间"),

    // 素材与成长档案 40xxx
    MEDIA_NOT_FOUND(40001, "素材不存在"),
    MEDIA_FILE_TOO_LARGE(40002, "单个文件超过大小限制"),
    MEDIA_FILE_COUNT_EXCEEDED(40003, "单次上传文件数量超过限制"),
    MEDIA_TYPE_INVALID(40004, "不支持的文件类型"),
    STORAGE_QUOTA_EXCEEDED(40005, "存储空间不足，升级会员解锁更大空间"),
    AI_SEARCH_QUOTA_EXCEEDED(40006, "本月 AI 语义检索次数已用完，升级会员解锁不限次"),
    TAG_NAME_INVALID(40007, "标签名不能为空且不超过50个字符"),
    TAG_DUPLICATE(40008, "标签已存在"),
    TAG_NOT_FOUND(40009, "标签不存在"),
    EVENT_RECORD_NOT_FOUND(40010, "事件记录不存在"),
    EVENT_RECORD_NOT_MODIFIABLE(40011, "提醒生成的事件不可编辑或删除"),

    // 提醒模块 50xxx
    REMINDER_NOT_FOUND(50001, "提醒不存在"),
    REMINDER_TIME_IN_PAST(50002, "提醒时间不能早于当前时间"),
    REMINDER_INTERVAL_INVALID(50003, "周期配置无效"),
    REMINDER_STATE_INVALID(50004, "提醒当前状态不支持该操作"),
    REMINDER_COMPLETION_TIME_INVALID(50005, "完成时间不能晚于当前时间"),

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
