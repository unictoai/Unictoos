# Unictoos v0.1.0-alpha12

## Brand and mobile UI correction

Alpha12 replaces the bundled Unictoos mark with the supplied clean black-and-white logo. The logo is presented on intentional white brand surfaces in the header and Studio preview so it remains legible against the graphite UI instead of appearing as an accidental white block.

The bottom navigation now contains five focused destinations: Home, Studio, Scenes, Library, and More. Engage and Settings are grouped under More so the primary navigation is not overcrowded on narrow Android screens.

Studio now uses a clear action hierarchy. The primary full-width control is Go live or Stop broadcast. Mute and Record are grouped as secondary session controls, while Edit scene and Practice are lower-priority workflow actions. Library recording cards now use a readable identity section, full-width Play and Share actions, and a separate Rename/Delete row. Settings now uses a full-width secure save action with a separate destructive remove action. Home quick actions use a two-column layout plus a full-width Library action.

The cleanup pass removed stale project-local `.gradle`, `build`, and `app/build` artifacts before rebuilding. Build outputs naturally recreate the required Gradle cache and APK directories during compilation; these can be removed again after packaging without changing source state. No user credentials, recordings, scenes, or Android SDK installations were deleted.

## Preserved functionality

Alpha11 creator history and markers, alpha10 engagement boundaries, alpha09 scene layers, alpha08 Practice mode and recording library, the Executive Broadcast design system, secure credentials, capture safeguards, health history, bounded reconnect, and app-only advertising policy remain included.

## Validation

| Check | Result |
|---|---|
| Kotlin/Compose compilation | Passed |
| Android lint | Passed |
| JVM unit tests | Passed |
| Debug APK build | Passed |
| Release APK build | Passed |
| Static feature smoke suite | **44/44 passed** |
| Physical-device validation | Still required; no Android device/emulator is available in the sandbox |

## Device note

Install alpha12 on a physical device and verify the five-tab navigation, More surface, Studio control hierarchy, logo legibility, destination setup, recording Library actions, and all previously documented capture and streaming cases.
