# Unictoos Accessibility Audit

## Contrast

The existing `TextMuted` token is `#A6ADB7`. A WCAG relative-luminance calculation against the current graphite surfaces produced the following ratios:

| Foreground | Background | Contrast ratio | Result for body text |
|---|---|---:|---|
| `#A6ADB7` | `#191D22` (`Surface`) | 7.48:1 | Passes WCAG AA |
| `#A6ADB7` | `#24292F` (`SurfaceRaised`) | 6.48:1 | Passes WCAG AA |

The token does not need to be changed for the current dark surfaces. Future palette changes should rerun the same calculation before reducing this contrast.

## Icon-only controls

The UI audit found one actual icon-only action control in the shared scene card: its arrow icon already has the content description **“Open studio”**. Studio mute, record, stop, practice, marker, and scene controls include visible text labels, so their icons intentionally do not duplicate the spoken label. Informational icons in status, trust, and empty-state rows are decorative and are kept out of the accessibility tree to avoid duplicate announcements.

The stronger C1 LIVE badge now includes a visible and animated status dot alongside explicit text. The C3 scene thumbnail remains a visual cue; the scene name, ratio, source count, and source-type chips continue to provide the textual explanation.
