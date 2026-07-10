package com.pdfpocket.lite.security

import android.content.Context
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PinManager @Inject constructor(
    @ApplicationContext context: Context,
    private val crypto: KeystoreCrypto
) {
    private val prefs = context.getSharedPreferences("secure_lock", Context.MODE_PRIVATE)

    fun hasPin(): Boolean = prefs.contains("pin_blob")

    fun setPin(pin: String) {
        require(pin.length in 4..12 && pin.all(Char::isDigit))
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val payload = salt + derive(pin, salt)
        prefs.edit().putString("pin_blob", Base64.encodeToString(crypto.encrypt(payload), Base64.NO_WRAP)).apply()
    }

    fun verify(pin: String): Boolean = runCatching {
        val encoded = prefs.getString("pin_blob", null) ?: return false
        val payload = crypto.decrypt(Base64.decode(encoded, Base64.NO_WRAP))
        require(payload.size > 16)
        val salt = payload.copyOfRange(0, 16)
        val expected = payload.copyOfRange(16, payload.size)
        MessageDigest.isEqual(derive(pin, salt), expected)
    }.getOrDefault(false)

    fun clear() { prefs.edit().remove("pin_blob").apply() }

    private fun derive(pin: String, salt: ByteArray): ByteArray =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(pin.toCharArray(), salt, 120_000, 256)).encoded
}
