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

    /**
     * Migrates the original YouTube fields without ever copying legacy ciphertext into
     * current-format fields. The original app used the same AES/GCM layout and alias, so
     * encrypted legacy values are decrypted first and encrypted again before they are saved.
     *
     * Some development builds stored legacy values as plaintext under the misleading
     * encrypted_* names. Only values with an unambiguous textual shape are accepted as
     * plaintext. Ambiguous or malformed values remain in the legacy fields rather than being
     * guessed into a credential or discarded.
     */
    private fun migrateLegacyYoutubeCredentials() {
        val legacyServer = preferences.getString(LEGACY_SERVER_URL, null)
        val legacyStream = preferences.getString(LEGACY_STREAM_KEY, null)
        if (legacyServer == null && legacyStream == null) return

        val youtubeServerKey = serverKey(PlatformPreset.YOUTUBE)
        val youtubeStreamKey = streamKey(PlatformPreset.YOUTUBE)
        val hasNewServer = preferences.contains(youtubeServerKey)
        val hasNewStream = preferences.contains(youtubeStreamKey)
        val server = migrateLegacyValue(legacyServer, LegacyField.SERVER_URL)
        val stream = migrateLegacyValue(legacyStream, LegacyField.STREAM_KEY)
        val editor = preferences.edit()
        var changed = false

        // New-format data wins per field. A partial or malformed sibling remains untouched
        // so it can be recovered explicitly instead of being silently discarded.
        if (hasNewServer && legacyServer != null) {
            editor.remove(LEGACY_SERVER_URL)
            changed = true
        } else if (!hasNewServer && server is LegacyValue.Valid) {
            editor.putString(youtubeServerKey, encrypt(server.value))
            editor.remove(LEGACY_SERVER_URL)
            changed = true
        }
        if (hasNewStream && legacyStream != null) {
            editor.remove(LEGACY_STREAM_KEY)
            changed = true
        } else if (!hasNewStream && stream is LegacyValue.Valid) {
            editor.putString(youtubeStreamKey, encrypt(stream.value))
            editor.remove(LEGACY_STREAM_KEY)
            changed = true
        }
        if (changed) editor.apply()
    }

    private fun migrateLegacyValue(raw: String?, field: LegacyField): LegacyValue {
        if (raw.isNullOrBlank()) return LegacyValue.Valid("")
        decrypt(raw)?.let { return LegacyValue.Valid(it) }
        if (field.acceptsPlaintext(raw)) return LegacyValue.Valid(raw)
        return LegacyValue.Invalid
    }

    private enum class LegacyField {
        SERVER_URL {
            override fun acceptsPlaintext(value: String): Boolean =
                value.startsWith("rtmp://", ignoreCase = true) || value.startsWith("rtmps://", ignoreCase = true)
        },
        STREAM_KEY {
            override fun acceptsPlaintext(value: String): Boolean =
                value.length in MIN_PLAINTEXT_KEY_LENGTH..MAX_PLAINTEXT_KEY_LENGTH &&
                    value.all { it.isLetterOrDigit() || it == '-' || it == '_' || it == '.' }
        },
        ;

        abstract fun acceptsPlaintext(value: String): Boolean
    }

    private sealed interface LegacyValue {
        data class Valid(val value: String) : LegacyValue
        data object Invalid : LegacyValue
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
            require(ivSize in MIN_IV_SIZE..MAX_IV_SIZE) { "Invalid credential IV size" }
            require(buffer.remaining() > ivSize) { "Credential payload is incomplete" }
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
        private const val MIN_IV_SIZE = 12
        private const val MAX_IV_SIZE = 32
        private const val MIN_PLAINTEXT_KEY_LENGTH = 4
        private const val MAX_PLAINTEXT_KEY_LENGTH = 512
    }
}
