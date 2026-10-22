package com.fafa.biz.identity.application;

import com.fafa.biz.identity.domain.IdentityError;
import com.fafa.common.error.FafaException;

public class AuthRateLimitException extends FafaException {
    private final long retryAfter;
    public AuthRateLimitException(long retryAfter) {
        super(IdentityError.RATE_LIMITED);
        this.retryAfter = Math.max(1, retryAfter);
    }
    public long retryAfter() { return retryAfter; }
}
