# 0021 The dashboard's charts color statuses as the app does, in one color for both themes

Status: accepted

## Context

A chart draws on a canvas and takes literal colors. When the theme switches, the add-on redraws a chart in ECharts' own light or dark theme, but the colors set in its options stay. The app's status colors differ between the themes. ECharts lightens a fill under the pointer with a color parser that reads hex, `rgb()`, `hsl()`, and named colors only, and a fill it cannot read, such as `oklch()`, vanishes while hovered.

Alternatives considered and rejected:

- **Colors that follow the theme switch.** It needs a client-side mechanism of the project's own, for little gain.
- **A color per status chosen for at least 3:1 against both cards (WCAG 1.4.11), as first built.** The charts then differ from the colors the calendar and the approval screen give the same statuses.
- **Short in the orange of the calendar's week summary.** The red of a short day makes a shortfall stand out.

## Decision

Each status takes the app's color for it, the dark theme's value where the token differs between themes, as one literal for both themes. A fill is written in hex or `rgba()`; a line or a label may be `oklch()`. Short is the calendar's short-day red; New, the warning yellow, and To go, the accent at half strength, fall below 3:1 against the light card, and the legend and the tooltip name them. Decided by the user on 2026-11-14 (issue #69).

## Consequences

- A change of a status color in the theme does not reach the charts; `DashboardColors` is changed by hand with it.
- What a chart leaves to ECharts' theme, its axis labels and grid lines, follows the light and dark theme.
