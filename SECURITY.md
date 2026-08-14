# Security policy

## Reporting a vulnerability

Please do not publish stream keys, OAuth tokens, private ingest URLs, or exploitable security details in a public issue. Until a dedicated private security contact is configured for the repository, open a private GitHub security advisory when available or contact the repository maintainers through the account’s documented private channel.

Include the affected version, Android version/device, reproducible steps, impact, and whether any credential or private broadcast data may have been exposed. Redact all secrets from screenshots and logs.

## Security expectations

Unictoos must not log stream keys or OAuth tokens, include credentials in scene exports, transmit telemetry by default, or bypass Android’s user-consent flow for screen capture. Changes touching MediaProjection, foreground services, network transport, credential storage, exported components, or third-party dependencies require a security-focused review.

## Supported versions

The project’s initial supported range is Android 10 or later. Release notes will identify versions that are experimental, unsupported, or affected by platform-specific capture/encoder limitations.
