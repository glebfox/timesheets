# 0015 A range of two dates is one field of its own

Status: accepted

## Context

Three screens showed a range of dates as two date pickers joined by a dash, and each built it by hand: the absence editor, the holiday calendar days dialog, and the absence list's period filter. The two editors each carried about a hundred lines of controller code doing the same things — the last day's minimum and opening month, the range summary, marking both pickers on a refusal and clearing the mark on the next change — and the list styled its period in a stylesheet of its own. The three uses differ in their label, what is required, whether an empty last day means one day or no end, the summary, clear buttons, and where the data lives (glebfox/timesheets#73).

Alternatives considered and rejected:

- **Pull the shared controller code into a helper while each screen keeps its two pickers.** Less code to write, but every screen still lays out, binds, and validates two pickers, and the next range screen copies that again.
- **Keep the list's period in the URL with a `urlQueryParameters` binder for the new filter.** The list's other filters are kept there. In tabbed mode the facet is replaced with a no-op, and a reload of the browser page restores the open view with its filters (`docs/guidelines/views.md`), so a binder would add code that never runs.

## Decision

A range of two dates is one `DateRangeField` with its own XML element, loader, Studio entry, and stylesheet, bound to two attributes of an entity through a value source of its own; the differences between its uses are its attributes. A list filters by such a range with a `DateRangeFilter`, a filter component whose value component is the field, and it has no `urlQueryParameters` binder. The field stacks its dates only when two filled dates do not fit whole. Decided by the user.

## Consequences

- A screen with a range of dates declares the field and configures it; the controller keeps only what is specific to the screen, such as the absence editor's overlap check and the days dialog's 31-day limit.
- When the tabbed-mode add-on supports the `urlQueryParameters` facet, the list's other filters will be kept in the URL and the period will not, until a binder is written for it.
- A filter that keeps the rows whose single date lies within a range, and predefined ranges such as "this week", are not built; they would extend the field or the filter.
