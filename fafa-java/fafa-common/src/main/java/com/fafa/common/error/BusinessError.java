package com.fafa.common.error;

import org.springframework.http.HttpStatus;

/** 第二批业务使用的稳定错误线码；客户端按编码而非消息分支。 */
public enum BusinessError implements ErrorCode {
    VERSION_CONFLICT("FAFA-40910", "资料已更新，请重新读取后提交", HttpStatus.CONFLICT),
    IDEMPOTENCY_CONFLICT("FAFA-40911", "相同幂等键不能提交不同内容", HttpStatus.CONFLICT),
    PET_STATE_CONFLICT("FAFA-40912", "宠物当前状态不允许此操作", HttpStatus.CONFLICT),
    QUOTA_INSUFFICIENT("FAFA-40913", "可用存储额度不足", HttpStatus.CONFLICT),
    OPERATION_CONFLICT("FAFA-40914", "操作已完成或参数与原操作不一致", HttpStatus.CONFLICT),
    ADMIN_CREDENTIALS_INVALID("FAFA-40110", "管理员账号或密码错误", HttpStatus.UNAUTHORIZED),
    ADMIN_SESSION_INVALID("FAFA-40111", "管理员会话无效或已过期", HttpStatus.UNAUTHORIZED),
    ADMIN_LOGIN_LIMITED("FAFA-42910", "登录尝试过于频繁，请稍后重试", HttpStatus.TOO_MANY_REQUESTS),
    RPC_UNAUTHORIZED("FAFA-40310", "内部调用认证失败", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;
    BusinessError(String code, String message, HttpStatus status) {
        this.code = code; this.message = message; this.status = status;
    }
    public String code() { return code; }
    public String message() { return message; }
    public HttpStatus httpStatus() { return status; }
}
