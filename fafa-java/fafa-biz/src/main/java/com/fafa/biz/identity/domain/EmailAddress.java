package com.fafa.biz.identity.domain;

import com.fafa.common.error.CommonErrorCode;
import com.fafa.common.error.FafaException;
import java.util.Locale;

public record EmailAddress(String display, String canonical) {
    public static EmailAddress parse(String value) {
        if (value == null) throw new FafaException(CommonErrorCode.INVALID_ARGUMENT);
        String display = value.strip();
        if (display.length() > 254 || !display.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) {
            throw new FafaException(CommonErrorCode.INVALID_ARGUMENT, "A valid email address is required");
        }
        return new EmailAddress(display, display.toLowerCase(Locale.ROOT));
    }
}
