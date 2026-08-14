package com.unictoai.unictoos.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.unictoai.unictoos.domain.PlatformPreset
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CredentialStoreTest {
    private lateinit var context: Context
    private lateinit var preferences: android.content.SharedPreferences

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        preferences = context.getSharedPreferences("unictoos_secure_credentials", Context.MODE_PRIVATE)
        preferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun saveAndLoadRoundTripPreservesServerAndKey() {
        val store = CredentialStore(context)

        store.save(PlatformPreset.YOUTUBE, "rtmps://youtube.example/app", "youtube-key")

        assertEquals("rtmps://youtube.example/app", store.load(PlatformPreset.YOUTUBE).first)
        assertEquals("youtube-key", store.load(PlatformPreset.YOUTUBE).second)
    }

    @Test
    fun platformsDoNotClobberEachOther() {
        val store = CredentialStore(context)

        store.save(PlatformPreset.YOUTUBE, "youtube-url", "youtube-key")
        store.save(PlatformPreset.TWITCH, "twitch-url", "twitch-key")

        assertEquals("youtube-url" to "youtube-key", store.load(PlatformPreset.YOUTUBE))
        assertEquals("twitch-url" to "twitch-key", store.load(PlatformPreset.TWITCH))
    }

    @Test
    fun clearRemovesBothCredentialFields() {
        val store = CredentialStore(context)
        store.save(PlatformPreset.KICK, "kick-url", "kick-key")

        store.clear(PlatformPreset.KICK)

        assertEquals("" to "", store.load(PlatformPreset.KICK))
    }

    @Test
    fun legacyYoutubeCredentialsMigrateExactlyOnce() {
        val store = CredentialStore(context)
        val encrypt = CredentialStore::class.java.getDeclaredMethod("encrypt", String::class.java).apply { isAccessible = true }
        val legacyUrl = encrypt.invoke(store, "legacy-url") as String
        val legacyKey = encrypt.invoke(store, "legacy-key") as String
        preferences.edit()
            .putString("encrypted_server_url", legacyUrl)
            .putString("encrypted_stream_key", legacyKey)
            .remove("youtube_server_url")
            .remove("youtube_stream_key")
            .commit()

        val migrated = CredentialStore(context)

        assertEquals("legacy-url" to "legacy-key", migrated.load(PlatformPreset.YOUTUBE))
        assertFalse(preferences.contains("encrypted_server_url"))
        assertFalse(preferences.contains("encrypted_stream_key"))
        assertTrue(preferences.contains("youtube_server_url"))
        assertTrue(preferences.contains("youtube_stream_key"))

        val secondConstruction = CredentialStore(context)
        assertEquals("legacy-url" to "legacy-key", secondConstruction.load(PlatformPreset.YOUTUBE))
    }

    @Test
    fun corruptedPayloadReturnsEmptyValueWithoutThrowing() {
        CredentialStore(context)
        preferences.edit()
            .putString("youtube_server_url", "not-valid-base64")
            .putString("youtube_stream_key", "also-not-valid")
            .commit()

        val reloaded = CredentialStore(context)

        assertEquals("" to "", reloaded.load(PlatformPreset.YOUTUBE))
    }
}
