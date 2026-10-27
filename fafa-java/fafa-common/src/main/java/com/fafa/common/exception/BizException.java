package com.fafa.common.exception;

import com.fafa.common.api.ErrorCode;

/**
 * 业务异常基类（跨服务统一）。
 *
 * <p>使用约定（架构规范 §8.2）：</p>
 * <ul>
 *   <li>仅用于表达<strong>可预期的业务失败</strong>（校验不通过、资源冲突、状态不合法等）；
 *       意料之中的"空结果"不应抛异常；</li>
 *   <li>禁止吞掉异常；系统级异常（IO、网络）不应包装成本类掩盖根因；</li>
 *   <li>由各服务 {@code interfaces} 层的全局异常处理器统一转换为稳定错误响应。</li>
 * </ul>
 *
 * @author FaFa 项目组
 * @version 1.0.0
 */
public class BizException extends RuntimeException {

    /** 业务错误码（对外稳定契约） */
    private final ErrorCode errorCode;

    /**
     * @param errorCode 业务错误码
     */
    public BizException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    /**
     * @param errorCode 业务错误码
     * @param message   覆盖默认的提示文案
     */
    public BizException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * @param errorCode 业务错误码
     * @param message   覆盖默认的提示文案
     * @param cause     根因（保留完整异常链，便于日志排查）
     */
    public BizException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * @return 业务错误码，供异常处理器映射稳定响应结构
     */
    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
