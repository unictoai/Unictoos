# Multistream and Mobile-Network Switching Stability Test Report

**Project:** Unictoos  
**Repository:** `unictoai/Unictoos`  
**Branch under test:** `main`  
**Test target:** v0.2.3 source state at the time of audit  
**Automated run:** `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and `tools/feature_smoke_test.py` completed successfully  
**Prepared by:** Manus AI  

## Executive conclusion

The current Unictoos release does **not yet implement simultaneous multistreaming**. The foreground service owns one `GenericStream` instance and one `currentEndpoint`; therefore, there is no multi-destination fan-out integration whose stability can be honestly certified. The existing implementation is a **single-destination stream with network-loss reconnect behavior**.

This report therefore uses two evidence classes. First, it exercises the implemented single-endpoint reconnect and lifecycle contracts through pure unit tests, static smoke checks, and build validation. Second, it defines the physical-device matrix required before certifying a future multi-destination implementation, including Wi-Fi/cellular handoffs and destination-specific failures.

> A passing build or a simulated state test is not evidence that Android radio handoffs preserve an active broadcast. The final handoff rows require a physical Android device with controllable Wi-Fi and mobile data.

## Architecture boundary found during audit

| Capability | Current v0.2.3 behavior | Stability implication |
|---|---|---|
| Active encoder session | One `GenericStream` per foreground service | No shared encoded fan-out path exists |
| Destinations | One `currentEndpoint` string | YouTube, Twitch, and Kick cannot be active simultaneously in this release |
| Network callback | `registerDefaultNetworkCallback` | The service observes default-network loss/availability, not a per-destination network state |
| Reconnect | Up to three attempts with exponential delay plus bounded jitter | Applies to the single active endpoint |
| Authentication error | Stops retrying and surfaces credential guidance | Must remain unchanged when multistreaming is eventually added |
| Capture pipeline during reconnect | The current code retries `startStream` on the existing encoder session | Requires physical validation because RootEncoder behavior during a live-to-reconnect handoff is device and server dependent |
| Aggregate upload budget | Not implemented | A future fan-out preflight must account for the sum of destination bitrates |
| Destination-specific status | Not implemented | A future implementation must isolate one destination's failure from the others |

## Deterministic test matrix

The following matrix is the required test contract. `Automated` means it can be exercised without a physical radio; `Device` means it must be performed on an Android device while streaming to controlled RTMP test endpoints or real platform destinations.

| ID | Scenario | Procedure | Expected result for current single endpoint | Expected result for future multistream | Evidence class |
|---|---|---|---|---|---|
| N01 | Stable Wi-Fi start | Prepare capture, attach preview, start one destination on stable Wi-Fi | CONNECTING → LIVE; no reconnect | All selected destinations reach LIVE independently | Automated + Device |
| N02 | Stable cellular start | Disable Wi-Fi, start on mobile data | Same as N01 | Same as N01 with aggregate upload preflight | Device |
| N03 | Wi-Fi → cellular handoff | Start live on Wi-Fi; disable Wi-Fi while cellular remains available | RECONNECTING, bounded retry, then LIVE or explicit error; capture must remain intact | Healthy destinations reconnect independently; no healthy destination is stopped because another retries | Device |
| N04 | Cellular → Wi-Fi handoff | Start live on cellular; enable Wi-Fi and disable cellular | Same as N03 | Same as N03 | Device |
| N05 | Full connectivity loss | Start live; disable Wi-Fi and cellular for 20–30 seconds; restore | RECONNECTING while retry budget remains; explicit error after the maximum; no stale callback can resurrect the session after Stop | Each destination records the outage separately; restoration retries only failed destinations | Device + Automated state tests |
| N06 | Airplane mode interruption | Start live; enable airplane mode; wait; disable airplane mode | Same as N05, with no duplicate reconnect timers | Per-destination independent recovery; one aggregate incident in timeline | Device |
| N07 | Network flap | Toggle the active transport three times during a 2-minute stream | At most one reconnect timer at a time; no duplicate start calls; eventual LIVE or explicit terminal error | Same per destination; no cross-destination reconnect amplification | Device + diagnostics |
| N08 | Handoff during CONNECTING | Change transport after start but before onConnectionSuccess | No duplicate stream attempt; stale generation callbacks ignored | Destination sessions retain independent generation guards | Device + Automated state tests |
| N09 | Handoff during local recording | Start broadcast and recording; switch networks | Recording continues or finalizes validly; reconnect does not release capture | Recording policy is explicit: primary destination or local composed output | Device |
| N10 | Destination authentication failure | Use an invalid key for one destination while another is valid | Current single endpoint stops with auth error; no retry loop | Invalid destination becomes ERROR only; valid destination remains LIVE | Device + Automated policy tests |
| N11 | Destination server rejection | Use a server that accepts TCP but rejects publish | Current endpoint reports terminal rejection without retry storm | Only rejected destination stops; others remain active | Device |
| N12 | One destination socket loss | Drop traffic to one destination only | Not applicable because only one endpoint exists | Failed destination enters RECONNECTING; healthy destinations remain LIVE | Device |
| N13 | Low-bandwidth cellular | Throttle or move to weak signal while live | Bitrate telemetry may degrade and adaptive bitrate may step down; no capture teardown | Aggregate budget prevents starting an unsustainable fan-out | Device |
| N14 | Process/service interruption | Stop/restart the app service during a network handoff | No stale reconnect callback; user must explicitly prepare/start again | Each destination session is restored only by an explicit policy | Automated + Device |
| N15 | Manual Stop during pending reconnect | Trigger Stop immediately after network loss | All delayed reconnect callbacks are cancelled; state reaches STOPPED | All destination sessions stop and no session restarts | Automated + Device |
| N16 | Infinix X6853 live-preview isolation | Run N03, N05, and N07 on the user's Infinix X6853 | Preview may pause after LIVE, but encoder and reconnect path must remain stable | Same capture isolation rule, without destination fan-out coupling | Device-required |

## Automated evidence to run in the sandbox

The sandbox can prove the following without a physical phone: failure classification, retryability, backoff bounds, legal lifecycle transitions, stale callback rejection at the state-machine boundary, endpoint preflight rejection, and the existing 47-check feature smoke suite. It cannot prove that a cellular handoff preserves RootEncoder's encoder, EGL state, socket, audio capture, or server-side publishing.

The following commands are the required automated gate:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/home/ubuntu/android-sdk
export ANDROID_SDK_ROOT=/home/ubuntu/android-sdk
export PATH=/home/ubuntu/android-sdk/platform-tools:/home/ubuntu/android-sdk/cmdline-tools/latest/bin:$PATH
./gradlew lintDebug testDebugUnitTest assembleDebug --no-daemon --max-workers=1 -Dorg.gradle.jvmargs='-Xmx1024m -Dfile.encoding=UTF-8'
python3 tools/feature_smoke_test.py
```

## Device protocol for actual radio-handoff validation

Use the Infinix X6853 running Android 15, because this is the device on which the graphics failure was physically reproduced and later stabilized. Before each run, start with a clean app process, confirm microphone and capture permissions, select a known-good endpoint, and record the selected profile, network transport, battery percentage, thermal state, and destination-side ingest status. Do not rotate the app or change capture source during the handoff run; those are separate variables.

For every interruption, capture timestamps for the last server-side video packet, the moment the transport changes, the first `RECONNECTING` state, each retry, the first packet after recovery, and the final session state. Export Unictoos diagnostics and the destination-side ingest log together. A run passes only if there is no graphics-resource failure, no duplicate active publish session, no microphone loss, no stale callback after Stop, and no unbounded reconnect loop.

## Current certification status

| Area | Status | Reason |
|---|---|---|
| Single-destination failure-policy unit coverage | **Passed** | Covered by `testDebugUnitTest`; retryable network/timeout classification and bounded backoff passed |
| Single-destination lifecycle/build regression | **Passed** | `lintDebug`, `testDebugUnitTest`, `assembleDebug`, and 47/47 smoke checks passed |
| Wi-Fi/cellular radio handoff | **Not executed** | `adb devices -l` found no connected physical Android device or emulator |
| True simultaneous multistreaming | Not testable in v0.2.3 | The feature is not implemented |
| Destination-isolated failure behavior | Not testable in v0.2.3 | No per-destination session model exists |
| Infinix X6853 network-switch validation | **Device-required / pending** | Must be run on the user's handset with Wi-Fi and cellular controls |

## Guardrails for the future multistream implementation

A future multistream task must add a provider-neutral destination session model rather than reusing one global endpoint and one global status. Each destination needs its own connection generation, state, retry budget, authentication result, bitrate telemetry, and terminal failure action. The service must also calculate aggregate upload demand and battery impact before starting, define whether adaptive bitrate is shared or per destination, and preserve the existing credential encryption and authentication semantics.
