package com.pdfpocket.lite.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PasswordValidatorTest {
    @Test fun requiresLengthLetterAndDigit() {
        assertThat(PasswordValidator.isValid("secret12")).isTrue()
        assertThat(PasswordValidator.isValid("12345678")).isFalse()
        assertThat(PasswordValidator.isValid("abcdefgh")).isFalse()
        assertThat(PasswordValidator.isValid("a1" )).isFalse()
    }
    @Test fun confirmationMustMatch() {
        assertThat(PasswordValidator.matches("secret12", "secret12")).isTrue()
        assertThat(PasswordValidator.matches("secret12", "secret13")).isFalse()
    }
}
