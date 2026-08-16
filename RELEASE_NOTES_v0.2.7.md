# Unictoos v0.2.7 — Glassy mobile navigation

Unictoos v0.2.7 refines the mobile navigation hierarchy without changing the underlying screens or streaming behavior.

## Navigation changes

- The bottom bar now contains exactly three primary creator actions: **Home**, **Studio**, and **Library**.
- **Settings** is available from a dedicated glass control in the top-right corner.
- **Scenes**, **Engage**, and **More tools** are available from the top-left glass dropdown.
- The active destination remains visible in the glass top bar so secondary screens do not feel disconnected from the workspace.
- The navigation uses translucent dark surfaces, restrained borders, rounded glass containers, subtle elevation, and the existing Unictoos blue accent.

## Preserved behavior

All existing screen routes remain available. Scenes, Engage, More, Settings, Studio, Home, and Library retain their existing content and callbacks. The redesign does not modify capture, RootEncoder, CredentialStore encryption, diagnostics, preflight logic, or multistream behavior.

## Validation

The required validation gate passed:

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx768m -Dfile.encoding=UTF-8'
python3 tools/feature_smoke_test.py
```

The static smoke suite reported **59/59 checks passed**. Because the sandbox has no usable Android emulator acceleration and no connected physical device, this release has not received screenshot-based runtime UI verification in the sandbox.
