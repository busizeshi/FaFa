package com.fafa.common.error;

import org.springframework.http.HttpStatus;

/** Error codes that have the same meaning across Java services. */
public enum CommonErrorCode implements ErrorCode {
    /** Request data could not be parsed or validated. */
    INVALID_ARGUMENT("FAFA-40000", "Request parameters are invalid", HttpStatus.BAD_REQUEST),
    /** The caller has not authenticated. */
    UNAUTHENTICATED("FAFA-40100", "Authentication is required", HttpStatus.UNAUTHORIZED),
    /** The caller is authenticated but lacks permission. */
    FORBIDDEN("FAFA-40300", "You do not have permission to perform this action", HttpStatus.FORBIDDEN),
    /** The requested resource does not exist. */
    NOT_FOUND("FAFA-40400", "The requested resource was not found", HttpStatus.NOT_FOUND),
    /** The HTTP method is not supported for the resource. */
    METHOD_NOT_ALLOWED("FAFA-40500", "The HTTP method is not supported for this resource", HttpStatus.METHOD_NOT_ALLOWED),
    /** The operation conflicts with the resource's current state. */
    CONFLICT("FAFA-40900", "The request conflicts with the current resource state", HttpStatus.CONFLICT),
    /** The request media type is unsupported. */
    UNSUPPORTED_MEDIA_TYPE("FAFA-41500", "The request media type is not supported", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    /** A required downstream service is unavailable. */
    DEPENDENCY_UNAVAILABLE("FAFA-50300", "A required service is temporarily unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    /** An unexpected internal failure occurred. */
    INTERNAL_ERROR("FAFA-50000", "An unexpected server error occurred", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus httpStatus;

    /** Creates an error definition with its transport status.
     *
     * @param code stable client-visible code
     * @param message safe client-visible message
     * @param httpStatus matching HTTP status
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
