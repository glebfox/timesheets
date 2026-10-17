# Calendar

Source: `view/calendar/`, `app/CalendarSupport.java`, `app/util/FactAndPlan.java`, `app/util/WeekSummary.java`, `app/util/WeekStatus.java`, `entity/timeentry/TimeEntryCalendarWrapper.java`, `security/EditActiveTimeEntriesRole.java`

`calendar`, the first item of menu Timesheets. The current user's own [time entries](../data-model/time-entry.md) laid out on a month, with the time spent per day, per week, and for the month against the time planned.

## Month

- **Header** — the month and year of the displayed month, the year beside the month name in a muted color.
- **Controls** — previous, Today, and next move by a month, and Today is disabled while the current month is displayed; Jump to date moves to the month of the chosen date; Refresh reloads the entries and confirms it with a notification. They stand in that order, with Refresh last after a thin divider, and keyboard focus follows it.
- **Grid** — one row per week that has a day of the month; days of the neighboring months fill the first and last rows. Weeks start on Sunday in English and on Monday in Russian. Rows have equal heights: a day with more entries than fit shows a "+N more" link.
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

The plan is 8 hours for each workday, Monday to Friday, and nothing for Saturday and Sunday; holidays are not known. Spent time is summed exactly to the minute. A week and the month count only the days of the displayed month, and time spent on a weekend adds to the spent time and not to the plan.

- **Day** — the bottom of each day shows the day's total as `hh:mm`. On a workday it is red under the plan, green at the plan, and in the accent color over it; on a weekend it is not colored.
- **Week** — the column headed Week beside the grid has one card per week row, level with the row and bordered like the day cells, so that the column continues the grid. A card shows the time spent, `of <planned>` beside it, a progress bar, and a [status](#week-status) note. The bar shows the spent share of the plan, full at or over it; a week with no workdays but weekend time shows a full bar. The current week's card is highlighted, whatever its status. The cards stay level with the rows while the calendar's rows fit the window; on a very short window (under about 640px tall), or at phone width in a six-week month, the grid scrolls inside itself, the cards drift off their rows, and their notes are cut.
- **Month** — a footer under the grid with four figures, in this order: Month target (the plan), Logged (the time spent), Remaining (the plan less the time spent), and Of target (the time spent as a percent of the plan, rounded to a whole number, with a small bar under it that stops at full; the percent itself can exceed 100). When the month is over its plan, the third figure reads Over target and shows `+<over>` in the accent color instead of Remaining; on plan it shows zero. From 800px wide the four figures stand in one row, narrower in a 2×2 grid.
- **Format** — the week cards and the month figures show whole hours without minutes (`36h`), otherwise hours and minutes (`35h 30m`). The day's total stays `hh:mm`.

### Week status

A week is current when today is one of its days, upcoming when all of its days are after today, and past otherwise; today is the date the Today button moves to. In a past month every week is past, in a future month every week is upcoming. The first row that applies wins, so a current week over or on its plan shows as Over or On plan, not In progress.

| Status | When | Note |
|---|---|---|
| No workdays | nothing planned and nothing spent | No workdays |
| Over | more spent than planned, also weekend time in a week with no workdays | +4h over |
| On plan | spent equals the plan | ✓ On plan |
| In progress | under the plan, a current week | 20h to go |
| Upcoming | under the plan, an upcoming week | Upcoming |
| Short | under the plan, a past week | −4h 30m short |

Over is in the accent color, On plan green, and Short orange, not red, which stays the signal of the day totals. In progress and Upcoming are muted, since the week is not over; the time spent of an Upcoming week is muted too.

## Not covered

- The whole screen: no UI test exists for it. The week statuses are tested in `WeekSummaryTest`, the derived figures (difference and percent) in `FactAndPlanTest`, and the totals in `CalendarSupportTest`. How the screen looks was checked live on 2026-10-17: in English and in Russian (Monday week start), in the light and the dark theme, at 1440×900, 1280×720, 760×900, and 375×812, in September 2026 (five weeks), August 2026 (six weeks, starting on a Saturday), October 2026 (the current month), and November 2026 (a future month). That covered every week status; the footer, including an over-plan month and the 2×2 layout; the week cards level with the rows; no vertical scroll at 1280×720 in a six-week month; the order of the header controls and the keyboard order; and the totals following at once after an entry is created, edited, copied, removed, and moved. The Today button and reloading the page were checked live the same day.
- Moving an entry the user may not change: not checked live, since no seed user holds a role that forbids it.
