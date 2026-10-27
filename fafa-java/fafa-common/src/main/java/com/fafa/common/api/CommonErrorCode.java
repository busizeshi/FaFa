package com.fafa.common.api;

/**
 * 全局通用错误码（跨服务复用的最小集合）。
 *
 * <p>业务专属错误码（如照护任务冲突、AI 草稿过期等）应定义在各自服务的 ErrorCode 枚举中，
 * 不要全部堆进这里，避免公共库承载业务语义。</p>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public enum CommonErrorCode implements ErrorCode {

    /** 成功。注意：成功码固定为 "0"，与失败码区分 */
    OK("0", "成功"),

    /** 请求参数格式错误（协议层校验失败，对应 HTTP 400） */
    BAD_REQUEST("400-00-00", "请求参数错误"),

    /** 未认证或会话已失效（对应 HTTP 401） */
    UNAUTHORIZED("401-00-00", "登录状态已失效，请重新登录"),

    /** 已认证但无权限访问目标资源（对应 HTTP 403） */
    FORBIDDEN("403-00-00", "没有访问该资源的权限"),

    /** 目标资源不存在或不属于当前用户（对应 HTTP 404，避免暴露资源存在性） */
    NOT_FOUND("404-00-00", "资源不存在"),

    /** 并发修改冲突：客户端携带的 revision 已过期（对应 HTTP 409） */
    CONFLICT("409-00-00", "数据已被他人修改，请刷新后重试"),

    /** 业务规则校验失败（对应 HTTP 422） */
    UNPROCESSABLE("422-00-00", "业务校验未通过"),

    /** 触发限流（对应 HTTP 429，网关或服务端限流） */
    TOO_MANY_REQUESTS("429-00-00", "请求过于频繁，请稍后再试"),

    /** 服务内部错误（对应 HTTP 500，对外不暴露细节） */
    INTERNAL_ERROR("500-00-00", "系统繁忙，请稍后再试"),

    /** 下游依赖（数据库/MQ/外部服务）暂不可用（对应 HTTP 503） */
    DEPENDENCY_UNAVAILABLE("503-00-00", "服务暂不可用，请稍后再试");

    private final String code;
    private final String message;

    /**
     * @param code    稳定错误码
     * @param message 对外默认提示文案
     */
    CommonErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    /** {@inheritDoc} */
    @Override
    public String getCode() {
        return code;
    }

    /** {@inheritDoc} */
    @Override
    public String getMessage() {
        return message;
    }
}
