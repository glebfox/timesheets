# Calendar

Source: `view/calendar/`, `app/CalendarSupport.java`, `app/WorkPlanSupport.java`, `app/util/FactAndPlan.java`, `app/util/WeekSummary.java`, `app/util/WeekStatus.java`, `app/util/WorkPlan.java`, `app/util/ComponentSupport.java`, `entity/timeentry/TimeEntryCalendarWrapper.java`, `security/EditActiveTimeEntriesRole.java`

`calendar`, the first item of menu Timesheets. The current user's own [time entries](../data-model/time-entry.md) laid out on a month, with the time spent per day, per week, and for the month against the time their [work schedule](../data-model/work-schedule.md) plans.

## Month

- **Header** — the month and year of the displayed month, the year beside the month name in a muted color.
- **Controls** — previous, Today, and next move by a month, and Today is disabled while the current month is displayed; Jump to date moves to the month of the chosen date; Refresh reloads the plan and the entries and confirms it with a notification. They stand in that order, with Refresh last after a thin divider, and keyboard focus follows it.
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

## Days

Every day of the grid without a [plan](#totals), one that plans no time, is shaded whatever its day of the week, and a day with a plan is not: a holiday on a weekday is shaded, and a Saturday moved to a workday is not. Days of the neighboring months are shaded by the same rule and are not dimmed.

A day that the user's [holiday calendar](../data-model/holiday-calendar.md) decides shows the name the calendar gives it, whatever its type: a Day off, such as "Unity Day", in red, and a Workday, such as "Working Saturday", in the accent color. The holiday calendar is the one of the user's schedule in force that day. An ordinary weekend shows no name, and neither does any day of a schedule without a holiday calendar. The shading follows the plan and the color the type, so a Workday of a schedule with zero workday hours, which only the editor refuses, is shaded and named in the accent color.

- **Place** — the name stands at the start of the row of the day's number, in small type on one line, and the number keeps its place at the end of the row. A name too long for the row is cut with an ellipsis before it reaches the number.
- **Tooltip** — the full name shows on hover anywhere over the named day except its number and its entries, which keep their own tooltips.
- **Neighboring months** — on their days the name is dimmed, as their entries are.
- **Actions** — a right-click on a shaded or named day, outside its entries, offers Add for that day, as on any other day, and a click on its shaded or named part does nothing. Its number and its entries work as on any other day, and an entry can be dragged onto any day, with or without a plan.

## Totals

The plan of each day is the user's [plan for that day](../data-model/work-schedule.md#a-users-plan-for-a-day), from the work schedule they are on that day and its holiday calendar: a holiday plans nothing whatever its day of the week, and a weekend day moved to a workday plans the schedule's workday hours. It is the plan of the user whose entries the calendar shows, the substituted user under user substitution, whatever their permissions on schedules and calendars. The plan is read again on each move to another month and on Refresh, so changes made meanwhile to schedules, holiday calendars, and assignments show then; an open calendar does not follow them by itself. Spent time is summed exactly to the minute. A week and the month count only the days of the displayed month, and time spent on a day without a plan adds to the spent time and not to the plan.

- **Day** — the bottom of each day shows the day's total as `hh:mm`. On a day with a plan it is red under the plan, green at the plan, and in the accent color over it; on a day without a plan it is not colored, so the total of a holiday on a weekday is not colored and that of a worked Saturday is.
- **Week** — the column headed Week beside the grid has one card per week row, level with the row and bordered like the day cells, so that the column continues the grid. A card shows the time spent, `of <planned>` beside it, a progress bar, and a [status](#week-status) note. The bar shows the spent share of the plan, full at or over it; a week that plans nothing but has time spent shows a full bar. The current week's card is highlighted, whatever its status. The cards stay level with the rows while the calendar's rows fit the window. On a short window (under about 640px tall) the grid scrolls inside itself, the cards drift off their rows, and their notes are cut; at phone width in a six-week month the notes are cut as well.
- **Month** — a footer under the grid with four figures, in this order: Month target (the plan), Logged (the time spent), Remaining (the plan less the time spent), and Of target (the time spent as a percent of the plan, rounded to a whole number, with a small bar under it that stops at full; the percent itself can exceed 100). When the month is over its plan, the third figure reads Over target and shows `+<over>` in the accent color instead of Remaining; on plan it shows zero. The four figures stand in one row when the calendar's area is at least 800px wide, which with the menu open takes a window of about 1090px; narrower, they stand in a 2×2 grid.
- **Format** — the week cards and the month figures show whole hours without minutes (`36h`), otherwise hours and minutes (`35h 30m`). The day's total stays `hh:mm`.

### Week status

A week is current when today is one of its days, upcoming when all of its days are after today, and past otherwise; today is the date the Today button moves to. In a past month every week is past, in a future month every week is upcoming. The first row that applies wins, so a current week over or on its plan shows as Over or On plan, not In progress. A week of holidays plans nothing, so it reads No workdays unless time is spent in it, and Over if it is.

| Status | When | Note |
|---|---|---|
| No workdays | the week's days of the month plan nothing, and nothing is spent on them | No workdays |
| Over | more spent than planned, also any time spent in a week that plans nothing | +4h over |
| On plan | spent equals the plan | ✓ On plan |
| In progress | under the plan, a current week | 20h to go |
| Upcoming | under the plan, an upcoming week | Upcoming |
| Short | under the plan, a past week | −4h 30m short |

Over is in the accent color, On plan green, and Short orange, not red, which stays the signal of the day totals. In progress and Upcoming are muted, since the week is not over, and so is No workdays; the time spent of an Upcoming week is muted too.

## Not covered

- The whole screen except one flow and which days are shaded or named: `CalendarUiTest` edits an entry in its dialog and then moves it, or confirms its removal without going through the context menu, and checks which days are shaded, and that none is named, when the calendar opens on a schedule without a holiday calendar; that a move to another month drops the previous month's shading and names; and which days are shaded or named after a Refresh that follows a schedule change. The week statuses are tested in `WeekSummaryTest`, the derived figures (difference and percent) in `FactAndPlanTest`, and the totals, against a plan with a weekday holiday and a worked Saturday among others, and which days get a shade or a name, with which name, in `CalendarSupportTest`. How the screen looks was checked live on 2026-10-17: in English and in Russian (Monday week start), in the light and the dark theme, at 1440×900, 1280×720, 760×900, and 375×812, in September 2026 (five weeks), August 2026 (six weeks, starting on a Saturday), October 2026 (the current month), and November 2026 (a future month). That covered every week status; the footer, including an over-plan month and the 2×2 layout; the week cards level with the rows; no vertical scroll at 1280×720 in a six-week month; the order of the header controls and the keyboard order; and the totals following at once after an entry is created, edited, copied, removed, and moved. The Today button and reloading the page were checked live the same day, and so were moving an entry by dragging and removing it through its context menu right after editing it, once or twice in a row.
- How the days look, and entries on shaded and named days: checked live only, on 2026-10-28, on the `dev` demo data, in full in English, in the light theme, at 1440×900, and in part in Russian, in the dark theme, and at 1280×720. That covered the shading of the days without a plan, a worked Saturday unshaded with its name in the accent color and its total colored, and weekday holidays shaded with their names in red and their totals not colored; a long name cut before the number, its baseline level with the number's; the full name as a tooltip over the name and the empty part of the day while the number keeps its own and an entry without a description shows no holiday name; names dimmed on the neighboring months' days and the shading not; the New Year holidays, ten named days in a row, with the week of 4–10 January 2026 (Sunday start) and of 5–11 January (Monday start) reading No workdays; the month targets of a user on a schedule with a holiday calendar and of one whose schedule changes on the first of a month; clicking the number of a long-named day, and of a shaded or named day of a neighboring month, each of which opens a new entry; dragging an entry onto a holiday, which saves it there; and, on shaded or named days of a neighboring month, entries that open on a click, offer Copy and Remove on a right-click, and can be dragged to another day, and a "+N more" link that opens its popover, all of them still dimmed.
- The context menu on a shaded or named day, which offers only Add, and a click on one, which does nothing: the context menu cannot be driven in a UI test, and no UI test clicks a shaded or named day. Both were checked live on 2026-10-28 only, as was an entry's context menu still offering Copy and Remove.
- The colors of the day totals after a Refresh that changes the plan but not the entries: the UI tests check which days are shaded and named, not the totals' colors. Checked live on 2026-10-28 only: a schedule changed in another tab to work Saturdays unshaded the Saturdays, colored their totals, and raised the month target; after it was reverted, a Refresh shaded the Saturdays again and brought the month target back, and the totals' colors were not checked then.
- User substitution: the plan and the entries are both the substituted user's, but no seed user substitutes another, so neither a test nor a live check has seen it.
- The calendar of a user with no permission on work schedules, holiday calendars, or assignments: `WorkPlanSupportTest` loads such a user's plan, but the UI tests open the calendar as `admin`, and both seed users hold [Full access](../roles/system-full-access.md), so neither a test nor a live check has seen the screen for such a user.
- A Workday of a schedule with zero workday hours, shaded and named in the accent color: no test, and not checked live, since the demo data has no such schedule and the editor refuses one.
- Moving an entry the user may not change: not checked live, since no seed user holds a role that forbids it.
