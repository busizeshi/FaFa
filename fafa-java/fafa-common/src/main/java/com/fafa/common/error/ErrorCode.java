package com.fafa.common.error;

import org.springframework.http.HttpStatus;

/** A stable machine code, safe client message and corresponding HTTP status. */
public interface ErrorCode {
    /** Returns the stable client-visible code.
     * @return error code
     */
    String code();

    /** Returns the safe client-visible message.
     * @return safe message
     */
    String message();

    /** Returns the HTTP status corresponding to this error.
     * @return HTTP status
     */
    HttpStatus httpStatus();
}
