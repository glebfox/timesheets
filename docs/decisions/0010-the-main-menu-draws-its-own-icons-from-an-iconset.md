# 0010 The main menu draws its own icons from an iconset

Status: accepted

## Context

The main menu showed Font Awesome Free icons. The free set has only filled styles, which do not match the outline Gravity UI icons inside the screens; it fit the screens only roughly, and some entries shared an icon, such as Resource roles and Row-level roles (issue #56). The menu needs an icon for every group and item, the add-on screens included, no two alike, in one outline style whose line width is set in one place and reaches no other icon.

Alternatives considered and rejected:

- **Font Awesome Free, as before.** Filled only, so the menu never matches the screens, and it has no distinct icon for every entry.
- **Gravity UI, as the screens use.** Its icons are outlines drawn as filled shapes at one fixed weight, so the menu's line width cannot be set in one place.
- **The same drawings as an SVG sprite in the theme, named in `menu.xml` with Jmix's `svgIcon` element.** It was built and measured on the branch of issue #56 and replaced before the merge. The sprite is a file of its own, so a cold load of the main view waited one more round trip for it, about 620 ms on Chrome's Slow 4G profile; it is served at a fixed address, so a browser kept the old file for up to an hour after a deploy; and Jmix loads an `svgIcon` only inside a UI, so the menu configuration could no longer be loaded outside one, as the role tests and `MenuIconsTest` load it.

## Decision

The main menu's icons are the project's own outline set, drawn for it on a 16-unit grid and delivered as a Vaadin iconset, `ts-menu`, in a JavaScript module that a class in `component/icon/` brings into the frontend bundle; `menu.xml` names each icon as `ts-menu:<id>`. The icons take their line width from the project's own property, `--ts-menu-icon-stroke-width`, not from Vaadin's `--vaadin-icon-stroke-width`, which Aura sets for every icon. Font Awesome is removed, and the name fields that used it take a Gravity UI icon. Decided by the user, the iconset over the sprite after the sprite was measured on a slow network (issue #56).

## Consequences

- The set arrives with the frontend bundle, before the menu is shown, under file names that carry a hash of their content, so a deploy never leaves an old copy in a browser.
- Every new menu entry needs a drawing of its own, made by the rules in `docs/guidelines/design-system.md`, and its pair in `MenuIconsTest`.
- In the dev run, a changed icon reaches the app only after a restart, which rebuilds the development bundle; a sprite in the theme would have needed only `processResources`.
- A change of `--ts-menu-icon-stroke-width` changes every menu icon and no other icon, and a change of Aura's stroke width in an upgrade does not reach the menu.
