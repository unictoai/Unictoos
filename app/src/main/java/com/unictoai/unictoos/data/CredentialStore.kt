package com.unictoai.unictoos.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.unictoai.unictoos.domain.PlatformPreset
import java.nio.ByteBuffer
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface CredentialRepository {
    fun save(platform: PlatformPreset, serverUrl: String, streamKey: String)
    fun load(platform: PlatformPreset): Pair<String, String>
    fun clear(platform: PlatformPreset)
}

class CredentialStore(context: Context) : CredentialRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    init {
        ensureKey()
        migrateLegacyYoutubeCredentials()
    }

    override fun save(platform: PlatformPreset, serverUrl: String, streamKey: String) {
        preferences.edit()
            .putString(serverKey(platform), encrypt(serverUrl))
            .putString(streamKey(platform), encrypt(streamKey))
            .apply()
    }

    override fun load(platform: PlatformPreset): Pair<String, String> =
        decrypt(preferences.getString(serverKey(platform), null)).orEmpty() to
            decrypt(preferences.getString(streamKey(platform), null)).orEmpty()

    override fun clear(platform: PlatformPreset) {
        preferences.edit()
            .remove(serverKey(platform))
            .remove(streamKey(platform))
            .apply()
    }

    fun save(serverUrl: String, streamKey: String) = save(PlatformPreset.YOUTUBE, serverUrl, streamKey)

    fun load(): Pair<String, String> = load(PlatformPreset.YOUTUBE)

    fun clear() = clear(PlatformPreset.YOUTUBE)

    private fun migrateLegacyYoutubeCredentials() {
        val hasNew = preferences.contains(serverKey(PlatformPreset.YOUTUBE)) || preferences.contains(streamKey(PlatformPreset.YOUTUBE))
        val legacyServer = preferences.getString(LEGACY_SERVER_URL, null)
        val legacyStream = preferences.getString(LEGACY_STREAM_KEY, null)
        if (!hasNew && (!legacyServer.isNullOrBlank() || !legacyStream.isNullOrBlank())) {
            preferences.edit()
                .putString(serverKey(PlatformPreset.YOUTUBE), legacyServer.orEmpty())
                .putString(streamKey(PlatformPreset.YOUTUBE), legacyStream.orEmpty())
                .remove(LEGACY_SERVER_URL)
                .remove(LEGACY_STREAM_KEY)
                .apply()
        }
    }

    private fun serverKey(platform: PlatformPreset) = "${platform.name.lowercase()}_server_url"

    private fun streamKey(platform: PlatformPreset) = "${platform.name.lowercase()}_stream_key"

    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        val payload = ByteBuffer.allocate(4 + cipher.iv.size + encrypted.size)
            .putInt(cipher.iv.size)
            .put(cipher.iv)
            .put(encrypted)
            .array()
        return Base64.encodeToString(payload, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            val payload = Base64.decode(encoded, Base64.NO_WRAP)
            val buffer = ByteBuffer.wrap(payload)
            val ivSize = buffer.int
            val iv = ByteArray(ivSize).also(buffer::get)
            val ciphertext = ByteArray(buffer.remaining()).also(buffer::get)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(TAG_BITS, iv))
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        }.getOrNull()
    }

    private fun ensureKey() {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
            generator.init(
                KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setRandomizedEncryptionRequired(true)
                    .build(),
            )
            generator.generateKey()
        }
    }

    private fun getKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return (keyStore.getEntry(KEY_ALIAS, null) as KeyStore.SecretKeyEntry).secretKey
    }

    companion object {
        private const val PREFERENCES = "unictoos_secure_credentials"
        private const val KEY_ALIAS = "unictoos_stream_credentials"
        private const val LEGACY_SERVER_URL = "encrypted_server_url"
        private const val LEGACY_STREAM_KEY = "encrypted_stream_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val TAG_BITS = 128
    }
}
