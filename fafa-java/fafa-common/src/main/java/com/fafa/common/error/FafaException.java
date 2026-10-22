package com.fafa.common.error;

/** 预期的应用错误，包含可安全返回给客户端的消息和稳定错误编码。 */
public class FafaException extends RuntimeException {
    /** 传递给 HTTP 异常处理器的稳定错误定义。 */
    private final ErrorCode errorCode;

    /** 使用错误编码的默认安全消息创建预期的应用错误。
     *
     * @param errorCode 稳定的错误定义
     */
    public FafaException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /** 使用面向调用方的安全消息创建预期的应用错误。
     *
     * @param errorCode 稳定的错误定义
     * @param safeMessage 可安全返回给客户端的消息
     */
    public FafaException(ErrorCode errorCode, String safeMessage) {
        super(safeMessage);
        this.errorCode = errorCode;
    }

    /** 使用安全消息和内部原因创建预期的应用错误。
     *
     * @param errorCode 稳定的错误定义
     * @param safeMessage 可安全返回给客户端的消息
     * @param cause 导致该异常的内部错误
     */
    public FafaException(ErrorCode errorCode, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.errorCode = errorCode;
    }

    /** 返回 HTTP 响应使用的稳定错误定义。
     *
     * @return 错误定义
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
