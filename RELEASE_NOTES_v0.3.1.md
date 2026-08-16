# Unictoos v0.3.1 — Main release

Unictoos v0.3.1 is the main release focused on correcting the glass navigation layout and reducing avoidable UI-shell work while preserving the RootEncoder 2.8.0 streaming and graphics-lifecycle remediation from v0.3.0.

## User-facing improvements

| Area | Change |
|---|---|
| Top bar boundaries | Added status-bar-safe insets, bounded horizontal padding, a minimum surface height, weighted leading/trailing control regions, and single-line ellipsized title text. The Settings and workspace controls remain inside the glass surface instead of extending beyond the display edges. |
| UI responsiveness | Removed the unnecessary bottom-navigation `animateContentSize` pass, reduced tab-transition duration from 180 ms to 120 ms, and kept the glass navigation visually smooth without animating static navigation-bar layout. |
| Recomposition scope | Moved session, destination, settings, quality, thermal, audio, and diagnostics state collection out of the root app shell into the active Home, Studio, and Settings routes. High-frequency stream-state updates no longer force the entire navigation shell to collect and recompose unrelated screens. |
| Streaming behavior | Preserved the queued-start fix, RootEncoder 2.8.0 adapter, terminal release boundary, generation-safe graphics recovery, Infinix preview isolation, credential encryption, and provider configuration paths. |

## Validation

The final release gate includes lint, unit tests, debug packaging, instrumentation-test APK compilation, R8 release packaging, source diff hygiene, and static smoke checks. The smoke suite includes explicit safe-boundary, route-local-state, animation-cost, stream-start, and graphics-lifecycle guards.

Physical Android streaming and UI frame-time measurement still require a connected device. This release does not claim a physical network-ingestion or Infinix X6853 soak result from the sandbox.
