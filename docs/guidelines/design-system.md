# Design System

> Theme and visual standards. Reference this when building or reviewing UI. How to apply styles in Jmix views (class names, theme variants, inline styles) is the `jmix-style-ui` skill.

---

## 1. Theme

- **Base theme:** Vaadin Aura, with the Jmix Aura additions.
- **Custom CSS:** `src/main/resources/META-INF/resources/themes/timesheets-aura/styles.css`. It imports the project tokens (`tokens.css`) and per-view, per-component, and app-wide files from `view/`, `component/`, and `app/`.

**Aura and Lumo are two different, incompatible design systems.** This project uses **Aura**. Do not use `--lumo-*` CSS variables — they belong to the Lumo theme and must not be mixed with Aura. Use `--aura-*` variables for Aura-specific properties (typography, shadows) and `--vaadin-*` variables for base properties shared across all themes (spacing, radius, colors).

Lumo's utility classes, such as `text-secondary`, `text-s`, `font-bold`, or `p-m`, do not exist here either: neither Aura layer defines utility classes, so such a class does nothing and nothing reports it. The project's own utility classes are in `app/util.css`; where Java code may set them: `docs/conventions.md`.

**Always use Aura theme variables instead of hard-coded values** (e.g., `--aura-font-size-xs` through `--aura-font-size-xl` for font sizes). Do not use hardcoded `px`, `rem`, or `em` values when an Aura variable exists. This ensures consistency with the Vaadin Aura theme and allows global adjustments through theme customization. Some existing theme files predate this rule and still hard-code sizes.

Project-specific values are `--ts-*` custom properties defined in `tokens.css`.

**Light and dark.** The theme picked with the user menu's theme switch, Light, Dark, or System, which follows the system's setting, reaches the stylesheets through the root element: Jmix's `color-scheme-switching-support.js`, which `TimesheetsApplication` loads, sets `color-scheme` and a `theme` attribute of `light` or `dark` on `<html>`. Aura's own colors switch on `color-scheme`, and so does a project color that differs between the themes, with `light-dark()`, as the backdrop's colors in `tokens.css` do; a status color has one value for both themes (below). An image cannot follow `color-scheme`, so the drawer's logo is the brand's SVG put inline with Jmix's `svg` element, and `view/main-view.css` colors its parts with `light-dark()`: the generator marks the strips and the lettering `ink` and the mint corner `fold`, and a fill set by a stylesheet wins over the file's own `fill` attribute. Nothing keys on the `theme` attribute that the same script sets for Lumo. A `prefers-color-scheme` media query sees only the system's setting and misses a theme the user picked.

---

## 2. Color Palette

Aura computes all color variations automatically from a small set of base properties, and `tokens.css` sets them to the project's colors instead of hard-coding colors in rules:

| Token | Project value | Usage |
|-------|---------------|-------|
| `--aura-accent-color-light` | `--ts-brand-blue-color`, the brand's blue | Primary actions, focus rings, selection highlights (light mode) |
| `--aura-accent-color-dark` | Aura's neutral, a near white: the dark theme is monochrome ([decision 0022](../decisions/0022-the-dark-themes-accent-is-a-neutral-white.md)) | Primary actions, focus rings, selection highlights (dark mode) |
| `--aura-background-color-light` / `-dark` | White / black | The work area's card; the drawer lies on the backdrop (below) |
| `--aura-contrast-level` | 2 | Text and border contrast |
| `--aura-neutral` / `-light` / `-dark` | Aura's, derived from the background | Text, borders, default UI chrome |
| `--aura-red` | `--ts-danger-color` | Error states, destructive actions |
| `--aura-green` | `--ts-success-color` | Success states, confirmations |
| `--aura-yellow` | `--ts-warning-color`, an amber | Caution, highlights |
| `--aura-blue` | The brand's hue at the palette's lightness | Informational; a badge with no color of its own in the dark theme; the dashboard's Over and To go, as literals in `DashboardColors` |
| `--aura-orange` | One orange for both themes | The calendar's short week |
| `--aura-purple` | One purple for both themes | The calendar's absences |

Every Aura and Jmix rule that colors by status reads this palette: the theme names, the accent classes, the badges, and the `--aura-*-text` colors, so the project's colors reach them all. Each palette color is one value for both themes, as Aura's own are, in the middle of the lightness range, except the warning's light amber, so a fill shows on the white and on the black background alike; Aura derives from it, with relative color syntax, `oklch(from …)`, a text color and a tint for each theme, switching on `color-scheme`. The tokens sit on `html`, not `:where(:root)`: Aura sets `--aura-app-background` inside an `@scope (:root)` rule, and a scoped rule wins over an unscoped one of the same specificity, so a rule without specificity loses to it.

**Backdrop.** The drawer and the inset around the work area show `--aura-app-background`, the project's backdrop in `tokens.css`: soft spots of color on a calm base, as light falling through, rather than one gradient across the window, each fading from a core through a softer ring to nothing. The backdrop is fixed to the window, so the spots sit at the left, where the drawer shows them. The light theme is frosted glass: the brand's mint behind the logo, its blue in the middle of the drawer, and a faint mint at the bottom. The dark theme, where no glass shows, is a fire behind the app: the top stays dark behind the ivory logo, an ember barely shows in the middle, and amber rises from the bottom left with a deeper ember ring. One geometry serves both themes; each spot's colors are a `light-dark()` pair. Aura paints the work area's card as a translucent surface over that same property, so `view/main-view.css` sets it back to the plain background color inside the card; otherwise the gradient tints the content.

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

**Project colors** (defined in `tokens.css`, each one value for both themes). The danger and the success color share the palette blue's lightness; the warning is a light amber, since Aura darkens a color for its text and a lemon yellow darkens to olive. Aura's own rules swap the accent to a palette color by context, and the palette carries the project's colors:

| Context | Theme names | Classes | Accent color |
|---------|-------------|---------|--------------|
| Primary (accent): primary actions, focus rings, selection highlights | — | — | `--ts-brand-blue-color` in the light theme, Aura's neutral in the dark one |
| Error | `error`, `danger` | `.aura-accent-red`, `.v-error` | `--ts-danger-color` |
| Success | `success` | `.aura-accent-green`, `.v-success` | `--ts-success-color` |
| Warning | `warning` | `.aura-accent-yellow`, `.v-warning` | `--ts-warning-color` |
| Info | `info` | `.aura-accent-blue`, `.v-info` | `--aura-blue` |

Reach these colors through the theme names and classes rather than through the `--ts-*` tokens directly. The exception is a project CSS rule that colors a single property of the app's own element, such as a border, a dot, or a progress bar's fill: it uses the `--ts-*` token, because an accent class would recolor everything accent-based inside the element. Text in a status color takes Aura's `--aura-red-text`, `--aura-green-text`, and so on instead: the token is a mid tone, too light for text on white and too dark on black, and the `-text` color is the one Aura darkens or lightens for the theme. There is no `--ts-*` orange, purple, or neutral: `tokens.css` sets `--aura-orange` and `--aura-purple` directly, and rules use those and `--aura-neutral`, except a neutral fill shown beside the status colors, such as Closed on the approval screen: `--aura-neutral` is the text color itself, nearly black in the light theme and nearly white in the dark one, so such a fill takes the mid gray of `--vaadin-text-color-secondary`, which Aura derives from it.

**Badges** are Vaadin's `Badge` component, not a `span` with `theme="badge"`: that look comes from a stopgap stylesheet in the Jmix Aura layer, marked for removal now that the component exists. Aura styles the badge's `success`, `warning`, `error`, `filled`, and `small` variants, but not `contrast`, which compiles and changes nothing; a neutral badge takes the `.aura-accent-neutral` class instead. `BadgeStyle` in `data/renderer/` applies these. `component/badge.css` restyles them in two ways. A badge without a fill shows a fixed tint of its color, a border a step stronger, and text in its color: Aura scales the tint's opacity by the color's chroma and mixes it into the surface at half strength, so on the white surface a gray or a light color such as the warning shows hardly any tint, and the badges look alike. And a badge with no color of its own, a default or a filled one such as a tag or a project role, is blue in both themes: the accent, the brand's blue, in the light theme, and `--aura-blue` in the dark one, whose accent is a neutral white that would make it the gray of a neutral badge.

**Buttons.** `component/button.css` makes a neutral button flat, with the body text color and the fields' border; a button with a color, by its theme name or an accent class, and a primary one keep Aura's colors.

**Toggle buttons** are a checkbox group or a radio button group with a class that makes each item read as a button, or as a tab of a filled tab bar, a checked one filled with the accent color, while the keyboard works as for the group's checkboxes or radio buttons:

- `toggle-group` (`component/toggle-group.css`), on a checkbox group, as the work schedule editor's workdays: a row of separate buttons, each pressed on its own.
- `segmented-group` (`component/segmented-group.css`), on a radio button group, as a holiday calendar day's type and the approval screen's Week | Month and roster switches: one control, since only one item can be chosen, drawn as Aura draws its tabs in the `filled` variant, from the same tokens: a gray track without a border holds the segments, each sized as a tab, as tall as a button, and the chosen segment is a pill filled with the accent color, while an unchosen label is muted, as an unselected tab's is, and takes the full text color on hover. So the whole control is taller than a button by the track's padding, as Aura's tabs are, and a row that puts buttons or fields beside it centers its items, as the approval screen's toolbar does, so that they line up on its middle. With no border to the track, the invalid and read-only states draw theirs as a line along its inner edge, which takes no room. The segments are of equal width, each at least as wide as the longest label, and a label never wraps. The focus ring is drawn as on Aura's tabs: inside an unchosen segment, and outside the chosen pill, within the track's padding.

Both show the states of an Aura field. Invalid, the borders take the error color. Read-only, the borders turn dashed and only a checked item keeps a faint accent tint, with a solid outline in the accent text color and a semibold label, so it reads as checked by more than its hue. Disabled, the labels dim and a checked item keeps a stronger fill. Hovered and pressed, an item shades as an Aura button does, an unchecked one as a regular button and a checked one as a primary button; a pressed toggle button also drops its shadow and, on a high-density screen, shrinks slightly, while a segment keeps its size, as Aura's joined menu bar buttons do. A read-only or disabled group shows no hover or press. In forced-colors (high contrast) mode the chosen items use the system's selected-item colors, and a read-only chosen item keeps a doubled solid border and its semibold label.

In a dialog, Tab stops on every option of a radio button group, a `segmented-group` included, rather than only on the checked one: the dialog's focus trap walks every focusable element and skips the browser's rule for radio groups, vaadin/web-components#8186. The project does not work around it.

**Selected items.** A selected item of the project's own, such as the approval screen's roster card and week card, is filled as a selected grid row, with `--vaadin-grid-row-selected-background-color`, and has no accent line along an edge or accent border: such a line merges with the focus ring.

**Forced colors.** Forced-colors (high contrast) mode drops background fills and box shadows, and draws a transparent border in the text color. So a card or row whose selection is only a fill, such as the approval screen's roster card and week card, marks it there as Vaadin's side nav marks its current item: the selected one has a border, or an outline where the part has no border all round, and text in `Highlight`, and a transparent border on the others takes `Canvas`. A system color that a rule sets explicitly survives the mode, so these rules need no `forced-color-adjust: none`. The mode stands for the user's own palette, each system color in its role, so the project's colors never come back in it: a part that is nothing but a color repeating what text says, such as the approval screen's status bars, dots, lines, and entry stripes, is left to disappear. A drawing whose shape tells what no text does, such as the roster's fingerprint, takes system colors, as Vaadin's progress bar draws its value in `CanvasText`; the mode leaves an SVG's `fill` as the author set it, so such a drawing sets it itself. An image, such as the login page's logo, keeps its own colors in the mode, so it is made transparent, keeping its place and its alternative text, and a pseudo-element over it draws its shape as a `mask` filled with `CanvasText`.

**Brand colors.** The login page's sheet and its fold keep the brand's colors in both themes, as the brand's guide asks of dark surfaces. The sheet takes the brand's blue, `#0256A9`, from `--ts-brand-blue-color`, which is also the light theme's accent, and the fold the brand's mint, `#87F1AC`, from `--ts-brand-mint-color`, both in `tokens.css`, which also holds the brand's ivory, `--ts-brand-ivory-color`, the color of the drawer's logo in the dark theme; the brand mark's path gives the fold only its shape, as a mask, since a color inside an SVG image is beyond the theme's reach.

**Charts.** A chart of the Charts add-on draws on a canvas and takes literal colors in its options, never the theme's custom properties, so it is the one place where colors are literals. When the theme switches, the add-on redraws the chart in ECharts' own light or dark theme, which colors its axis labels and grid lines, while the colors set in the options stay; each is one literal for both themes ([decision 0021](../decisions/0021-the-dashboards-charts-color-statuses-as-the-app-does-in-one-color-for-both-themes.md)). A fill, of a bar or a slice, is written in hex or `rgba()`: ECharts lightens a fill under the pointer through a color parser that reads no `oklch()`, and a fill it cannot read vanishes while hovered. A line's, a hatching's, or a label's color is drawn as written. See `DashboardColors`.

**Notation.** Colors are written in `oklch()` wherever what draws them reads it: the stylesheets, and a chart's lines and labels. Where it does not, a color keeps its notation, with a note beside it that says why and, where it matches a token, its `oklch()`: a chart's fill (above); the brand collection under `assets/brand/`, its copies, and `.idea/icon.svg`, all in hex, since CairoSVG, which draws the brand's PNG exports, paints a color it cannot read black (`scripts/brand/generate.py`); and the Jmix Studio palette icons under `meta/icon/component/`, in IntelliJ's own icon grays, which the IDE matches by their exact hex values to recolor an icon for its theme.

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

**Box sizing.** The theme leaves the project's own elements in `content-box`, so their padding and border add to a width set on them. A part given a share of its row, such as an approval figure at `flex-basis: calc(50% - var(--vaadin-gap-s))` for two to a row, sets `box-sizing: border-box`; otherwise its padding and border push the second part onto a row of its own.

**Focus rings at an edge.** A focus ring reaches outside its part by its width, `--vaadin-focus-ring-width`, plus its offset, which Aura sets to 2 pixels for a primary button, to minus the border's width, 1 pixel inward, for any other button, and to zero or less for most other parts. A container that clips its overflow, as one that scrolls does, cuts the ring off at the outer edge of its padding, so a container whose parts reach its edge, such as the approval screen's person panel with the person's primary button, pads by the ring's width plus 2 pixels.

---

## 5. Responsive Behavior

- **Narrow** (< 600px): one column, stacked layouts, full-width cards.
- **Medium** (600–1023px): two columns.
- **Wide** (1024px and more): three columns, as the dashboard's grid.

A container query styles the descendants of its container, never the container itself, so what changes under the query sits inside the container. As the approval figures go from four in a row to two, their row is the container, a wrapping flex container, and each figure changes its `flex-basis`; a grid that is itself the container cannot change its own columns.

A layout attribute in a descriptor, such as an `hbox`'s `alignItems` or a component's `width`, is an inline style on its element, and no stylesheet rule overrides it, a container query's included. A property that a query changes is set in the stylesheet, not in the descriptor, as the date range field sets how its two pickers stack in its own `component/date-range-field.css`.

A container's width at a common window is not the screen's. With the navigation drawer open at 1280 pixels, the approval screen's person panel is about 640 pixels wide, and a classic scrollbar, as on Windows or in Playwright's Chromium, takes 15 pixels of it where a Mac's overlay scrollbar takes none. A switch within a scrollbar's width of such a width puts the same window on either side of it, depending on the scrollbar.

---

## 6. Icons

Icons inside screens, on fields, buttons, and grids, come from the Gravity UI set (`gravity-ui:<name>`), and so do most of the framework's own; a grid's empty state is the exception, with an icon of the project's own set (below). `TsIcons` replaces Jmix's `Icons` bean and draws each icon that Jmix names by its purpose, such as `CREATE_ACTION` or `GENERIC_FILTER_SETTINGS`, as the Gravity UI counterpart of the Vaadin icon that Jmix's `jmix-font-icons.css` maps it to, or the nearest one where Gravity UI has none: a display for the system theme and `file-letter-x` for the Excel export. Save is the one icon chosen by its meaning: a floppy disk, where Jmix draws an archive box. Why counterparts, and why Cancel keeps `ban`: [decision 0013](../decisions/0013-the-framework-icons-are-gravity-ui-counterparts-of-jmix-ones.md). It lists the names of the framework and of the add-ons the project uses: the standard list, detail, and picker actions, a detail view's OK and Cancel, the generic and column filters, and the user menu. Any other name falls through to Jmix's icon, so an add-on brought in later, such as Reports, keeps its Vaadin icons until its names are added there. The project's own actions and dialog buttons that stand for a standard one, such as Add or a dialog view's OK, show the standard one's icon; asking that bean for it, `icons.get(JmixFontIcon.<NAME>)` in code or `icon="<NAME>"` in a descriptor, keeps them in step with it.

Three exceptions remain. Icons the framework creates without that bean keep Jmix's Vaadin icons: the buttons of the standard option and input dialogs (`DialogAction.Type`), the pagination, the arrow of the generic filter's Refresh button, which is a combo button, the twin column's buttons, and the close button on each tab of the main view. Icons the framework asks that bean for by a glyph's name rather than a purpose keep them too, such as those the framework's own screens set, like the Entity inspector's export and import: `TsIcons` hands such names on to Jmix, apart from the close button (`CLOSE_SMALL`) of dialog views, notifications, and pickers' dialogs and a dialog's maximize button (`EXPAND`, `COMPRESS`), which it draws too. And the avatar field's person placeholder is a Lucide drawing.

The project has a set of its own, drawn for the main menu and also used by the grids' empty states: each menu group and item has its own icon, and no two are the same, and each empty state shows the menu icon of the records it lists or, for a kind of record with no menu entry, such as tags and tag groups, an icon of its own. The rest of this section is about that set.

**The style.**

- Outline only, no fills. Lines have round caps and round joins, and the corners of rectangles are rounded.
- A 16 × 16 grid, `viewBox="0 0 16 16"`. The live area is 1..15, so the centers of the strokes stay within 1.75..14.25.
- One stroke width for the whole set, `--ts-icon-stroke-width` in `tokens.css`, 1.5 grid units, never mixed inside an icon or across the set. The menu draws its icons at 16 px, so one grid unit is one CSS pixel; an empty state draws them at `--aura-font-size-xl`, about 18 px.
- A dot is a zero-length line, `M x y h0`: a round dot of the stroke width.
- The set reads at 16 px on a high-density screen at strokes from 1.33 to 1.75. A thinner stroke only widens the gaps; a stroke above 1.75 needs the tight spots checked again: Time entries, Locks, and Work schedules.

**How the icons reach the screens.**

- The set is an iconset, `ts`, in `src/main/frontend/src/component/icon/ts-icons.js`, a module of the same form as `gravity-ui-icons.js` but written by hand: `scripts/icons/generate.py` builds only the sets that come from an npm package. A class in `component/icon/` carries its `@JsModule`, which brings the module into the frontend bundle. So the icons arrive with the bundle, before the menu or any screen is shown, and the bundle's file names carry a hash of their content, so a browser never keeps an old copy of the set after a deploy. Why an iconset of the project's own, and why one set serves the menu and the empty states: [decision 0016](../decisions/0016-one-iconset-of-the-projects-own-serves-the-menu-and-the-empty-states.md).
- Each `menu` and `item` in `menu.xml`, and each grid's `gridEmptyState`, names its icon with the `icon` attribute: `icon="ts:<id>"`.
- Each icon is a `<g id="ts:<id>" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round">`, and the shapes inside it set no `fill`, `stroke`, `stroke-width`, or `style` of their own. `vaadin-icon` draws a copy of that `<g>`, its `style` included, in a `0 0 16 16` view box given by the iconset's `size="16"`. `MenuIconsTest` checks that the set holds exactly the menu's icons and the ones only empty states use, which it lists, and `GridEmptyStateDescriptorsTest` that every empty state names an icon of the set.
- Each icon draws one `<path>`: its circles, ellipses, and rectangles are written as arcs and joined into it. A menu item's text color is translucent, and every element of an SVG is painted on its own, so where the lines of two elements touch or cross they would be painted twice and show darker in the light theme and brighter in the dark one; one path is painted once.
- The stroke width is the project's own property. Vaadin's `--vaadin-icon-stroke-width`, which Aura sets to 1.75 for every icon, is not set or overridden anywhere in the project, so changing `--ts-icon-stroke-width` changes every icon of the set, in the menu and in the empty states, and no other icon.
- The lines take the color Vaadin gives icons, `--vaadin-icon-color`, falling back to `currentColor`, so a selected or hovered item colors its icon as it colors its text.
- `component/icon.css` sets `--vaadin-icon-visual-size: 80%` on the `ts:` icons as on the Gravity UI icons, which draws them at 16 px inside a menu item's icon box; `component/grid-empty-state.css` gives an empty state's icon a box of `--aura-font-size-xl` and the full visual size.

**The rules for a new icon.**

1. **What an icon shows.** One object per screen, or, for an icon only empty states use, per kind of record: a small preview of what the screen shows, not of what the user does there. Few parts; small details do not survive 16 px.
2. **Two entries with one object.** When two entries would get the same object, the group, or the narrower or personal entry, gets the plain object, and the wider list gets rows plus the object: three rows on the left, one full and two short, and the object in the bottom right, in about 7..15. Time entries (beside My time entries), Row-level roles (beside the Security shield), Entity inspector (the magnifier alone is the search of filter fields), and Locks (beside Pessimistic locking) follow it.
3. **A family with a mark.** Entries that share a base object and differ in one aspect take the base object cut open at the bottom right and one mark in that corner, a zone of about 7.5 × 7.5: Work schedules, Holiday calendars, and Absences on the calendar. A mark is not placed inside the base object: inside the calendar there are about 2.5 units of free height, and a mark drawn there becomes a blot.
4. **Meanings are shared with the screens.** The menu never gives an object a meaning that contradicts what the same object means on screens. The meanings in use:
   - briefcase — a project (project fields; Projects);
   - clock — time spent or work time (time fields; My time entries, Time entries, Work schedules);
   - tag — a project role (the participant editor's Role field; Project roles), and a tag (the tag editor's name; the tags' empty states);
   - tags — a task type (the task editor's Type field; Task types), and a tag group (the tag editor; the tag groups' empty states);
   - shield — security roles (Security, Resource roles, Row-level roles);
   - magnifier — search and inspection (filter fields; Entity inspector);
   - rows — the list of all records of a kind (Time entries, Row-level roles, Entity inspector, Locks);
   - calendar — dates and days (date fields; Calendar, Work schedules, Holiday calendars, Absences);
   - umbrella — an absence (Absence types, Absences);
   - star — the default work schedule (the dialog that picks a new default); not available to the menu;
   - key — a password (the user editor); not available to the menu;
   - personnel card — personnel records (Administration);
   - person — a person (person fields; Project roles, Users).
5. **Geometry.** The grid, live area, and stroke of the style above. Between stroke edges keep about 1 unit of clear space, and never less than 0.5, which only parts that sit side by side by design reach, such as the rows and the object of rule 2. A small part that must read on its own, such as the clock of Work schedules, is at least about 6 units across. Avoid shapes where many lines meet at sharp corners, such as a cube: at 16 px the corners blur and the lines seem to run past them.
6. **One stroke.** When a part is too tight, give it room or remove parts; never draw it with a thinner stroke.
7. **Naming.** The icon's id is the menu entry's name in kebab case, such as `work-schedules` or `approve-time-entries`; an icon no menu entry uses is named after its kind of record in the singular, such as `tag` or `tag-group`.
8. **Adding and checking.** Draw the icon on the grid, join its elements into one path, and add it to `ts-icons.js` in the icon's wrapper. A menu icon is named in `menu.xml` and its pair added to `MenuIconsTest`; an icon only empty states use is named in their `gridEmptyState` elements and added to `MenuIconsTest`'s `EMPTY_STATE_ONLY`. Check it next to its neighbors at the size it is shown, 16 px in the menu and about 18 px beside an empty state's heading, in both themes, on a high-density screen, at strokes from 1.33 to 1.75. In the dev run, a changed iconset reaches the app only after a restart, which rebuilds the development bundle because a frontend file changed.
