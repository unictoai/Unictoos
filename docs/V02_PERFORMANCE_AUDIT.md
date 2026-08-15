# Unictoos v0.2 performance audit

## Scope

This audit covers the v0.2 requirements for Compose recomposition, preview-surface efficiency, cold start, lifecycle cleanup, and release configuration. It deliberately does not change RootEncoder’s media path, credential encryption, or reconnect/authentication policy.

## Findings and changes

| Area | Alpha18 finding | v0.2 result | Evidence level |
|---|---|---|---|
| Compose state collection | `UnictoosApp` collected session and health history at the root, so health ticks could invalidate the root shell and the active tab switch. | Session collection now lives in `HomeRoute` and `StudioRoute`; health history is collected only in `StudioRoute`. The root still owns navigation and relatively stable settings/scenes data. | Static code audit; validated by build and smoke suite |
| Model stability | `Scene`, `Source`, `StreamDestination`, `StreamHealthSample`, and `StreamSessionState` already use `@Immutable`. | No unnecessary model rewrite was made. | Source inspection |
| Preview surface | `PreviewSurfaceView` used a hardware `SurfaceView` and did not copy bitmaps, but its listener and holder callback were not explicitly cleared on detach. | Detach now notifies the service, clears the listener, removes the holder callback, and re-registers it on attach. | Source inspection; build validation |
| Cold start | `core-splashscreen` was declared but `installSplashScreen()` was not called. | `MainActivity` installs the splash before `super.onCreate`; launch resources now use a neutral splash background and post-splash theme. | Source inspection; build validation |
| Release optimization | Release minification was disabled and the ProGuard file was empty. | Conservative R8 minification is enabled with keep rules for Android entry points, `StudioViewModel`, preview, and RootEncoder. | `assembleRelease` passed with a 1 GB Gradle heap |
| Memory cleanup | Preview callback cleanup was incomplete. The service already cancels handlers and releases RootEncoder, camera, microphone, and projection resources in `onDestroy`. | Preview cleanup is now paired at the view lifecycle boundary. No unrelated service reconnect changes were made. | Source inspection |

## Measurement limits

The sandbox does not include a connected physical device with Layout Inspector or Compose compiler metrics enabled. Therefore this change does not claim a numeric before/after recomposition count. The recomposition improvement is structural: the root no longer subscribes to the per-second stream session and health-history flows. Device-level confirmation should use Android Studio Layout Inspector or Compose recomposition counters during a one-minute Practice session, switching between Home, Studio, and Scenes while observing whether inactive screens recompose.

The preview change retains `SurfaceView` and RootEncoder’s GL path. No bitmap copy or software-rendering path was introduced. Actual frame latency, GPU load, and memory behavior still require measurement on representative Android 10–15 devices, especially the Infinix X6853 used for prior crash testing.

## Acceptance checks

The following checks passed after each isolated v0.2 task and after the final performance changes:

```text
./gradlew lintDebug testDebugUnitTest assembleDebug
python3 tools/feature_smoke_test.py
```

The final optimized release was additionally validated with:

```text
./gradlew assembleRelease --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8'
```

R8 required the larger heap in the sandbox; the failure at 512 MB was a build-environment memory limit, not a code or rules failure. The resulting unsigned release APK assembled successfully.

## Remaining debt

The app still needs physical-device measurement for cold-start time, frame latency, preview GPU load, repeated configuration-change sessions, and long-session memory growth. Official platform APIs, multi-source GPU compositing beyond the current text-overlay milestone, and true multistreaming are outside this v0.2 visual/performance scope and remain separate product work.
