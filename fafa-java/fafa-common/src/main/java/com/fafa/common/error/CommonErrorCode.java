package com.fafa.common.error;

import org.springframework.http.HttpStatus;

/** Java 服务之间含义一致的通用错误编码。 */
public enum CommonErrorCode implements ErrorCode {
    /** 请求数据无法解析或校验未通过。 */
    INVALID_ARGUMENT("FAFA-40000", "Request parameters are invalid", HttpStatus.BAD_REQUEST),
    /** 调用方尚未完成身份认证。 */
    UNAUTHENTICATED("FAFA-40100", "Authentication is required", HttpStatus.UNAUTHORIZED),
    /** 调用方已认证，但缺少执行该操作的权限。 */
    FORBIDDEN("FAFA-40300", "You do not have permission to perform this action", HttpStatus.FORBIDDEN),
    /** 请求的资源不存在。 */
    NOT_FOUND("FAFA-40400", "The requested resource was not found", HttpStatus.NOT_FOUND),
    /** 该资源不支持当前 HTTP 方法。 */
    METHOD_NOT_ALLOWED("FAFA-40500", "The HTTP method is not supported for this resource", HttpStatus.METHOD_NOT_ALLOWED),
    /** 当前操作与资源现有状态冲突。 */
    CONFLICT("FAFA-40900", "The request conflicts with the current resource state", HttpStatus.CONFLICT),
    REQUEST_TOO_LARGE("FAFA-41300", "The request body is too large", HttpStatus.PAYLOAD_TOO_LARGE),
    /** 不支持请求的媒体类型。 */
    UNSUPPORTED_MEDIA_TYPE("FAFA-41500", "The request media type is not supported", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    /** 必需的下游服务暂时不可用。 */
    DEPENDENCY_UNAVAILABLE("FAFA-50300", "A required service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    /** 发生了未预期的内部错误。 */
    INTERNAL_ERROR("FAFA-50000", "An unexpected server error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    /** 创建带传输层状态码的错误定义。
     *
     * @param code 对客户端稳定可见的错误编码
     * @param message 可安全返回给客户端的消息
     * @param httpStatus 对应的 HTTP 状态码
     */
    CommonErrorCode(String code, String message, HttpStatus httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    @Override
    public String code() {
        return code;
    }

    @Override
    public String message() {
        return message;
    }

    @Override
    public HttpStatus httpStatus() {
        return httpStatus;
    }
}
