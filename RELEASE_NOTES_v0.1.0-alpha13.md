# Unictoos v0.1.0-alpha13

## Engineering brief completion release

Alpha13 extends the executive broadcast foundation with persisted stream-quality profiles, adaptive bitrate protection, thermal-aware throttling, configurable audio processing, session controls, creator telemetry, configuration export, and a first pass of creator-focused visual polish.

### Included

| Area | Delivered |
|---|---|
| Stream quality | 480p/720p/1080p presets, 30/60 FPS options, custom bitrate/FPS controls, persisted selection, Studio chip |
| Adaptive streaming | Pure tested degradation/recovery policy, rolling bitrate window, RootEncoder runtime bitrate adjustment, visible status messaging |
| Device protection | Thermal protection preference, live bitrate cap, dismissible status notice, low-latency mode tradeoff |
| Audio | Standard/High bitrate, echo-cancellation and noise-suppression toggles |
| Session controls | 15/30/60/120-minute auto-stop, data-usage estimate, safe configuration export |
| Scenes | Colored-layer thumbnails, persisted editable TEXT sources, opacity/z-order controls |
| Creator history | Persisted health samples, Library bitrate graph, Studio audio-level meter boundary |
| UI | Higher-contrast LIVE treatment, animated live accent, actionable card strokes, empty-state CTAs, tab crossfade, tactile primary action |
| Documentation | PiP compositor plan, recording-quality limitation, scene-transition/destination boundaries, external-input audit, accessibility and telemetry audits |

### Validation

The release was validated with `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and the repository feature smoke suite. No new dependency was added. CredentialStore encryption and reconnect/auth-error handling were not modified as side effects.

### Known boundaries

RootEncoder 2.4.5 does not provide native screen-plus-camera compositing, independent recording bitrate/resolution, public audio peak telemetry, USB/UVC capture sources, or multi-destination session management. These are documented as future engineering plans rather than exposed as misleading controls.
