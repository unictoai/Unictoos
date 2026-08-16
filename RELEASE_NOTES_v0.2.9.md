# Unictoos v0.2.9 — Stream start and navigation correction

Unictoos v0.2.9 is a corrective release for two issues reported after v0.2.8.

## Fixes

| Reported issue | Correction |
|---|---|
| Pressing Go live could result in no stream start | A start command arriving while capture was in `PREPARING` is now retained as a pending request instead of being discarded. The existing active-session guards remain unchanged, and the request is dispatched once capture and encoder preparation finish. |
| Glassy settings and workspace bar appeared below the intended top position | `GlassyTopBar` is now owned by the Material `Scaffold` `topBar` slot. The former 72dp content offset was removed, so the bar is laid out above the screen content while the bottom bar remains independent. |

## Validation

The corrective source passed lint, unit tests, debug assembly, source diff hygiene, and the expanded smoke suite. The smoke suite includes explicit checks for the queued-start admission path and Scaffold top-bar placement. Physical streaming confirmation still requires an Android device; the sandbox has no connected device and cannot certify platform ingestion or the Infinix X6853 graphics behavior.
