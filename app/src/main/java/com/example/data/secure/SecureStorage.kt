package com.example.data.secure

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android KeyStore-backed secure storage.
 * Ensures API keys and sensitive voice configurations are securely encrypted
 * using AES-GCM and never stored as plain text.
 */
class SecureStorage(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "maiki_secure_voice_prefs"
        private const val KEY_ALIAS = "maiki_voice_encryption_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private var fallbackKey: SecretKey? = null
    }

    private var isKeyStoreAvailable = false

    init {
        ensureKeyExists()
    }

    private fun ensureKeyExists() {
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (!keyStore.containsAlias(KEY_ALIAS)) {
                val keyGenerator = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEYSTORE
                )
                val keyGenSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()

                keyGenerator.init(keyGenSpec)
                keyGenerator.generateKey()
            }
            isKeyStoreAvailable = true
        } catch (_: Exception) {
            // Environment fallback (e.g. host JVM testing without AndroidKeyStore SPI)
            isKeyStoreAvailable = false
            if (fallbackKey == null) {
                val keyGen = KeyGenerator.getInstance("AES")
                keyGen.init(256)
                fallbackKey = keyGen.generateKey()
            }
        }
    }

    private fun getSecretKey(): SecretKey {
        if (isKeyStoreAvailable) {
            try {
                val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
                return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
            } catch (_: Exception) {
                // fallback if keystore entry retrieval fails
            }
        }
        if (fallbackKey == null) {
            val keyGen = KeyGenerator.getInstance("AES")
            keyGen.init(256)
            fallbackKey = keyGen.generateKey()
        }
        return fallbackKey!!
    }

    fun putSecureString(key: String, value: String?) {
        if (value.isNullOrEmpty()) {
            prefs.edit().remove(key).apply()
            return
        }

        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(value.toByteArray(Charsets.UTF_8))

            val byteBuffer = ByteBuffer.allocate(1 + iv.size + cipherText.size)
            byteBuffer.put(iv.size.toByte())
            byteBuffer.put(iv)
            byteBuffer.put(cipherText)

            val encoded = Base64.encodeToString(byteBuffer.array(), Base64.NO_WRAP)
            prefs.edit().putString(key, encoded).apply()
        } catch (_: Exception) {
            // Securely handle failure without logging secrets
        }
    }

    fun getSecureString(key: String, defaultValue: String = ""): String {
        val encoded = prefs.getString(key, null) ?: return defaultValue
        return try {
            val byteBuffer = ByteBuffer.wrap(Base64.decode(encoded, Base64.NO_WRAP))
            val ivLength = byteBuffer.get().toInt()
            val iv = ByteArray(ivLength)
            byteBuffer.get(iv)

            val cipherText = ByteArray(byteBuffer.remaining())
            byteBuffer.get(cipherText)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)

            val plainText = cipher.doFinal(cipherText)
            String(plainText, Charsets.UTF_8)
        } catch (_: Exception) {
            defaultValue
        }
    }

    fun putBoolean(key: String, value: Boolean) {
        prefs.edit().putBoolean(key, value).apply()
    }

    fun getBoolean(key: String, defaultValue: Boolean): Boolean {
        return prefs.getBoolean(key, defaultValue)
    }

    fun putFloat(key: String, value: Float) {
        prefs.edit().putFloat(key, value).apply()
    }

    fun getFloat(key: String, defaultValue: Float): Float {
        return prefs.getFloat(key, defaultValue)
    }

    fun remove(key: String) {
        prefs.edit().remove(key).apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
