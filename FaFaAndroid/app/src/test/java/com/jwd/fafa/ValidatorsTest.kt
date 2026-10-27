package com.jwd.fafa

import com.jwd.fafa.core.common.Validators
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidatorsTest {

    @Test
    fun password_acceptsLettersAndDigits() {
        assertNull(Validators.passwordIssue("Abc12345"))
        assertNull(Validators.passwordIssue("a1b2c3d4"))
    }

    @Test
    fun password_rejectsTooShort() {
        assertEquals("密码至少 8 位", Validators.passwordIssue("Ab123"))
    }

    @Test
    fun password_rejectsTooLong() {
        assertEquals("密码最多 64 位", Validators.passwordIssue("A1".repeat(33)))
    }

    @Test
    fun password_rejectsMissingLetterOrDigit() {
        assertEquals("密码需同时包含字母和数字", Validators.passwordIssue("12345678"))
        assertEquals("密码需同时包含字母和数字", Validators.passwordIssue("abcdefgh"))
    }

    @Test
    fun password_emptyMeansMissing() {
        assertEquals("请输入密码", Validators.passwordIssue(""))
    }

    @Test
    fun email_basicValidation() {
        assertTrue(Validators.isValidEmail("13127103609@163.com"))
        assertFalse(Validators.isValidEmail("not-an-email"))
        assertFalse(Validators.isValidEmail("a@b"))
        assertFalse(Validators.isValidEmail(""))
    }

    @Test
    fun verificationCode_sixDigitsOnly() {
        assertTrue(Validators.isValidVerificationCode("640824"))
        assertFalse(Validators.isValidVerificationCode("64082"))
        assertFalse(Validators.isValidVerificationCode("64082a"))
        assertFalse(Validators.isValidVerificationCode(""))
    }

    @Test
    fun nickname_rangeCheck() {
        assertTrue(Validators.isValidNickname("咪咪"))
        assertFalse(Validators.isValidNickname(""))
        assertFalse(Validators.isValidNickname("x".repeat(21)))
    }
}
