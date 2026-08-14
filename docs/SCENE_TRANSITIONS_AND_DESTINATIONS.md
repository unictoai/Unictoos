# Scene Transitions and Destination Boundaries

## Scene transitions

The current streaming path has one active RootEncoder video source at a time. It can replace a source, but it does not expose a compositor timeline or a second render graph that can cross-fade two complete scenes. A reliable fade therefore remains a future compositor task rather than a cosmetic UI toggle.

A cut transition is safe only at the scene-management boundary: the app can persist a new selected scene and prepare its source before a future session, but switching live scenes would require coordinated source teardown and replacement. The current release intentionally keeps scene selection outside the active stream lifecycle so it cannot interrupt a broadcast unexpectedly.

## Multi-destination streaming

The current service owns one `GenericStream` connection and one endpoint. Simultaneous YouTube, Twitch, and Kick delivery is not implemented. Supporting it requires either provider-side restreaming or one encoder/RTMP client per destination, with multiplied upload, CPU, battery, reconnect, and auth state. The existing `CredentialRepository` and reconnect/auth-error behavior should not be changed as a side effect of a future multi-destination task.

A future implementation should first add a provider-neutral destination session model, per-destination status, independent error policy, aggregate health reporting, bandwidth budgeting, and an explicit device-capability preflight. It must also define whether recording follows the primary destination or the composed local output.

## External cameras and capture cards

The current `Camera2Source` path targets Android camera devices exposed through the platform camera stack. USB webcams and capture cards are not assumed to be available through that same path. A future external-input task should identify Android USB host/device support, permission lifecycle, UVC format negotiation, frame conversion, orientation, and device-specific testing before adding a source type.
