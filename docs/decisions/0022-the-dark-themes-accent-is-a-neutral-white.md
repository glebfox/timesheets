# 0022 The dark theme's accent is a neutral white

Status: accepted. Supersedes [0021](0021-the-dashboards-charts-color-statuses-as-the-app-does-in-one-color-for-both-themes.md) for the colors of Over and To go.

## Context

The light theme's accent is the brand's blue, `#0256A9`, `oklch(0.46 0.15 255)`. The dark theme's accent was a purple, `oklch(0.59 0.2 277)`, which is not a color of the brand. The brand restyling (issue #93) gave the dark theme a black background.

Alternatives considered and rejected:

- **The purple.** It is not the brand's, and beside the brand's blue on the login page's sheet it reads as a second identity.
- **The brand's blue.** It is dark: about 2.9:1 against black, so it gets lost on the dark theme (decided by the user).
- **The brand's hue lifted for dark surfaces, `oklch(0.59 0.15 255)`.** A monochrome dark theme looks more contrasting (decided by the user).

## Decision

The dark theme's accent is Aura's neutral, `--aura-neutral-dark`, a near white, so the dark theme is monochrome: a primary button and the main menu's current item are white with dark text, and a focus ring and a selection are white. What needs a color of its own in the dark theme takes the palette's blue, `--aura-blue`, the brand's hue at the palette's lightness: a badge with no color of its own, and the dashboard's Over and To go, which are one literal for both themes, and white can be no fill on the light card. Decided by the user on 2026-11-15 (issue #93).

## Consequences

- Wherever the app colors something with the accent, it is white in the dark theme: the calendar's Over figures and the name of a worked holiday read white rather than as a color, and the login page's button is white beside the blue sheet.
- A badge with no color of its own would take the neutral gray of a neutral badge, so `component/badge.css` gives it `--aura-blue` in the dark theme.
- The dashboard's Over and To go no longer take the accent, as 0021 had them: they are `--aura-blue` and that blue at half strength, so in the light theme the chart's Over is a lighter blue than the calendar's.
