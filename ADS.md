# Unictoos advertising architecture

Unictoos treats advertising as an optional app-surface feature, not as part of the creator’s broadcast pipeline. The current alpha includes a provider-neutral `AdProvider` contract, a persisted opt-in policy, and an app-only sponsor-space component. The provider is intentionally a no-op until a production ad network, privacy disclosures, age/region handling, consent flow, and release configuration are reviewed.

The following boundaries are non-negotiable:

| Boundary | Policy |
|---|---|
| Outgoing stream | Ads must never be inserted into RTMP/RTMPS frames by the app’s normal ad system |
| Local recording | Ads must never be burned into recordings by the normal ad system |
| Live session | Ads must never interrupt or cover the Studio controls during an active broadcast |
| Credentials | Ad providers must never receive stream keys, ingest URLs, OAuth tokens, or microphone/capture data |
| Development builds | Development and test builds use the no-op provider |
| User control | Ads require a visible opt-in/preference and should be frequency-limited |

Creator-controlled sponsor graphics are a separate scene-source feature. If implemented, they must be explicitly added, previewed, and included in the creator’s outgoing scene by choice; they are not the same as app monetization.
