# 0004 Editor tables keep their load order

Status: accepted

## Context

The tables inside three editors open newest first: a holiday calendar's days in the calendar editor, and the work schedule assignments in the work schedule editor and the user editor. A row added there, or an edited row whose date changed, can belong elsewhere in that order, but a grid sorts its rows only when its sort order changes, so such a row stays where the container put it.

Alternatives considered and rejected:

- **Re-sort the table after every add and edit.** This was built first: a shared helper applied the grid's sort order again through the container's sorter on every change. The user, in the review of PR #33, did not want the table re-sorted on every change, and judged the reason for it, that new rows would otherwise land at the end, weak.
- **Sort the grid from code when the editor opens.** The order is lost on the next change, and it belongs to the data, not to the view.

## Decision

The order comes from the model or from the loader's query: the days' `@OrderBy` on the calendar, and the assignment queries ordered by start date, newest first. Rows added in an editor go to the top of their table, whatever their dates: the latest add above the earlier ones, and within one add of several dates, such as a range of calendar days, newest first. The user found that more convenient, since all new rows are on top and seen at once. An edited row stays in its place. Nothing re-sorts the table on a change of its container, and a user can still sort it by a column header. Decided by the user.

The decision covers the tables of these three editors; it is not a project-wide convention.

## Consequences

- After an add or an edit, a table can show rows out of date order until the editor opens again.
- An edit no longer redraws the whole table as a side effect of sorting, so the assignment tables redraw every row on a replace themselves, and the Current badge follows the edit (`docs/guidelines/views.md`).
- Going back to rows sorted into place means a re-sort on every change again.
