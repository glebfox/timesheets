# Calendar

Source: `view/calendar/`, `app/CalendarSupport.java`, `app/util/FactAndPlan.java`, `entity/timeentry/TimeEntryCalendarWrapper.java`, `security/EditActiveTimeEntriesRole.java`

`calendar`, the first item of menu Timesheets. The current user's own [time entries](../data-model/time-entry.md) laid out on a month, with the time spent per day, per week, and for the month against the time planned.

## Month

- **Header** — the month and year of the displayed month.
- **Controls** — Refresh reloads the entries and confirms it with a notification; previous, Today, and next move by a month, and Today is disabled while the current month is displayed; Jump to date moves to the month of the chosen date.
- **Grid** — one row per week that has a day of the month; days of the neighboring months fill the first and last rows. Weeks start on Sunday in English and on Monday in Russian. A day shows up to four entries and a "+N more" link for the rest.
- Reloading the page keeps the displayed month.

## Entries

Each entry shows as `<spent time> [<project>] <task>`, with a dot colored by status: New yellow, Approved green, Rejected red, Closed neutral. Entries on days of the neighboring months are dimmed. The calendar shows only the current user's entries, of the displayed days.

- **Edit** — clicking an entry opens the [time entry editor](time-entries.md#editor) in a dialog on an own entry.
- **Create** — clicking a day's number, or Add in the day's context menu, opens the editor on a new own entry for that day.
- **Copy** — in the entry's context menu; saves a [copy](../data-model/time-entry.md#copying) on the same day at once, without the editor.
- **Remove** — in the entry's context menu, after a confirmation.
- **Move** — dragging an entry to another day saves it with that date. When the user may not change the entry, as under the [edit active time entries](../roles/edit-active-time-entries.md) role for an Approved or Closed one, the entry returns to its day and a warning says why.

Copy, Remove, and Move are confirmed with a notification. After any of these, the totals below follow at once.

## Totals

The plan is 8 hours for each workday, Monday to Friday, and nothing for Saturday and Sunday; holidays are not known. Spent time is summed exactly to the minute.

- **Day** — the bottom of each day shows the day's total as `hh:mm`. On a workday it is red under the plan, green at the plan, and in the accent color over it; on a weekend it is not colored.
- **Week** — the Total column beside the grid shows one total per week, counting only the days of the displayed month: the spent time alone when it equals the plan, otherwise `<spent> (<planned>)`, red under the plan and in the accent color over it. Time spent on a weekend adds to the spent total and not to the plan.
- **Month** — "Total for the month" below the grid, in the same form and colors as a week.

## Not covered

- The whole screen: no UI test exists for it. The totals are tested in `CalendarSupportTest`; how they are shown was checked live on 2026-10-17, together with the day colors after creating, editing, copying, removing, and moving an entry, the Today button, reloading the page, and the Monday week start in Russian.
- Moving an entry the user may not change: not checked live, since no seed user holds a role that forbids it.
