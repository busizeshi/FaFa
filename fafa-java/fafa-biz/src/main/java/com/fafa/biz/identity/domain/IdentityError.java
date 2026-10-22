package com.fafa.biz.identity.domain;

import com.fafa.common.error.ErrorCode;
import org.springframework.http.HttpStatus;

public enum IdentityError implements ErrorCode {
    INVALID_CREDENTIALS("FAFA-40101", "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    SESSION_REPLACED("FAFA-40102", "This account signed in on another device", HttpStatus.UNAUTHORIZED),
    SESSION_REVOKED("FAFA-40103", "Please sign in again", HttpStatus.UNAUTHORIZED),
    CHALLENGE_INVALID("FAFA-40001", "Verification code or challenge is invalid", HttpStatus.BAD_REQUEST),
    CHALLENGE_EXPIRED("FAFA-41001", "Verification challenge has expired", HttpStatus.GONE),
    EMAIL_REGISTERED("FAFA-40901", "This email is already registered; please sign in", HttpStatus.CONFLICT),
    LEGAL_VERSION_CHANGED("FAFA-40902", "Please review the current legal documents", HttpStatus.CONFLICT),
    PASSWORD_INVALID("FAFA-40002", "Passwords must match and contain 8-128 characters, letters and digits", HttpStatus.BAD_REQUEST),
    TICKET_INVALID("FAFA-41002", "The reset ticket is invalid or expired", HttpStatus.GONE),
    RATE_LIMITED("FAFA-42900", "Too many requests; please try again later", HttpStatus.TOO_MANY_REQUESTS),
    MAIL_UNAVAILABLE("FAFA-50301", "Verification email could not be sent; please retry", HttpStatus.SERVICE_UNAVAILABLE),
    ACCOUNT_DISABLED("FAFA-40301", "This account is unavailable", HttpStatus.FORBIDDEN);

    private final String code;
    private final String message;
    private final HttpStatus status;

    IdentityError(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
    public String code() { return code; }
    public String message() { return message; }
    public HttpStatus httpStatus() { return status; }
}
