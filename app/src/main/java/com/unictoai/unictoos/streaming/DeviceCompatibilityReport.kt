package com.unictoai.unictoos.streaming

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import com.unictoai.unictoos.domain.MultistreamDefaults
import com.unictoai.unictoos.domain.StreamQuality

/** Severity for a supportability check; CAUTION is not a hard block. */
enum class CompatibilityLevel {
    READY,
    CAUTION,
    BLOCKED,
}

data class CompatibilityCheck(
    val id: String,
    val label: String,
    val value: String,
    val level: CompatibilityLevel,
    val detail: String,
)

data class DeviceCompatibilityReport(
    val manufacturer: String,
    val model: String,
    val sdkInt: Int,
    val checks: List<CompatibilityCheck>,
) {
    val overallLevel: CompatibilityLevel
        get() = when {
            checks.any { it.level == CompatibilityLevel.BLOCKED } -> CompatibilityLevel.BLOCKED
            checks.any { it.level == CompatibilityLevel.CAUTION } -> CompatibilityLevel.CAUTION
            else -> CompatibilityLevel.READY
        }

    val summary: String
        get() = when (overallLevel) {
            CompatibilityLevel.READY -> "Profile looks ready for the selected settings"
            CompatibilityLevel.CAUTION -> "Profile is usable with stability precautions"
            CompatibilityLevel.BLOCKED -> "Selected settings are not safe for this profile"
        }
}

object DeviceCompatibilityReportFactory {
    fun current(context: Context, quality: StreamQuality): DeviceCompatibilityReport {
        val packageManager = context.packageManager
        val cameraAvailable = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        val modelLabel = "${Build.MANUFACTURER} ${Build.MODEL}".trim().ifBlank { "Unknown Android device" }
        return fromInputs(
            manufacturer = Build.MANUFACTURER,
            model = Build.MODEL,
            sdkInt = Build.VERSION.SDK_INT,
            quality = quality,
            cameraAvailable = cameraAvailable,
            isLargeHeap = (context.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_LARGE_HEAP) != 0,
            isKnownThreeDestinationDevice = modelLabel.contains(MultistreamDefaults.THREE_DESTINATION_DEVICE_GATE, ignoreCase = true),
        )
    }

    fun fromInputs(
        manufacturer: String,
        model: String,
        sdkInt: Int,
        quality: StreamQuality,
        cameraAvailable: Boolean,
        isLargeHeap: Boolean,
        isKnownThreeDestinationDevice: Boolean = false,
    ): DeviceCompatibilityReport {
        val checks = buildList {
            add(
                CompatibilityCheck(
                    id = "android_sdk",
                    label = "Android version",
                    value = "Android $sdkInt",
                    level = if (sdkInt >= 29) CompatibilityLevel.READY else CompatibilityLevel.BLOCKED,
                    detail = if (sdkInt >= 29) "Supported by the current minSdk" else "Android 10 or newer is required",
                ),
            )
            add(
                CompatibilityCheck(
                    id = "camera",
                    label = "Camera capability",
                    value = if (cameraAvailable) "Available" else "Not detected",
                    level = if (cameraAvailable) CompatibilityLevel.READY else CompatibilityLevel.CAUTION,
                    detail = if (cameraAvailable) "Camera scenes can be prepared when permission is granted" else "Screen capture can still be used; camera scenes are unavailable",
                ),
            )
            val pixels = quality.width.toLong() * quality.height.toLong()
            val highLoad = quality.fps >= 60 || pixels >= 2_073_600L || quality.bitrate > 6_000_000
            add(
                CompatibilityCheck(
                    id = "stream_profile",
                    label = "Selected stream profile",
                    value = "${quality.width}×${quality.height} • ${quality.fps} FPS",
                    level = if (highLoad) CompatibilityLevel.CAUTION else CompatibilityLevel.READY,
                    detail = if (highLoad) "Use a stable connection and watch temperature; lower quality if preview or encoder errors appear" else "Conservative profile for mobile streaming",
                ),
            )
            add(
                CompatibilityCheck(
                    id = "memory",
                    label = "Memory posture",
                    value = if (isLargeHeap) "Large-heap profile" else "Standard heap profile",
                    level = if (isLargeHeap) CompatibilityLevel.READY else CompatibilityLevel.CAUTION,
                    detail = if (isLargeHeap) "The app requests the device large-heap profile" else "Keep scenes and overlays lightweight during long sessions",
                ),
            )
            add(
                CompatibilityCheck(
                    id = "multistream_cap",
                    label = "Direct multistream cap",
                    value = "Up to ${MultistreamDefaults.DIRECT_DESTINATION_CAP} destinations",
                    level = if (isKnownThreeDestinationDevice) CompatibilityLevel.CAUTION else CompatibilityLevel.READY,
                    detail = if (isKnownThreeDestinationDevice) "Three destinations remain gated until this device passes long-duration validation" else "Three-destination rollout is feature-gated and not enabled by default",
                ),
            )
        }
        return DeviceCompatibilityReport(
            manufacturer = manufacturer,
            model = model,
            sdkInt = sdkInt,
            checks = checks,
        )
    }
}
