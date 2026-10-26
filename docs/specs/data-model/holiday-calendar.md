# Holiday calendar

Source: `entity/schedule/HolidayCalendar.java`, `entity/schedule/HolidayCalendarDay.java`, `entity/schedule/HolidayCalendarDayType.java`, `app/WorkPlanSupport.java`

A named list of dated days that override the weekly pattern of the [work schedules](work-schedule.md) that use it, such as a country's public holidays and the weekend days moved to workdays because of them. One calendar spans all years: days are entered as they become known, year by year. Several schedules share one calendar, so "Full time" and "0.5 rate" in one country do not repeat the holiday list.

Calendars and their days are managed in the [holiday calendar screens](../views/settings.md#holiday-calendars).

## Attributes

- **Name** — required; unique among non-deleted calendars (`docs/guidelines/database.md`).
- **Description** — optional.
- **Days** — the calendar's days, owned by it and edited inside it. Only the [editor](../views/settings.md#holiday-calendar-editor) limits how many are added at once: it adds them from a range of at most 31 days.

Each day has:

- **Date** — required; at most once among the calendar's non-deleted days. The editor's dialogs refuse a date the calendar already has before the save; the database's unique index stays the last line.
- **Type** — required: Day off (a public holiday, or a day off moved from elsewhere) or Workday (a weekend day moved to a workday).
- **Name** — required, such as "New Year" or "Working Saturday".
- Shown as `<date> <name> (<type>)`.

A day of the calendar decides the plan of that date for every schedule that uses the calendar: a day off plans nothing and a workday plans the schedule's workday hours, whatever the day of the week. There are no recurring days and no shortened days.

## Deletion

Deleting a calendar removes it from the lists but not its days: the schedules that still refer to it keep its days, so their plans do not change. A day removed from a calendar on its own is deleted as usual and stops counting.

## Not covered

- Uniqueness of the name and of a calendar's dates: the tests run on HSQLDB, which has no unique indexes (`docs/guidelines/testing.md`).
