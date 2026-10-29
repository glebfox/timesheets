# 0006 Java code sets no utility CSS classes

Status: accepted

## Context

A utility class carries one look, such as a font size, a color, or a font weight, and Vaadin offers Lumo's from Java as `LumoUtility` constants. The user cell set Lumo's `text-m`, `font-bold`, `text-body`, `text-s`, and `text-secondary` from Java. Aura defines no utility classes, so after the move from Lumo to Aura all of them did nothing: the name lost its weight, the username its size and color, and no build, test, or inspection reported it. The review of PR #43 found it long after the move.

Alternatives considered and rejected:

- **Replace them with the project's own utility classes from `app/util.css`, such as `secondary-text`.** This was done first in PR #43. It works today, but the look still lives in Java, and the next rename of a class or change of theme fails just as silently.
- **Load Lumo's utility stylesheet next to Aura.** It mixes the two design systems, which `docs/guidelines/design-system.md` rules out.

## Decision

Java code gives a component a class that names its part, such as `title` and `body` in the user cell, and the part's stylesheet in the theme gives its look, size, color, and weight included, with the theme's tokens. Java code sets no utility classes, neither Lumo's nor the project's own. Decided by the user.

## Consequences

- A look lives in one place, the theme, where a change of theme or token is made and checked.
- Parts that share a look each declare it in their own stylesheet instead of sharing a class.
- The task-with-project cell built by `ComponentSupport` predates the rule and still sets `secondary-text`.
