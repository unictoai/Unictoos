# Unictoos v0.1.0-alpha15

## Infinix XOS startup compatibility patch

Alpha15 addresses the immediate launch crash reported on an **Infinix X6853 running XOS 15.1.2**. The most likely failure boundary was Android Keystore initialization: alpha13/alpha14 constructed `CredentialStore` as a ViewModel default dependency, so a device-specific Keystore failure could occur before the first Compose screen was rendered.

The patched startup path now treats Keystore construction as a capability that may be unavailable on a particular device. If initialization fails, Unictoos opens with an empty credential state instead of crashing. CredentialStore encryption and normal save/load behavior remain unchanged when the Android Keystore is available. The app does not silently invent or expose stream keys.

## Validation

Alpha15 passed `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and the repository feature smoke suite with **47/47 checks passed**.

## Installation

Uninstall alpha14 before installing alpha15 if Android reports an update conflict. Open the app once after installation. If the app opens, configure the destination again in Settings because a device whose Keystore is unavailable cannot securely retain stream credentials until the platform issue is resolved.

If alpha15 still crashes immediately, the next required diagnostic is the Android Logcat stack trace from the Infinix device; the screenshot notification alone does not identify the failing class or line.
