# Design System

> Theme and visual standards. Reference this when building or reviewing UI. How to apply styles in Jmix views (class names, theme variants, inline styles) is the `jmix-style-ui` skill.

---

## 1. Theme

- **Base theme:** Vaadin Aura, with the Jmix Aura additions.
- **Custom CSS:** `src/main/resources/META-INF/resources/themes/timesheets-aura/styles.css`. It imports the project tokens (`tokens.css`), the accent remapping (`color.css`), and per-view, per-component, and app-wide files from `view/`, `component/`, and `app/`.

**Aura and Lumo are two different, incompatible design systems.** This project uses **Aura**. Do not use `--lumo-*` CSS variables — they belong to the Lumo theme and must not be mixed with Aura. Use `--aura-*` variables for Aura-specific properties (typography, shadows) and `--vaadin-*` variables for base properties shared across all themes (spacing, radius, colors).

Lumo's utility classes, such as `text-secondary`, `text-s`, `font-bold`, or `p-m`, do not exist here either: neither Aura layer defines utility classes, so such a class does nothing and nothing reports it. The project's own utility classes are in `app/util.css`; where Java code may set them: `docs/conventions.md`.

**Always use Aura theme variables instead of hard-coded values** (e.g., `--aura-font-size-xs` through `--aura-font-size-xl` for font sizes). Do not use hardcoded `px`, `rem`, or `em` values when an Aura variable exists. This ensures consistency with the Vaadin Aura theme and allows global adjustments through theme customization. Some existing theme files predate this rule and still hard-code sizes.

Project-specific values are `--ts-*` custom properties defined in `tokens.css`.

---

## 2. Color Palette

Aura computes all color variations automatically from a small set of base properties. Override these instead of hard-coding hex values.

| Token | Default | Usage |
|-------|---------|-------|
| `--aura-accent-color-light` | Blue | Primary actions, focus rings, selection highlights (light mode) |
| `--aura-accent-color-dark` | Blue | Primary actions, focus rings, selection highlights (dark mode) |
| `--aura-neutral` / `-light` / `-dark` | Dark gray / off-white | Text, borders, default UI chrome |
| `--aura-red` | Red | Error states, destructive actions |
| `--aura-orange` | Orange | Warnings |
| `--aura-green` | Green | Success states, confirmations |
| `--aura-blue` | Blue | Informational, links |
| `--aura-yellow` | Yellow | Caution, highlights |
| `--aura-purple` | Purple | Decorative accents |

Derived read-only tokens (do not override directly):
- `--aura-accent-contrast-color` — high-contrast text on accent backgrounds
- `--aura-accent-text-color` — accent-derived text color with good contrast
- `--aura-accent-border-color` — border tinted with accent color
- `--aura-accent-surface` — surface tinted with accent color
- `--aura-red-text`, `--aura-green-text`, etc. — palette text variants with better contrast

Base style tokens (shared across all themes):
- `--vaadin-text-color` — main text color
- `--vaadin-text-color-secondary` — secondary/muted text
- `--vaadin-text-color-disabled` — disabled state text
- `--vaadin-border-color` — prominent borders (3:1 contrast)
- `--vaadin-border-color-secondary` — subtle, non-essential borders
- `--vaadin-background-color` — base content background
- `--vaadin-background-container` — buttons, toolbars, highlighted areas
- `--vaadin-background-container-strong` — more prominent container background

Use accent class names (e.g. `.aura-accent-purple`) on `<html>` or individual components to swap accent color contextually.

**Project colors** (defined in `tokens.css`, each with `-light` and `-dark` variants). `tokens.css` makes the primary color the app-wide accent, and `color.css` swaps the accent to the others by context:

| Context | Theme names | Classes | Accent color |
|---------|-------------|---------|--------------|
| Primary (accent): primary actions, focus rings, selection highlights | — | — | `--ts-primary-color` |
| Error | `error`, `danger` | `.aura-accent-red`, `.v-error` | `--ts-danger-color` |
| Success | `success` | `.aura-accent-green`, `.v-success` | `--ts-success-color` |
| Warning | `warning` | `.aura-accent-yellow`, `.v-warning` | `--ts-warning-color` |
| Info | `info` | `.aura-accent-blue`, `.v-info` | `--ts-primary-color` |

Reach these colors through the theme names and classes rather than through the `--ts-*` tokens directly. The exception is a project CSS rule that colors a single property of the app's own element, such as a text color, a border, or a progress bar's fill: it uses the `--ts-*` token, because an accent class would recolor everything accent-based inside the element. There is no project orange or neutral; such rules use `--aura-orange` and `--aura-neutral`.

**Badges** are Vaadin's `Badge` component, not a `span` with `theme="badge"`: that look comes from a stopgap stylesheet in the Jmix Aura layer, marked for removal now that the component exists. Aura styles the badge's `success`, `warning`, `error`, `filled`, and `small` variants, but not `contrast`, which compiles and changes nothing; a neutral badge takes the `.aura-accent-neutral` class instead. `BadgeStyle` in `data/renderer/` applies these.

**Toggle buttons** are a checkbox group or a radio button group with a class that makes each item read as a button, a checked one filled with the accent color, while the keyboard works as for the group's checkboxes or radio buttons:

- `toggle-group` (`component/toggle-group.css`), on a checkbox group, as the work schedule editor's workdays: a row of separate buttons, each pressed on its own.
- `segmented-group` (`component/segmented-group.css`), on a radio button group, as a holiday calendar day's type: one bar of joined segments, since only one item can be chosen.

Both show the states of an Aura field. Invalid, the borders take the error color. Read-only, the borders turn dashed and only a checked item keeps a faint accent tint, with a solid outline in the accent text color and a semibold label, so it reads as checked by more than its hue. Disabled, the labels dim and a checked item keeps a stronger fill. Hovered and pressed, an item shades as an Aura button does, an unchecked one as a regular button and a checked one as a primary button; a pressed toggle button also drops its shadow and, on a high-density screen, shrinks slightly, while a segment keeps its size, as Aura's joined menu bar buttons do. A read-only or disabled group shows no hover or press. In forced-colors (high contrast) mode the chosen items use the system's selected-item colors, and a read-only chosen item keeps a doubled solid border and its semibold label.

---

## 3. Typography

This project keeps Aura's default font, **Instrument Sans**, which the Aura theme bundles; it sets no font of its own.

| Token | Purpose |
|-------|---------|
| `--aura-font-family` | App-wide font family (set on `<body>`) |
| `--aura-base-font-size` | Base size (unitless number, represents M size in px) |
| `--aura-font-size-xs` through `-xl` | Computed font sizes (rem, rounded to nearest px) |
| `--aura-base-line-height` | Base line height (unitless, relative to font size) |
| `--aura-line-height-xs` through `-xl` | Computed line heights (rem, rounded to nearest 2px) |
| `--aura-font-weight-regular` | Normal body text |
| `--aura-font-weight-medium` | Emphasis, subheadings |
| `--aura-font-weight-semibold` | Headings, strong emphasis |
| `--aura-font-smoothing` | Set to `auto` to disable grayscale anti-aliasing |

Use Aura font-size tokens (`--aura-font-size-s`, etc.) instead of hard-coded `px`/`rem` values.

---

## 4. Spacing & Layout

Aura computes gap and padding from `--aura-base-size` (unitless, range 12–24). Use the resulting base style tokens:

| Token | Purpose |
|-------|---------|
| `--vaadin-gap-xs` through `-xl` | Space between elements in flex/grid layouts |
| `--vaadin-padding-xs` through `-xl` | Internal padding for containers and content areas |
| `--vaadin-padding-inline-container` | Horizontal padding for single-line containers (buttons, inputs) |
| `--vaadin-padding-block-container` | Vertical padding for single-line containers |

**Border radius** (computed from `--aura-base-radius`, unitless, range 0–10):

| Token | Purpose |
|-------|---------|
| `--vaadin-radius-s` | Small controls (should not become circles) |
| `--vaadin-radius-m` | Default component radius |
| `--vaadin-radius-l` | Large containers, cards, dialogs |

**Shadows** (Aura-specific):

| Token | Purpose |
|-------|---------|
| `--aura-shadow-xs` | Subtle elevation — buttons, inputs, checkboxes |
| `--aura-shadow-s` | Slight elevation — primary buttons, selected controls, cards |
| `--aura-shadow-m` | Clear elevation — overlays, notifications, dialogs |

**Surface colors** for visual hierarchy (read-only, computed):
- `--aura-surface-color` — semi-transparent elevated background
- `--aura-surface-color-solid` — opaque version
- Control with `--aura-surface-level` (number, higher = more elevation) and `--aura-surface-opacity` (default 0.5)

**Layout approach:** Use `vbox` / `hbox` in view descriptors (Vaadin `VerticalLayout` / `HorizontalLayout`), or flexbox/grid in CSS, with `--vaadin-gap-*` / `--vaadin-padding-*` tokens. No hard-coded spacing values.

---

## 5. Responsive Behavior

- **Mobile** (< 640px): Single column, stacked layouts, full-width cards
- **Tablet** (640–1024px): Two-column grid, side-by-side content
- **Desktop** (> 1024px): Multi-column grid
