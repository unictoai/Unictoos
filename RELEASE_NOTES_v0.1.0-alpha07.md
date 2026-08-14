# Unictoos v0.1.0-alpha07

## Executive Broadcast UI refresh

This release applies the selected Executive Broadcast direction to the native Android app. The Home tab now uses a clearer broadcast-desk hierarchy: one primary broadcast-studio action, a grouped readiness grid, organized quick actions, scene shortcuts, and a compact credential-protection note. The design uses a restrained graphite palette, controlled cyan and mint status accents, stronger typography, and more deliberate spacing.

The updated visual system also carries through Studio, navigation, status pills, metric cards, scene actions, and the supplied black-and-white Unictoos logo. Purple-pink gradient treatment was reduced in the primary workflow in favor of a more established software-product appearance; color remains reserved for status, emphasis, and live-state communication.

## Motion and interaction polish

Home sections enter with short fade-and-rise transitions, the main hero responds smoothly to live-state changes, status labels animate between states, and Studio preview progress appears and disappears with motion. Live pulsing is conditional: it runs only while a stream is active, avoiding a continuous idle animation and reducing unnecessary battery and rendering work.

Cards use short content-size animations for state changes, while action buttons remain stable and easy to reach. The motion system intentionally avoids expensive blur, parallax, particle effects, or always-on animation because Unictoos is a streaming application where capture, encoding, thermals, and battery life take priority.

## Preserved alpha06 functionality

The alpha06 crash-hardening baseline remains intact, including safe empty-scene handling, guarded AudioRecord initialization, foreground-service protection, screen and camera capture flows, microphone mute controls, local recording, secure Android Keystore credentials, preflight checks, actionable error cards, Engage architecture, and the app-only advertising policy. Sponsor banners remain outside stream frames and recordings.

## Validation

The following checks passed in the sandbox:

| Check | Result |
|---|---|
| Kotlin/Compose compilation | Passed |
| Android lint | Passed; existing dependency/deprecation warnings only |
| JVM unit tests | Passed |
| Debug APK build | Passed |
| Release APK build | Passed |
| Feature smoke suite | 33/33 passed |
| Physical-device validation | Still required; no Android device/emulator is available in the sandbox |

## Important testing note

Install the alpha07 APK on a physical Android device and verify launch, all six tabs, microphone permission, screen-capture consent, camera-only capture, destination setup, Go Live, mute/unmute, Record, Stop, and Library indexing before relying on it for a public broadcast.
