# 0006 A component with its own stylesheet sets no utility classes

Status: accepted

## Context

A utility class carries one look, such as a font size, a color, or a font weight, and Vaadin offers Lumo's from Java as `LumoUtility` constants. The user cell has its own Java class and its own stylesheet, yet it set Lumo's `text-m`, `font-bold`, `text-body`, `text-s`, and `text-secondary` from Java, so its look was split between the two. Aura defines no utility classes, so after the move from Lumo to Aura the half in Java did nothing: the name lost its weight, the username its size and color, and no build, test, or inspection reported it. The review of PR #43 found it long after the move.

Alternatives considered and rejected:

- **Replace them with the project's own utility classes from `app/util.css`, such as `secondary-text`.** This was done first in PR #43. It works today, but the cell's look stays split between its stylesheet and Java.
- **No utility classes from Java at all.** Proposed next in PR #43. A renderer built in place, in a view or in `ComponentSupport`, such as the task-with-project cell, has no class or stylesheet of its own, so the project's utility classes it sets split nothing.
- **Load Lumo's utility stylesheet next to Aura.** It mixes the two design systems, which `docs/guidelines/design-system.md` rules out.

## Decision

A component or renderer with its own Java class and its own stylesheet in the theme gives its parts classes that name them, such as `title` and `body` in the user cell, and its stylesheet gives their look, size, color, and weight included, with the theme's tokens; it sets no utility classes. A piece built in place, with no class or stylesheet of its own, may set the project's utility classes from `app/util.css`. Lumo's utility classes are used nowhere. Decided by the user.

## Consequences

- The look of a component with its own stylesheet lives in one place, where a change of theme or token is made and checked.
- A piece built in place that gets a class and a stylesheet of its own moves the looks of its utility classes into that stylesheet.
- Renaming or removing a utility class in `app/util.css` still fails silently in the pieces built in place.
