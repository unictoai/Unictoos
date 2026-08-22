# Unictoos v0.4.7

Unictoos v0.4.7 is a focused usability and startup correction release. It addresses the reported first-run experience where the Go Live action was difficult to discover, capture permissions did not appear when expected, and the Home screen felt unnecessarily animated on mobile hardware.

## Go Live and first-run flow

The center navigation item is now labeled **Go Live**, the Home hero has a direct **Go Live** action, and completing onboarding opens the Go Live workspace instead of returning users to a generic home screen. The default selected scene is the usable Main Camera scene, while older installations automatically fall back to the first scene containing an enabled camera or screen source.

Pressing Go Live now retains the request state and asks for missing microphone permission first, plus camera permission when camera capture is selected. Screen capture then opens Android’s system capture-consent dialog after app permissions succeed. If permissions are granted before a destination is configured, Unictoos explains the next required step instead of silently skipping the permission flow or starting a doomed capture attempt. Practice mode keeps its separate local-recording path.

## Readiness and performance

Home readiness cards now report the real microphone and Internet capability state. The preflight card no longer treats any active network object as a working Internet connection. Redundant always-visible Home entrance animations and static-card `animateContentSize` layout work have been removed, reducing unnecessary measure/layout activity on lower-powered Android devices while retaining the live-status animation and Studio controls.

## Validation

The release is gated by the updated static smoke suite, JVM unit tests, debug lint, debug APK assembly, instrumentation APK assembly, minified release APK assembly, package/version inspection, checksum verification, and whitespace checks. Physical-device validation remains necessary for the full Android permission dialogs, preview, microphone capture, platform ingest, and long-running stream behavior.
