# Unictoos v0.5.4

Unictoos v0.5.4 is a bug-fix and visual-refinement release on top of v0.5.3. Streaming, capture, RootEncoder transport, destination, permission, and recovery behavior are unchanged.

## Bug fixes

- **Diagnostics export crash fixed.** Settings → Export diagnostics no longer crashes: the FileProvider now exposes the cache directory where the bundle is written.
- **Stale session scaffolding removed.** `StudioViewModel` no longer carries manual stream-state methods that raced the real `StreamingStatusBus`; the UI session state is a single mirror of the service bus.
- **Keystore recovery.** When Android invalidates the Keystore (e.g. lock-screen change), undecryptable destination credentials are now cleared and the user is asked to re-enter them, with a one-time notice in Settings, instead of failing silently.

## UI refinement

- **Restrained professional theme** per the v0.2 design-token proposal: cool neutral surfaces, one `#5B8DEF` blue accent reserved for primary actions, selection, and the live indicator. The competing cyan/violet/orange accents are gone.
- **Static backdrop.** The always-on animated background canvas (redrawn every frame on every screen) is replaced with a static gradient — calmer visuals and less GPU churn.
- **Unified live indicator.** The duplicated pulse dot is now a single shared component with slow opacity-only breathing.

## Repository

- README rewritten: badges, feature matrix, quick start, build/test commands, architecture sketch, roadmap.
- LICENSE fixed so GitHub detects Apache-2.0 (was reported as "Other").

## Evidence boundary

- Package `com.unictoai.unictoos`, versionName `0.5.4`, versionCode `64`.
- Static verification only in this environment: 154/155 feature smoke checks pass (the one failure is "debug APK exists", which needs an Android build), security source audit clean (0 credential literals, 0 direct log calls).
- No physical-device, platform-ingest, endurance, or OEM background validation yet. Alpha: do not trust an important broadcast to this build.
