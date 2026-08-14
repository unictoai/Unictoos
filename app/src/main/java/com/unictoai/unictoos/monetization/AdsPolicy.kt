package com.unictoai.unictoos.monetization

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AdSlot {
    HOME_BANNER,
    LIBRARY_BANNER,
    SETTINGS_BANNER,
}

/**
 * Provider-neutral advertising contract. A future provider adapter can implement this
 * without receiving stream frames, stream keys, or microphone/capture state.
 */
interface AdProvider {
    fun isAvailable(): Boolean
    fun load(slot: AdSlot)
    fun clear(slot: AdSlot)
}

data class AdsPolicy(
    val enabled: Boolean = false,
    val consentGranted: Boolean = false,
    val broadcastsNeverInterrupted: Boolean = true,
    val allowedSlots: Set<AdSlot> = setOf(AdSlot.HOME_BANNER, AdSlot.LIBRARY_BANNER),
)

class AdsPreferences(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val _policy = MutableStateFlow(
        AdsPolicy(
            enabled = preferences.getBoolean(KEY_ENABLED, false),
            consentGranted = preferences.getBoolean(KEY_CONSENT, false),
        ),
    )
    val policy: StateFlow<AdsPolicy> = _policy.asStateFlow()

    fun setEnabled(enabled: Boolean) {
        val next = _policy.value.copy(enabled = enabled, consentGranted = enabled)
        preferences.edit().putBoolean(KEY_ENABLED, enabled).putBoolean(KEY_CONSENT, next.consentGranted).apply()
        _policy.value = next
    }

    companion object {
        private const val PREFERENCES = "unictoos_ads_policy"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_CONSENT = "consent_granted"
    }
}

object NoOpAdProvider : AdProvider {
    override fun isAvailable(): Boolean = false
    override fun load(slot: AdSlot) = Unit
    override fun clear(slot: AdSlot) = Unit
}
