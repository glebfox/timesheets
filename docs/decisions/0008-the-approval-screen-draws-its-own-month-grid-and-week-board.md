# 0008 The approval screen draws its own month grid and week board

Status: accepted

## Context

The approval screen shows a person's period in one of two layouts: a month as a grid of weeks in the layout of the Calendar, with a card of each week beside its row, and a week as a board of seven columns. The Calendar draws its month with FullCalendar. On the approval screen a day shows its time and the status of its entries, a week card, a column of the board, and a day heading in the list below the grid carry their own Approve buttons, a week card selects the week whose entries are listed below the grid, and the days of the neighboring months show nothing and count nowhere.

Alternatives considered and rejected:

- **FullCalendar, as the Calendar does.** The screen needs none of what FullCalendar is for: dragging, resizing, its views, and its event model. What it needs, a status on every day, an Approve button on every day of the board and of the list below the grid, and a card with controls of its own for every week, is markup that the screen writes directly, and FullCalendar would have it fitted around a component that draws its days itself. A popover with a day's entries, which the follow-up issue #49 asks for on the days of the month grid, also stays simple to add to a day that is the screen's own element.

## Decision

The month grid and the week board are components of the approval screen, in `view/timeentry/approval/`, built from the project's own markup and styled by the screen's stylesheet. They take the first day of the week from `ComponentSupport`, which reads it from the user's locale; the Calendar takes it from FullCalendar's own messages. The Calendar keeps FullCalendar. Decided in the design of the approval screen (issue #20).

## Consequences

- The grid and the board share no code or style with the Calendar's FullCalendar: a change to the Calendar's layout does not reach the approval screen, and the other way round. Both begin the week on Sunday in English and on Monday in Russian, but from two sources, so a locale added later has to be checked against both.
- What the screen needs from its cells is its own work, such as keyboard handling: the week cards are buttons made by hand, chosen by a click, Enter, or Space.
- A popover with a day's entries can be added to the month grid's day cells without fitting it into a calendar component.
