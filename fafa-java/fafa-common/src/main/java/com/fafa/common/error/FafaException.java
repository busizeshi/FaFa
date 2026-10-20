package com.fafa.common.error;

/** Expected application failure with a client-safe message and stable error code. */
public class FafaException extends RuntimeException {
    /** Stable error definition carried to the HTTP exception mapper. */
    private final ErrorCode errorCode;

    /** Creates an expected application error using the code's default safe message.
     *
     * @param errorCode stable error definition
     */
    public FafaException(ErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode;
    }

    /** Creates an expected application error with a safe message for the caller.
     *
     * @param errorCode stable error definition
     * @param safeMessage client-safe message
     */
    public FafaException(ErrorCode errorCode, String safeMessage) {
        super(safeMessage);
        this.errorCode = errorCode;
    }

    /** Creates an expected application error with a safe message and internal cause.
     *
     * @param errorCode stable error definition
     * @param safeMessage client-safe message
     * @param cause internal failure that caused this exception
     */
    public FafaException(ErrorCode errorCode, String safeMessage, Throwable cause) {
        super(safeMessage, cause);
        this.errorCode = errorCode;
    }

    /** Returns the stable error definition used for the HTTP response.
     *
     * @return the error definition
     */
    public ErrorCode errorCode() {
        return errorCode;
    }
}
