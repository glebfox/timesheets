# Dashboard

Source: `view/dashboard/`, `app/DashboardSupport.java`, `app/TeamDashboardSupport.java`, `app/TimeEntryApprovalSupport.java`, `app/WorkPlanSupport.java`, `app/util/Attention.java`, `app/util/ComingUp.java`, `app/util/DaySummary.java`, `app/util/MyWeek.java`, `app/util/PersonAbsence.java`, `app/util/PersonWeek.java`, `app/util/ProjectTime.java`, `app/util/ShortDay.java`, `app/util/WaitingApproval.java`, `app/util/WeekSummary.java`, `app/util/WeekStatus.java`, `app/util/WorkPlan.java`, `app/util/ComponentSupport.java`

What the work area shows while no tab is open: after sign-in and each time the last tab closes. It has no menu item and no address of its own, and as part of the [main view](main.md) it needs no permission beyond [UI: minimal access](../roles/ui-minimal.md); the menu toggle stays above it, first in the work area. Why it is not a screen: [decision 0018](../../decisions/0018-the-dashboard-is-the-main-views-initial-layout-not-a-screen.md).

## Layout

A header, then **My time** for a user who may log time, that is, create time entries, then **My team** for a user who approves ([who sees it](#who-sees-it)). The headings "My time" and "My team" show only when both sections do, so a user who approves nowhere sees no heading, and neither does one who may not log time: a holder of [Time entries: approval](../roles/time-entry-approval.md) without a role that lets them log time sees My team alone, and a user who may do neither, such as an [Observer](../roles/observer.md), sees the header alone.

Each section is a grid of cards, each with a title: one column while the dashboard is narrower than 600 pixels, two from 600, three from 1024. The width is the work area's, not the window's, so opening or collapsing the drawer can change the columns.

- **My time**, in this order: This week, two columns wide (the whole row on two columns); Needs attention; Last 8 weeks; Time by project; Coming up; Recent entries, the whole row always.
- **My team**: on three columns, Waiting for my approval and Team away stacked in the first column and Team this week across the other two; on two, Waiting for my approval and Team away side by side and Team this week across the row below; on one, Waiting for my approval, Team this week, Team away. Without Team this week, the other two stand side by side.

The look these decisions of the user's keep, which later changes must not spoil:

- Every row of a grid is full, and the cards of one row are of one height: a chart grows to fill its card, a short list stays at the card's top, and Team away on two columns stretches to its row.
- Neighbouring cards of a row show no step: their titles, their first rows, and their empty-state lines stand on one line each, and every card is at least as tall as its title and three rows.
- A part that grows keeps its proportions: Team this week's bars thicken with their rows up to a limit, and Time by project's donut keeps its size, centered in its card.
- Spacing grows in three clear steps: inside a card, between cards, and between the header and the sections. Every card has one left and one right text edge, on which the texts, values, and avatars of its rows stand, whether a row is clickable or not.
- A row that does something is clickable whole and is reached by Tab; its highlight and focus ring reach outside the text edges, as far from the card's outer edge on the left, on the right, and, under a card's last row, at the bottom. On a touch screen it is at least 44 pixels tall.
- A link to a screen sits at the end of its card's header, its text ending on the content edge: "My entries" («Мои записи») and "Approve" («Утвердить»), the texts the user chose; "Approve" stays although it reads as an action.
- A block with nothing to show shows an icon and one line, quieter than data, centered on the whole card. It offers no Refresh: the header's Refresh reloads every block ([screens](README.md#empty-lists-and-tables)).
- Every date takes one short form, such as "Nov 10", with the year only outside the current year, such as "Dec 30, 2025"; a range is as short, such as "Oct 18 – 24" or "Sep 27 – Oct 3". Times are whole hours without minutes (`36h`), otherwise hours and minutes (`35h 30m`), as on the [calendar](calendar.md#totals).

## Header

- **Greeting** — "Good morning", "Good afternoon", or "Good evening" and the user's first name, or the username when it is empty: morning from 05:00 to 11:59, afternoon from 12:00 to 17:59, evening otherwise, the night included, in the user's time zone (the browser's when the user has none). Under it, the weekday, the date, and the week number in the user's locale, such as "Friday, Nov 14 · week 46".
- **Log time** — opens the [time entry editor](time-entries.md#editor) in a dialog over the dashboard on a new own entry dated today, in the user's time zone. Shown only to a user who may create time entries.
- **Refresh** — reloads the header and every block.

## Whose data, and when

- The current user is the effective user: under user substitution, the substituted user.
- My time reads as the [calendar](calendar.md) reads: the plan, the absences, and the holidays whatever the user may read, and the time entries, and so the rejected count, through the user's roles. Plan and time spent are computed as the calendar computes them; today is counted in the time zone of the user who is signed in, not the substituted user's ([user](../data-model/user.md#attributes)), and the weeks follow their locale's week (Sunday to Saturday in English, Monday to Sunday in Russian).
- A week on the dashboard is always whole, seven days across a month's or a year's end. The calendar's week cards count only the displayed month's days, so a week that spans two months may read differently on the two.
- A user has no start date: the default work schedule plans every workday before their first assignment, so a new user sees the past workdays of the dashboard's window as short, in This week, Last 8 weeks, and Needs attention, as the calendar counts them ([#89](https://github.com/glebfox/timesheets/issues/89)).
- Every block loads its data each time the dashboard is shown, and on Refresh; nothing loads while a tab hides the dashboard. A main view opened on a tab, as by a link to a screen, shows the dashboard only once that tab closes. The dashboard does not follow changes made elsewhere while it is shown; the main menu's rejected badge stays the only live number ([main view](main.md#main-view)).

## My time

### This week

- The week's time spent, large, with "of <plan> planned" and where the week stands in the calendar's [week status](calendar.md#week-status) words, such as "of 40h planned · 10h to go".
- A bar per day of the time spent, with the day's plan as a dashed outline. A day's label is the weekday over the day of the month; today's label differs only by its weight. A day off, one of an absence or a Day off of the holiday calendar, is hatched over the chart's whole height and named under its label, the absence's type before a holiday's name. Under 600 pixels the labels narrow and the bars thin, so neighbouring days keep apart.
- A day's bar is colored as the calendar colors a day's total: red under its plan, today and the days ahead included, green on it, blue over it, and neutral on a day without a plan. There is no legend.
- The tooltip: the weekday and date, spent, planned, where the day stands in the week status words (such as "−2h short"), and the day off's name.
- A click on a day opens the calendar on its month, if the user may open the calendar; otherwise it does nothing.

### Last 8 weeks

- A bar per week, the current one and the seven before it, labeled with the week's number in the locale, of the time spent, with the week's plan as a dashed outline.
- Each bar is colored by the week's status: Over blue, On plan green, Short red (the red of a short day, not the orange of the calendar's week summary: the user's choice, so a shortfall stands out), and a current week under its plan in that blue at half strength. A legend under the chart names the statuses the bars show, in the order Over, On plan, Short, To go, and then Plan.
- The tooltip: the week's dates, spent, planned, and the status note.
- A click on a week opens the calendar on the month of its first day, if the user may open the calendar.
- The hours axis of This week, Last 8 weeks, and Team this week ends at least half a step above the tallest bar or plan, in steps of 1, 2, or 5 hours times a power of ten.

### Needs attention

- **Rejected entries** — the Rejected status's icon and "Rejected entries" in the danger color, with the count, the main menu's red badge, at the row's end. It opens My time entries, with its filter empty, if the user may open them.
- **Short days** — the past days of the current and the previous week, today excluded, whose time spent is under their plan, newest first, each with how much is missing, such as "Mon, Nov 10 · 2h missing".
- **Short weeks** — the weeks among the last 8 whose status is Short, newest first, each with how much is missing.
- At most 5 rows, rejected first, then days, then weeks, and "and N more" for the rest. A short day or week opens the calendar on its month, if the user may open the calendar; otherwise its row is plain.
- "My entries" in the header opens My time entries; without permission to open them, the header has no link.
- Nothing to show reads "All caught up" with the Approve time entries icon.

### Time by project

- The current month, named in the subtitle without its year: a donut with the month's total in its hole, and beside it a list of the projects, each with its color, its name (two lines, then cut, and whole in a tooltip on hover), and its time, the largest first, then by name. With more than six projects, the five largest are shown and the rest together as Other. Only the entries the user may read count.
- A project's color follows its place by size, not the project, so the largest is always blue (accepted by the user): six fixed colors, each at least 3:1 against the card in both themes, and Other in the neutral.
- The tooltip, under the donut so that it covers neither the total nor the list: the project, its time, and its share in percent.
- Nothing spent this month: "No time spent this month" with the Projects icon.

### Coming up

- The user's absences that have not ended, the current one included, within six months, nearest first, at most 3; then the Day off holidays of their holiday calendar within six months, nearest first, at most 3, consecutive days of one name as one row with its range.
- Each row: the name, its kind under it (Absence or Day off), and the dates. The rows do nothing on a click.
- Nothing coming: "Nothing coming up" with the Calendar icon.

### Recent entries

- The user's 5 latest entries by date, entries of one date in a fixed order, in rows without a header or borders that span the card: the date, quieter; the task, then its project in the secondary color after "·", on a second line under 600 pixels; the time spent; the status badge; and Copy to today, which ends each row on the content edge. A name too long is cut from its end, the project first, so the task stays.
- The task is a link as wide as its text and opens the entry in the time entry editor, in a dialog over the dashboard, if the user may open the editor; otherwise it is plain text. A click on the project or on a cell's empty room does nothing.
- **Copy to today** — an icon button with its name as the tooltip; it opens the editor on a new entry [copied](../data-model/time-entry.md#copying) from the row and dated today.
- After a save in either, the dashboard reloads.
- Tab enters the rows once and leaves them with the next Tab; the arrows move between cells; Space on the task or on Copy to today presses it; Enter moves the focus onto the cell's button, and a second Enter presses it.
- No entries: "No time entries yet" with the My time entries icon.

## My team

### Who sees it

A user who may open the [approval screen](approval.md) and approves on at least one project, as a participant with a Manager- or Approver-type role. A user who approves nowhere sees no My team, Full access included.

### The people

The people the user approves by participation: on a project where the user is a Manager, every participant; where the user is an Approver, every participant except the project's Managers. The project's Observers are left out, as they follow the project without working on it, so a person who is only an Observer on the user's projects is on no team of theirs. The user is left out, as their own time is in My time, and so are inactive and deleted users; a deleted participant, project role, or project grants nothing, and a participant whose project role is deleted is left out. A Manager and an Approver see the same blocks.

### Waiting for my approval

- The New entries the user approves, of any date, by the [approval screen's rule](approval.md#which-entries-it-shows): their number, large, then their total time and the date of the oldest. Rejected entries are not counted. Entries of deleted and inactive users, of former participants, and of Observers count, so the number matches the approval screen's.
- Up to 5 people with the most waiting entries, then by name, each with avatar, name, and count, and "and N more". A click on a person opens the approval screen in Month mode on the month of their oldest waiting entry, under Needs review, with the person selected.
- "Approve" in the header opens the approval screen as the menu does.
- Nothing waiting: "Nothing waits for your approval" with the Approve time entries icon.

### Team this week

- A horizontal bar per person, all of them, in alphabetical order, the first on top; the chart grows taller with the team. The names stand beside the bars, and above them on a chart narrower than 600 pixels. Two people of one name get a bar each.
- A bar is the person's time spent this week: on the projects where the user approves them, split by status in the order Approved green, New amber, Rejected red, Closed gray; on any other project, one striped gray segment, a plain total with no project or status.
- The person's plan, computed as for My time, is a dashed outline. A person absent the whole week, with nothing planned, is hatched along the axis with the absence's name.
- A legend names what the bars show: the statuses, Other projects, Plan, and Absence, each only if some bar shows it. The tooltip: the name, the time spent, each status's time, the other projects' time, the plan, and each absence with its dates.
- A click on a person opens the approval screen in Week mode on the current week, under Everyone, with the person selected. A person with no entries to approve this week, time on other projects alone included, is not clickable.
- A team of nobody hides the block.

The other projects' time, the plan, and the absences are shown whatever the user may read: [decision 0020](../../decisions/0020-the-dashboard-shows-an-approver-their-teams-other-time-plans-and-absences.md).

### Team away

- Subtitle "This week and next": the absences of the same people that share a day with this week or the next, nearest first, then the one ending first, then by name: avatar, name, the absence's type, also of a deleted type, and the whole absence's dates; not the comment. No permission on absences is needed.
- The rows do nothing on a click.
- No one away: "No one is away" with the Absences icon.

## Chart colors

Each status has one color in both themes, the app's color for the same status: Approved and On plan green, Rejected and Short red, New the warning amber, and Closed, other projects, and a day without a plan a neutral gray. Over is a blue of the brand's hue, lighter than the brand's blue so that it reads on the dark card too, and To go that blue at half strength: the calendar shows Over in the accent, the brand's blue in the light theme and a neutral white in the dark one, which one color for both themes cannot follow. New and To go fall below 3:1 against the light card, the user's choice of consistency with the calendar and the approval screen; the legend and the tooltip name them. The rest of a chart, its axis labels and grid lines, follows the light or dark theme, and a tooltip looks like the app's popovers in either theme, shows whole however narrow its chart, and is never wider than the window. Why: [decision 0021](../../decisions/0021-the-dashboards-charts-color-statuses-as-the-app-does-in-one-color-for-both-themes.md), and for Over and To go [decision 0022](../../decisions/0022-the-dark-themes-accent-is-a-neutral-white.md). The charts come from the Jmix Charts add-on: [decision 0019](../../decisions/0019-the-dashboards-charts-come-from-the-jmix-charts-add-on.md).

## Not covered

- The charts' colors since the brand restyling, the new blue of Over and To go and the amber of New: `MyTimeChartsUiTest` checks only the literals, and they have not been looked at live, since the developer's app shows a Java change only after a restart.
- What the tests check. `DashboardUiTest`: the initial layout holds the menu toggle first and the dashboard after it; the greeting's name; the date line's week number; My time and Log time hidden without create permission, and Log time opening an own entry dated today; a save reloading the dashboard; a block asking it to reload; reloading when shown again, once per show; and the headings. `GreetingTest` and `DashboardDatesTest`: the times of day and the date formats. `DashboardSupportTest` and `TeamDashboardSupportTest`: the figures of every block, the people of the team, the Observers left out, and substitution. `MyTimeChartsUiTest`: what each chart is given, its rows, colors by status, the today label, the narrow options, the hours axis, the legends, Time by project's Other, palette, tooltip place, and the project names' tooltips, and the calendar opening on a click or not without permission; and, by reporting the charts ready, that options and data reach the browser. `MyTimeListsUiTest`: Needs attention's rows, link, and "All caught up"; Coming up; Recent entries opening, copying, and the reload after a save; the lists hidden from a user who may not create time entries; a new user's empty states. `MyTeamUiTest`: who sees My team and My time beside it, the Observers left out of the team, the blocks' places on the grid, Waiting for my approval, Team this week's data, clicks, names on a narrow chart, and a week of absence, and Team away.
- A participant whose project role is deleted, left out of the team: read from the query, no test.
- Recent entries' task as plain text and Needs attention without its "My entries" link, for a user who may create time entries but open neither the editor nor My time entries: no role grants that, and no test checks it.
- How the dashboard looks and responds in a browser, which no test sees (`docs/guidelines/testing.md`): the columns and placement at each width, the cards' heights, spacing and edges, the charts as drawn, hover, tooltips, the theme switch, the Russian layout, and the keyboard in Recent entries beyond a click that comes from no pointer.
- A live check on 2026-11-07, on a copy of the working tree with `dev` demo data, as `kleiner` (Manager and Approver), `admin` (Full access, Approver on one project), `chell` (Full access, approving nowhere), `mossman` (granular roles), `cave`, and `caroline`, saw: every block's content; the charts' colors, plan marks, hatching, and tooltips in both themes; the reflow at 1440×900, 1280×800, 1024×768, and 375×812 with the drawer open and collapsed, one column under 600 pixels, two from 600, three from 1024, no sideways scroll, and the menu toggle first; Log time saving and the dashboard reloading; a recent entry opening, and Copy to today; the reload after the last tab closes; the calendar and the approval screen opening from each block; a person with nothing to approve not clickable; and the Russian texts, plural forms, and Monday week start.
- Live checks during the polish rounds, on a copy of the working tree: on 2026-11-10 and 11-11 with `dev` demo data, as `admin`, `kleiner`, `chell`, and `mossman`, at 1440, 1280, 1024, and 375 pixels with a touch screen emulated, in both themes and both languages, they saw no text, badge, or avatar clipped, one text edge per card, equal rows, the type scale and the spacing steps, the team grid without holes, the dates, merged holidays, and the keyboard in Recent entries. On 2026-11-14, on a scratch database with the demo data and three test users, one with no entries and no holidays ahead, one with nothing that needs attention, and an approver with nothing waiting and no one away, they saw every block's empty state at 1440, 1280, and 375 in both themes; the rows' gaps and focus rings; the header links; the Recent entries grid and its keyboard; the charts redrawn after a resize or a drawer toggle; no sideways scroll on a slow window shrink, in both languages, with the drawer open, collapsed, or as an overlay; the tooltips whole and next to the pointer; the legends, the colors by status, This week by the day's status, the rejected row, and the donut's colors. They did not see: today's or a coming workday's bar in This week (the last check ran on a Saturday), forced colors, a real touch device, Firefox or Safari, or a user time zone different from the server's.
