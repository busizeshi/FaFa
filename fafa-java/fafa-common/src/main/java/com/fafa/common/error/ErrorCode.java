package com.fafa.common.error;

import org.springframework.http.HttpStatus;

/** 稳定的机器错误编码、可安全返回的客户端消息及对应 HTTP 状态码。 */
public interface ErrorCode {
    /** 返回对客户端稳定可见的错误编码。
     * @return 错误编码
     */
    String code();

    /** 返回可安全返回给客户端的消息。
     * @return 安全消息
     */
    String message();

    /** 返回该错误对应的 HTTP 状态码。
     * @return HTTP 状态码
     */
    HttpStatus httpStatus();
}
