## RootEncoder GL recovery hardening

Alpha7 is a focused Priority 1 device-test build for the graphics-resource exhaustion failure reproduced on the Infinix X6853 running Android 15. It upgrades RootEncoder from 2.4.5 to 2.5.9, which provides improved GL-resource shutdown behavior and a supported render-error callback.

## Included fixes

- Uses RootEncoder 2.5.9 and its migrated source packages.
- Registers `RenderErrorCallback` for each generation-bound `GenericStream`.
- Stops RootEncoder’s GL interface synchronously from the render-error callback before the failed render executor can disappear.
- Performs a second serialized service teardown that stops recording, streaming, preview, GL, media sources, and the generic stream in a deterministic order.
- Makes graphics-failure recovery idempotent across RootEncoder callbacks, the global uncaught-exception bridge, and the Studio Fix button.
- Prevents a late global failure intent from overwriting the successful Fix-button release state.
- Records bounded redacted diagnostics for render-error handling, pipeline-release start, pipeline-release errors, and pipeline-release completion.
- Registers `PreviewSurfaceView`’s holder callback exactly once instead of registering it again on every window attachment.
- Suppresses duplicate surface-destroy notifications during Compose disposal and Android surface teardown.

## Scope limits

This build does not implement multi-destination streaming, a GPU compositor, OAuth integrations, or a new credential-encryption format. Keystore-backed credential encryption and existing reconnect/authentication policy are unchanged.

The graphics-resource exhaustion issue remains physical-device gated. The new code addresses the confirmed stale-EGL ownership path and makes the release boundary observable, but sandbox compilation and unit tests cannot prove the behavior on the Infinix GPU. Do not use an important broadcast or an irreplaceable stream key during validation.

## Automated validation

The following checks passed for the alpha7 source:

- `lintDebug`
- `testDebugUnitTest`
- `assembleDebug`
- `tools/feature_smoke_test.py` with 47/47 checks passing
- APK metadata verification for versionCode 25 and versionName 0.2.0-alpha7

## Required device test

Install alpha7 on the Infinix X6853 and reproduce the graphics-resource exhaustion failure. Confirm that the preview error appears, tap **Fix**, and verify that the app shows **“Capture resources released. Start capture again”** without the error returning from a late failure intent. Start screen capture again and confirm that the preview succeeds. Repeat the failure-and-recovery sequence at least three times, then test camera capture separately.

If the failure recurs, export or capture diagnostics immediately after the event and provide the sequence: whether the Fix message appeared, whether the next capture prepared, and whether the error returned before or after the preview surface appeared.
