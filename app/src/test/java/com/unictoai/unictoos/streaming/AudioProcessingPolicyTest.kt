package com.unictoai.unictoos.streaming

import com.unictoai.unictoos.integrations.AudioProcessingProfile
import com.unictoai.unictoos.integrations.EqualizerBand
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioProcessingPolicyTest {
    @Test
    fun acceptsSafeProfile() {
        val result = AudioProcessingPolicy.validate(
            AudioProcessingProfile(
                noiseGateThresholdDb = -48f,
                compressorRatio = 4f,
                limiterCeilingDb = -1f,
                equalizerBands = listOf(EqualizerBand(1_000, 3f)),
            ),
        )
        assertTrue(result is AudioProcessingValidation.Valid)
    }

    @Test
    fun rejectsUnsafeProfile() {
        assertTrue(AudioProcessingPolicy.validate(AudioProcessingProfile(noiseGateThresholdDb = 4f)) is AudioProcessingValidation.Invalid)
        assertTrue(AudioProcessingPolicy.validate(AudioProcessingProfile(compressorRatio = 24f)) is AudioProcessingValidation.Invalid)
        assertTrue(AudioProcessingPolicy.validate(AudioProcessingProfile(equalizerBands = listOf(EqualizerBand(10, 20f)))) is AudioProcessingValidation.Invalid)
    }
}
