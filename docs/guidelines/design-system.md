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

**Light and dark.** The theme picked with the user menu's theme switch, Light, Dark, or System, which follows the system's setting, reaches the stylesheets through the root element: Jmix's `color-scheme-switching-support.js`, which `TimesheetsApplication` loads, sets `color-scheme` and a `theme` attribute of `light` or `dark` on `<html>`. A color follows it with `light-dark()`, as the project colors in `tokens.css` do. What is not a color, such as the drawer's logo, which has an image for each theme, keys on `html[theme='dark']`. A `prefers-color-scheme` media query sees only the system's setting and misses a theme the user picked.

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

Reach these colors through the theme names and classes rather than through the `--ts-*` tokens directly. The exception is a project CSS rule that colors a single property of the app's own element, such as a text color, a border, or a progress bar's fill: it uses the `--ts-*` token, because an accent class would recolor everything accent-based inside the element. There is no project orange or neutral; such rules use `--aura-orange` and `--aura-neutral`, except a neutral fill shown beside the status colors, such as Closed on the approval screen: `--aura-neutral` is the text color itself, nearly black in the light theme and nearly white in the dark one, so such a fill takes the mid gray of `--vaadin-text-color-secondary`, which Aura derives from it.

**Badges** are Vaadin's `Badge` component, not a `span` with `theme="badge"`: that look comes from a stopgap stylesheet in the Jmix Aura layer, marked for removal now that the component exists. Aura styles the badge's `success`, `warning`, `error`, `filled`, and `small` variants, but not `contrast`, which compiles and changes nothing; a neutral badge takes the `.aura-accent-neutral` class instead. `BadgeStyle` in `data/renderer/` applies these.

**Toggle buttons** are a checkbox group or a radio button group with a class that makes each item read as a button, or as a tab of a filled tab bar, a checked one filled with the accent color, while the keyboard works as for the group's checkboxes or radio buttons:

- `toggle-group` (`component/toggle-group.css`), on a checkbox group, as the work schedule editor's workdays: a row of separate buttons, each pressed on its own.
- `segmented-group` (`component/segmented-group.css`), on a radio button group, as a holiday calendar day's type and the approval screen's Week | Month and roster switches: one control, since only one item can be chosen, drawn as Aura draws its tabs in the `filled` variant, from the same tokens: a gray track without a border holds the segments, each sized as a tab, as tall as a button, and the chosen segment is a pill filled with the accent color, while an unchosen label is muted, as an unselected tab's is, and takes the full text color on hover. So the whole control is taller than a button by the track's padding, as Aura's tabs are, and a row that puts buttons or fields beside it centers its items, as the approval screen's toolbar does, so that they line up on its middle. With no border to the track, the invalid and read-only states draw theirs as a line along its inner edge, which takes no room. The segments are of equal width, each at least as wide as the longest label, and a label never wraps. The focus ring is drawn as on Aura's tabs: inside an unchosen segment, and outside the chosen pill, within the track's padding.

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

**Box sizing.** The theme leaves the project's own elements in `content-box`, so their padding and border add to a width set on them. A part given a share of its row, such as an approval figure at `flex-basis: calc(50% - var(--vaadin-gap-s))` for two to a row, sets `box-sizing: border-box`; otherwise its padding and border push the second part onto a row of its own.

**Focus rings at an edge.** A focus ring reaches outside its part by its width, `--vaadin-focus-ring-width`, plus its offset, which Aura sets to 2 pixels for a primary button, to minus the border's width, 1 pixel inward, for any other button, and to zero or less for most other parts. A container that clips its overflow, as one that scrolls does, cuts the ring off at the outer edge of its padding, so a container whose parts reach its edge, such as the approval screen's person panel with the person's primary button, pads by the ring's width plus 2 pixels.

---

## 5. Responsive Behavior

- **Mobile** (< 640px): Single column, stacked layouts, full-width cards
- **Tablet** (640–1024px): Two-column grid, side-by-side content
- **Desktop** (> 1024px): Multi-column grid

A container query styles the descendants of its container, never the container itself, so what changes under the query sits inside the container. As the approval figures go from four in a row to two, their row is the container, a wrapping flex container, and each figure changes its `flex-basis`; a grid that is itself the container cannot change its own columns.

A container's width at a common window is not the screen's. With the navigation drawer open at 1280 pixels, the approval screen's person panel is about 640 pixels wide, and a classic scrollbar, as on Windows or in Playwright's Chromium, takes 15 pixels of it where a Mac's overlay scrollbar takes none. A switch within a scrollbar's width of such a width puts the same window on either side of it, depending on the scrollbar.

---

## 6. Icons

Icons inside screens, on fields, buttons, and grids, come from the Gravity UI set (`gravity-ui:<name>`), and so do most of the framework's own. `TsIcons` replaces Jmix's `Icons` bean and draws each icon that Jmix names by its purpose, such as `CREATE_ACTION` or `GENERIC_FILTER_SETTINGS`, as the Gravity UI counterpart of the Vaadin icon that Jmix's `jmix-font-icons.css` maps it to, or the nearest one where Gravity UI has none: a display for the system theme and `file-letter-x` for the Excel export. Save is the one icon chosen by its meaning: a floppy disk, where Jmix draws an archive box. It lists the names of the framework and of the add-ons the project uses: the standard list, detail, and picker actions, a detail view's OK and Cancel, the generic and column filters, and the user menu. Any other name falls through to Jmix's icon, so an add-on brought in later, such as Reports, keeps its Vaadin icons until its names are added there. The project's own actions and dialog buttons that stand for a standard one, such as Add or a dialog view's OK, show the standard one's icon; asking that bean for it, `icons.get(JmixFontIcon.<NAME>)` in code or `icon="<NAME>"` in a descriptor, keeps them in step with it.

Three exceptions remain. Icons the framework creates without that bean keep Jmix's Vaadin icons: the buttons of the standard option and input dialogs (`DialogAction.Type`), the pagination, the arrow of the generic filter's Refresh button, which is a combo button, the twin column's buttons, and the close button on each tab of the main view. Icons the framework asks that bean for by a glyph's name rather than a purpose keep them too, such as those the framework's own screens set, like the Entity inspector's export and import: `TsIcons` hands such names on to Jmix, apart from the close button (`CLOSE_SMALL`) of dialog views, notifications, and pickers' dialogs and a dialog's maximize button (`EXPAND`, `COMPRESS`), which it draws too. And the avatar field's person placeholder is a Lucide drawing.

The main menu has a set of its own, drawn for it: each group and item has its own icon, and no two are the same. The rest of this section is about that set.

**The style.**

- Outline only, no fills. Lines have round caps and round joins, and the corners of rectangles are rounded.
- A 16 × 16 grid, `viewBox="0 0 16 16"`. The live area is 1..15, so the centers of the strokes stay within 1.75..14.25.
- One stroke width for the whole set, `--ts-menu-icon-stroke-width` in `tokens.css`, 1.5 grid units, never mixed inside an icon or across the set. The menu draws its icons at 16 px, so one grid unit is one CSS pixel.
- A dot is a zero-length line, `M x y h0`: a round dot of the stroke width.
- The set reads at 16 px on a high-density screen at strokes from 1.33 to 1.75. A thinner stroke only widens the gaps; a stroke above 1.75 needs the tight spots checked again: Time entries, Locks, and Work schedules.

**How the icons reach the menu.**

- The menu's icons are an iconset, `ts-menu`, in `src/main/frontend/src/component/icon/ts-menu-icons.js`, a module of the same form as `gravity-ui-icons.js` but written by hand: `scripts/icons/generate.py` builds only the sets that come from an npm package. A class in `component/icon/` carries its `@JsModule`, which brings the module into the frontend bundle. So the icons arrive with the bundle, before the menu is shown, and the bundle's file names carry a hash of their content, so a browser never keeps an old copy of the set after a deploy. Why an iconset of the project's own, and not Font Awesome, Gravity UI, or an SVG sprite: [decision 0010](../decisions/0010-the-main-menu-draws-its-own-icons-from-an-iconset.md).
- Each `menu` and `item` in `menu.xml` names its icon with the `icon` attribute: `icon="ts-menu:<id>"`.
- Each icon is a `<g id="ts-menu:<id>" style="fill: none; stroke: var(--vaadin-icon-color, currentColor); stroke-width: var(--ts-menu-icon-stroke-width); stroke-linecap: round; stroke-linejoin: round">`, and the shapes inside it set no `fill`, `stroke`, `stroke-width`, or `style` of their own. `vaadin-icon` draws a copy of that `<g>`, its `style` included, in a `0 0 16 16` view box given by the iconset's `size="16"`. `MenuIconsTest` checks the set against the menu.
- Each icon draws one `<path>`: its circles, ellipses, and rectangles are written as arcs and joined into it. A menu item's text color is translucent, and every element of an SVG is painted on its own, so where the lines of two elements touch or cross they would be painted twice and show darker in the light theme and brighter in the dark one; one path is painted once.
- The stroke width is the project's own property. Vaadin's `--vaadin-icon-stroke-width`, which Aura sets to 1.75 for every icon, is not set or overridden anywhere in the project, so changing `--ts-menu-icon-stroke-width` changes every menu icon and no other icon.
- The lines take the color Vaadin gives icons, `--vaadin-icon-color`, falling back to `currentColor`, so a selected or hovered item colors its icon as it colors its text.
- `component/icon.css` sets `--vaadin-icon-visual-size: 80%` on the `ts-menu:` icons as on the Gravity UI icons, which draws them at 16 px inside the item's icon box.

**The rules for a new icon.**

1. **What an icon shows.** One object per screen: a small preview of what the screen shows, not of what the user does there. Few parts; small details do not survive 16 px.
2. **Two entries with one object.** When two entries would get the same object, the group, or the narrower or personal entry, gets the plain object, and the wider list gets rows plus the object: three rows on the left, one full and two short, and the object in the bottom right, in about 7..15. Time entries (beside My time entries), Row-level roles (beside the Security shield), Entity inspector (the magnifier alone is the search of filter fields), and Locks (beside Pessimistic locking) follow it.
3. **A family with a mark.** Entries that share a base object and differ in one aspect take the base object cut open at the bottom right and one mark in that corner, a zone of about 7.5 × 7.5: Work schedules and Holiday calendars on the calendar. A mark is not placed inside the base object: inside the calendar there are about 2.5 units of free height, and a mark drawn there becomes a blot.
4. **Meanings are shared with the screens.** The menu never gives an object a meaning that contradicts what the same object means on screens. The meanings in use:
   - briefcase — a project (project fields; Projects);
   - clock — time spent or work time (time fields; My time entries, Time entries, Work schedules);
   - tag — a project role (the participant editor's Role field; Project roles), and a tag's name (the tag editor);
   - tags — a task type (the task editor's Type field; Task types), and a tag group (the tag editor);
   - shield — security roles (Security, Resource roles, Row-level roles);
   - magnifier — search and inspection (filter fields; Entity inspector);
   - rows — the list of all records of a kind (Time entries, Row-level roles, Entity inspector, Locks);
   - calendar — dates and days (date fields; Calendar, Work schedules, Holiday calendars);
   - star — the default work schedule (the dialog that picks a new default); not available to the menu;
   - key — a password (the user editor); not available to the menu;
   - person — a person (person fields; Project roles, Users).
5. **Geometry.** The grid, live area, and stroke of the style above. Between stroke edges keep about 1 unit of clear space, and never less than 0.5, which only parts that sit side by side by design reach, such as the rows and the object of rule 2. A small part that must read on its own, such as the clock of Work schedules, is at least about 6 units across. Avoid shapes where many lines meet at sharp corners, such as a cube: at 16 px the corners blur and the lines seem to run past them.
6. **One stroke.** When a part is too tight, give it room or remove parts; never draw it with a thinner stroke.
7. **Naming.** The icon's id is the menu entry's name in kebab case, such as `work-schedules` or `approve-time-entries`.
8. **Adding and checking.** Draw the icon on the grid, join its elements into one path, add it to `ts-menu-icons.js` in the icon's wrapper, name it in `menu.xml`, and add the pair to `MenuIconsTest`. Check it at 16 px in the menu next to its neighbors, in both themes, on a high-density screen, at strokes from 1.33 to 1.75. In the dev run, a changed iconset reaches the app only after a restart, which rebuilds the development bundle because a frontend file changed.
