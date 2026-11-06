# 0016 One iconset of the project's own serves the main menu and the grids' empty states

Status: accepted. Supersedes [0010](0010-the-main-menu-draws-its-own-icons-from-an-iconset.md).

## Context

Decision 0010 made the main menu's icons a set of the project's own, `ts-menu`, whose line width reached no other icon. Issue #42 gave every grid an empty state that names what is missing beside an icon. The lists' empty states name the records their menu entries open, so their icons are the menu's drawings. Tags and tag groups have no menu entry and needed drawings of their own.

Alternatives considered and rejected:

- **Keep the name `ts-menu`.** It would say the set is the menu's while the grids use it too (decided by the user).
- **Gravity UI's tag icons for tags and tag groups, as elsewhere inside the screens.** They are filled shapes at one fixed weight, beside the outline icons of every other empty state; the mockup showed the mismatch, and the user chose drawings in the set's style.

## Decision

One set of the project's own, renamed `ts` (`ts-icons.js`, `TsIconset`, `--ts-icon-stroke-width`), holds the main menu's icons and the icons of the grids' empty states, among them `tag` and `tag-group`, which no menu entry uses. Each empty state shows the menu icon of the records it lists, and a kind of record without a menu entry gets a drawing of its own, made by the set's rules. 0010's reasons for an iconset of the project's own, delivered in the frontend bundle, rather than Font Awesome, Gravity UI, or an SVG sprite, still hold. Decided by the user (issue #42).

## Consequences

- A change of `--ts-icon-stroke-width` changes every icon of the set, in the menu and in the empty states, and no other icon; a change of Aura's stroke width in an upgrade reaches neither.
- The empty states draw the icons at about 18 px and the menu at 16 px, so a new or changed icon is checked at both sizes.
- An icon that only empty states use has no menu entry to name it, so `MenuIconsTest` lists such icons, and `GridEmptyStateDescriptorsTest` checks that every empty state names an icon of the set.
- As before, a changed icon reaches the dev run only after a restart, which rebuilds the development bundle.
