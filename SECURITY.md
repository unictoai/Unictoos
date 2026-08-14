# Security policy

## Reporting a vulnerability

Please do not publish stream keys, OAuth tokens, private ingest URLs, or exploitable security details in a public issue. Until a dedicated private security contact is configured for the repository, open a private GitHub security advisory when available or contact the repository maintainers through the account’s documented private channel.

Include the affected version, Android version/device, reproducible steps, impact, and whether any credential or private broadcast data may have been exposed. Redact all secrets from screenshots and logs.

## Security expectations

Unictoos must not log stream keys, OAuth access tokens, or OAuth refresh tokens; include credentials in scene exports; transmit telemetry by default; or bypass Android’s user-consent flow for screen capture. Stream-key storage and platform OAuth storage are separate concerns. OAuth integrations must use the minimum scopes necessary, prefer PKCE or a secure backend relay, support disconnect/revocation, and expose a clear connected-account state to the user. Chat, alerts, moderation, and event payloads must not be sent to the encoder unless a creator explicitly turns on a scene source that requires them. Ad providers must never receive stream keys, ingest URLs, OAuth refresh tokens, microphone samples, or raw screen frames. Changes touching MediaProjection, foreground services, network transport, credential storage, exported components, platform integrations, or third-party dependencies require a security-focused review.

## Integration boundaries

The open-source Android client can own capture, encoding, local scenes, recording, and user-visible controls. Platform APIs that need OAuth, webhooks, client secrets, moderation scopes, chat relays, or persistent event subscriptions should be implemented as explicit provider adapters with a documented backend or PKCE strategy. A generic RTMP stream key does not grant those capabilities.

## Supported versions

The project’s initial supported range is Android 10 or later. Release notes will identify versions that are experimental, unsupported, or affected by platform-specific capture/encoder limitations.
