# Unictoos v0.2 A1 design-token proposal

## Decision requested

This proposal defines the visual foundation for v0.2. The implementation will not change any screen until this direction is approved. The goal is a restrained, professional creator tool: neutral surfaces, one purposeful accent, quiet data presentation, and motion that settles quickly without decorative overshoot.

## Before

Alpha18 currently uses a near-black base with several competing accents: violet, magenta, cyan, mint, and amber. Those colors are useful in isolated places, but their broad use makes actions, telemetry, source types, and decoration compete for attention. The current hierarchy is therefore often communicated by saturation rather than spacing, scale, and typography.

## Proposed palette

The proposal uses a cool neutral scale and one confident blue accent. The blue is reserved for primary actions, selected states, and the live/recording indicator. Destructive and error states use a separate red. Amber is retained only for a genuinely cautionary condition such as thermal warning or an in-progress transition; it is not used as a general decorative accent. Cyan, mint, magenta, and violet would no longer be general-purpose screen colors.

| Token | Proposed value | Intended role |
|---|---:|---|
| `Neutral950` | `#0B0D0F` | App background and deepest surface |
| `Neutral900` | `#111418` | Primary content surface |
| `Neutral850` | `#171B20` | Raised cards and panels |
| `Neutral800` | `#1E242A` | Focused/selected neutral surface |
| `Neutral700` | `#2A323A` | Dividers and quiet borders |
| `Neutral500` | `#6E7883` | Secondary labels and metadata |
| `Neutral300` | `#AEB7C1` | Quiet but readable body text |
| `Neutral100` | `#F1F4F7` | Primary text and high-contrast content |
| `AccentBlue` | `#5B8DEF` | Primary action, selection, live/recording indicator |
| `AccentBluePressed` | `#4677D6` | Pressed/engaged accent state |
| `Danger` | `#E05A64` | Destructive action and error only |
| `Caution` | `#C9953B` | Thermal or transition warning only |
| `OnAccent` | `#FFFFFF` | Text and icons on the blue action |

The accent blue should appear sparingly. A neutral card containing a single blue primary action is preferred to a screen with multiple colored cards. Functional source-type differentiation in Scenes may remain, but the colors should be muted and secondary to the enabled/selected state.

## Typography

The existing typography already has a useful bold headline direction. v0.2 should tighten the contrast rather than introduce a new font dependency: display and headline styles remain bold, title styles use semibold only for clear hierarchy, body styles remain regular and quiet, and labels use small caps-like sizing with stronger letter spacing. Health metrics should use the largest available numeric emphasis with stable alignment; no external typography dependency is proposed.

## Spacing tokens

The implementation will establish named 4-point-based spacing tokens. Existing screens contain ad-hoc values and will not all be migrated in the token-definition commit; the migration debt will be recorded and handled screen by screen.

| Token | Value |
|---|---:|
| `Spacing.xs` | `4.dp` |
| `Spacing.sm` | `8.dp` |
| `Spacing.md` | `12.dp` |
| `Spacing.lg` | `16.dp` |
| `Spacing.xl` | `24.dp` |
| `Spacing.xxl` | `32.dp` |
| `Spacing.section` | `40.dp` |

## Motion tokens

Motion will use named, non-bouncy timing constants. `MotionTokens.quick` will be approximately 120 ms for press feedback, `MotionTokens.standard` approximately 200 ms for tab and card transitions, and `MotionTokens.emphasis` approximately 300 ms for high-salience state changes. The live indicator will use a slow opacity-only breathing animation; it will not scale, glow, or create a halo.

## Screen hierarchy after approval

Studio will receive the first application of the token set. The preview will dominate the vertical layout, health metrics will become quiet tabular data separated primarily by whitespace, and Go Live/Stop will be the only visually confident action. Home, Scenes, Engage, Library, Settings, More, and shared components will then migrate in the requested order, each as a separate commit with the required validation commands.

## Approval gate

Please approve this token direction, request changes to the palette, or select a different accent color before A2 implementation begins. No screen-level visual changes have been made for v0.2 yet.
