package com.fafa.biz.identity.domain;

import com.fafa.common.error.FafaException;

public final class PasswordPolicy {
    private static final int MIN_CHARACTERS = 8;
    private static final int MAX_CHARACTERS = 128;
    private PasswordPolicy() { }

    public static void validate(String password, String confirmation) {
        if (password == null || !password.equals(confirmation)
                || password.codePointCount(0, password.length()) < MIN_CHARACTERS
                || password.codePointCount(0, password.length()) > MAX_CHARACTERS
                || password.codePoints().noneMatch(Character::isLetter)
                || password.codePoints().noneMatch(Character::isDigit)) {
            throw new FafaException(IdentityError.PASSWORD_INVALID);
        }
    }
}
