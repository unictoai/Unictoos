package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.integrations.AudioProcessingProfile

sealed interface AudioProcessingValidation {
    data object Valid : AudioProcessingValidation
    data class Invalid(val reason: String) : AudioProcessingValidation
}

object AudioProcessingPolicy {
    fun validate(profile: AudioProcessingProfile): AudioProcessingValidation {
        profile.noiseGateThresholdDb?.let { threshold ->
            if (threshold !in -80f..0f) return AudioProcessingValidation.Invalid("Noise gate threshold must be between -80 and 0 dB")
        }
        profile.compressorRatio?.let { ratio ->
            if (ratio !in 1f..20f) return AudioProcessingValidation.Invalid("Compressor ratio must be between 1:1 and 20:1")
        }
        profile.limiterCeilingDb?.let { ceiling ->
            if (ceiling !in -24f..0f) return AudioProcessingValidation.Invalid("Limiter ceiling must be between -24 and 0 dB")
        }
        if (profile.equalizerBands.size > MAX_EQ_BANDS) return AudioProcessingValidation.Invalid("Equalizer supports at most $MAX_EQ_BANDS bands")
        profile.equalizerBands.forEach { band ->
            if (band.centerHz !in 20..20_000) return AudioProcessingValidation.Invalid("Equalizer frequencies must be between 20 Hz and 20 kHz")
            if (band.gainDb !in -12f..12f) return AudioProcessingValidation.Invalid("Equalizer gain must be between -12 and 12 dB")
        }
        return AudioProcessingValidation.Valid
    }

    private const val MAX_EQ_BANDS = 8
}
