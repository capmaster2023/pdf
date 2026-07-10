package com.pdfpocket.lite.core

object PasswordValidator {
    fun isValid(value: String): Boolean = value.length >= 8 && value.any(Char::isLetter) && value.any(Char::isDigit)
    fun matches(value: String, confirmation: String): Boolean = value == confirmation && isValid(value)
}
