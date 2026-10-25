# Work schedule

Source: `entity/schedule/WorkSchedule.java`, `app/WorkScheduleSupport.java`, `listener/WorkScheduleEventListener.java`, `app/WorkPlanSupport.java`, `app/util/WorkPlan.java`, `liquibase/changelog/2026/10/25-161255-08247a92.xml`, `liquibase/changelog/init-data/090-init-work-schedule.xml`

A named weekly pattern of planned work, such as "Full time" or "0.5 rate": how long a workday is, which days of the week are workdays, and optionally a [holiday calendar](holiday-calendar.md) whose days override the week. Users are tied to schedules by [assignments](work-schedule-assignment.md); a user's plan for a day comes from them.

Schedules are set up in Data Tools: no screen of the application manages them or shows a plan, and the calendar plans 8 hours Monday to Friday whatever a user's schedule ([calendar](../views/calendar.md#totals)).

## Attributes

- **Name** — required; unique among non-deleted schedules (`docs/guidelines/database.md`).
- **Description** — optional.
- **Workday hours** — required; the planned time of each workday, a [work time](work-time.md) value such as 8h or 6h 30m.
- **Monday** to **Sunday** — one flag per day of the week; a new schedule has Monday to Friday on and Saturday and Sunday off.
- **Holiday calendar** — optional; without one, the schedule knows no holidays. Several schedules may share one calendar.
- **Default** — off for a new schedule. See below.

## The default schedule

Exactly one non-deleted schedule is the default. A user's days before their first assignment, and every day of a user with no assignments, use it. The rule holds on every save, whatever screen or `DataManager` call makes it, and whatever the saving user may read:

- Marking a schedule as the default unmarks the previous default in the same save.
- Unmarking the default, or deleting it, is refused while no other schedule is the default, with the message "There must be one default work schedule. Make another schedule the default first." To change the default, mark another schedule.

The schema changelog creates "Full time" in every environment: 8 hours, Monday to Friday, no holiday calendar, the default. A user with no assignments is therefore planned 8 hours, Monday to Friday, with no holidays. In the `dev` demo data it has the Russia holiday calendar.

## A user's plan for a day

1. The schedule is the one of the user's assignment with the latest start date on or before the day, or the default schedule when there is none.
2. When the schedule has a holiday calendar with this day, the day decides: a day off plans nothing, a workday plans the schedule's workday hours.
3. Otherwise a day whose day of the week is on in the schedule plans its workday hours, and any other day nothing.

A day is a workday when it plans more than zero. A plan covers a date range, its end excluded, and answers only for the days in it. It does not depend on the current user's permissions: every user gets their own plan, whatever roles they hold.

There are no versions of a schedule: editing its hours or days of the week changes the plan of every day it applies to, past days included. Conditions that change from some date are a new schedule, assigned from that date. Changing the default likewise changes the past plan of users who have no assignment on those days.

## Deletion

Deleting a schedule removes it from the lists, but the assignments that refer to it keep it, so the plans of their days stay as they were, past, current, and future. Deleting the default is refused unless another schedule is the default; see above. A schedule that was deleted as the default, in the same save that marked another, comes back as not the default when it is restored while another schedule is the default.

## Not covered

- Nothing refuses a schedule whose workday hours are zero, which plans nothing on any day, or whose days of the week are all off, which plans time only on the Workday days of its holiday calendar. The tests cover only a schedule with every day off and no calendar.
- Unmarking the default in the same save that marks another: no test.
- The default rule for a saving user who cannot read every schedule: the tests save as `admin`.
- Name uniqueness: the tests run on HSQLDB, which has no unique indexes (`docs/guidelines/testing.md`).
