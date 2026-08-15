# Unictoos v0.2.0-alpha1

Unictoos v0.2 is a deliberate visual and architectural refinement for serious mobile creators. It preserves the alpha18 streaming, capture, credential, and scene foundations while making the interface calmer, more legible, and less wasteful during live sessions.

## Visual system

The app now uses an approved neutral token system with a refined near-black-to-near-white scale, one purposeful blue accent, separate danger and caution colors, named 4-point spacing tokens, and a small motion system. The previous broad violet, magenta, cyan, mint, and amber treatment has been removed from the migrated UI surfaces. Functional source-type cues remain muted and purposeful.

Studio is now content-first: the preview area has less chrome, health values are quieter dashboard data, the live indicator uses a subtle opacity-only breath, and Go Live/Stop is the single confident action. Home, Scenes, Engage, Library, Settings, More, shared components, and bottom navigation use the same token language. Empty states now use restrained copy and a single clear action.

## Performance and stability

The root Compose shell no longer collects the per-second session and health-history flows. Session state is collected in Home and Studio routes, while health history is collected only in Studio. This reduces unnecessary invalidation of inactive navigation content. The data models used by these screens already carry Compose immutability annotations, so no new dependency or broad business-logic rewrite was required.

The declared core-splashscreen dependency is now wired into `MainActivity` before `super.onCreate`, with neutral v0.2 launch resources. `PreviewSurfaceView` now removes its SurfaceHolder callback and service listener when detached and re-registers the callback when attached, reducing configuration-change leak risk. Release R8 minification is enabled with conservative keep rules for Android entry points, ViewModel construction, preview, and RootEncoder.

## Validation

The following checks passed repeatedly after isolated tasks and after the final visual/performance changes:

```text
./gradlew lintDebug testDebugUnitTest assembleDebug
python3 tools/feature_smoke_test.py
```

The result was **47/47 smoke checks passed**. The optimized unsigned release APK was also built successfully with:

```text
./gradlew assembleRelease --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8'
```

The larger heap was required by R8 in the sandbox. This is a build-environment requirement, not an application runtime requirement.

## Important scope limits

This alpha1 release does not claim official YouTube, Twitch, or Kick OAuth/chat/moderation APIs; those remain separate integration work. The current compositor milestone supports the existing RootEncoder path and text overlays, but simultaneous camera-plus-screen PiP, full multi-source GPU compositing, image backgrounds, scene transitions, and true multi-destination streaming remain future work. Physical-device measurements are still required for cold-start time, frame latency, GPU load, and long-session memory growth.
