package com.unictoai.unictoos.stream

import com.unictoai.unictoos.domain.StreamQuality
import com.unictoai.unictoos.domain.StreamQualityPreset

enum class QualityTier(
    val label: String,
    val width: Int,
    val height: Int,
    val fps: Int,
    val targetBitrate: Int,
) {
    TIER_1080P60("1080p60", 1_920, 1_080, 60, 8_000_000),
    TIER_1080P30("1080p30", 1_920, 1_080, 30, 6_000_000),
    TIER_720P60("720p60", 1_280, 720, 60, 6_000_000),
    TIER_720P30("720p30", 1_280, 720, 30, 4_500_000),
    TIER_480P30("480p30", 854, 480, 30, 2_000_000),
    TIER_360P30("360p30", 640, 360, 30, 1_200_000),
}

fun StreamQuality.toQualityTier(): QualityTier = when (preset) {
    StreamQualityPreset.FULL_HD_HIGH_FPS -> QualityTier.TIER_1080P60
    StreamQualityPreset.FULL_HD -> QualityTier.TIER_1080P30
    StreamQualityPreset.HIGH_FPS_720 -> QualityTier.TIER_720P60
    StreamQualityPreset.BALANCED, StreamQualityPreset.CUSTOM -> QualityTier.TIER_720P30
    StreamQualityPreset.DATA_SAVER -> QualityTier.TIER_480P30
}
