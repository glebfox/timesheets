# Settings screens

Source: `view/tasktype/`, `view/projectrole/`, `view/workschedule/`, `view/holidaycalendar/`, `view/holidaycalendarday/`, `view/workscheduleassignment/`, `action/WorkScheduleMakeDefaultAction.java`, `action/WorkScheduleRemoveAction.java`, `action/WorkScheduleUsersAddAction.java`, `action/WorkScheduleAssignmentEditAction.java`, `action/WorkScheduleAssignmentRemoveAction.java`, `action/HolidayCalendarDaysAddAction.java`, `app/WorkScheduleAssignmentSupport.java`

The catalogs under the Settings menu: task types, project roles, work schedules, and holiday calendars.

## Task types

`task-types`. Lists task types with the name and description, a name filter, and pagination, both kept in the URL; Create, Edit, and Remove. The description is cut short, with the full text in a popover that renders the Markdown. The editor (`task-types/:id`, a 640px dialog when opened as one) has the name and the description, in a Markdown editor with a preview. What a task type is: [task type](../data-model/task-type.md).

## Project roles

`project-roles`. Lists project roles with the name, the type as a badge (Manager primary, Approver green, Observer neutral, Member plain), and the description, which is cut short with the full text in a popover that renders the Markdown. It offers a name filter, a type filter, and pagination, all kept in the URL; Create, Edit, and Remove. The editor (`project-roles/:id`, a 640px dialog when opened as one) has the name, the type, and the description in a Markdown editor with a preview. What a project role is: [project role](../data-model/project-role.md).

## Work schedules

`work-schedules`. Lists [work schedules](../data-model/work-schedule.md), one selected at a time, with the name and a plain Default badge on the default schedule; the workday hours as a work time (`8h 00m`); the workdays as seven day chips in the week order of the user's locale, Sunday first in English and Monday first in Russian, the workdays plain and the other days muted; the holiday calendar, a deleted one included; and the description, cut short with the full text in a popover. It offers a name filter and pagination, both kept in the URL; Create, Edit, Remove, and Make default. Only one schedule ever shows the Default badge, also after an editor save that made another schedule the default.

- **Make default** — enabled for one selected schedule that is not the default. It marks the schedule as the default and saves it at once, with no confirmation; the previous default is unmarked in the same save ([the default schedule](../data-model/work-schedule.md#the-default-schedule)). The list reloads, and a notification says "«Half time» is now the default schedule". A schedule deleted in another session since the list was loaded is not saved: the list reloads, and a warning says the schedule is already deleted.
- **Remove** — on a schedule that is not the default, the usual confirmation, then deletion, even when assignments refer to the schedule: they keep it, and their plans do not change. On the default, the [default deletion dialog](#deleting-the-default-schedule). When the default is the only schedule, Remove deletes nothing and warns: "The only work schedule cannot be deleted. Create another and make it the default first."

## Work schedule editor

`work-schedules/:id`, opened as a page.

- **Name** and **workday hours**, side by side, both required. Workday hours are typed as a [work time](../data-model/work-time.md#input), such as `8h` or `7:30`; zero is refused with "Must be more than zero".
- **Workdays** — required, full width: seven day toggles, labeled with the short day names in the locale's week order; a checked day is filled with the accent color. The keyboard works as it does for checkboxes. A schedule with no day checked is refused: "Select at least one workday". A line under the toggles counts the week as the fields change, such as "5 workdays, 40h a week" («5 рабочих дней, 40ч в неделю»), or only the workdays while the hours are empty.
- **Holiday calendar** — optional: a drop-down of the non-deleted calendars, searched by name, with a picker that opens the [holiday calendar list](#holiday-calendars), and a clear button. A schedule whose calendar is deleted shows that calendar with the helper "This calendar is deleted" and saves as it is; choosing another calendar drops the helper.
- **Default** — a switch. On the default schedule it is on and read-only, with the helper "To change it, make another schedule the default". On any other schedule it can be turned on, and saving makes the schedule the default and unmarks the previous one.
- **Description** — a plain text area.
- **Users** — the schedule's assignments; see below.

OK saves the schedule and its assignment changes together; Cancel discards both. In a read-only editor the toggles cannot be changed either, and the Default switch stays read-only on the default schedule whatever the mode.

### Users

The [assignments](../data-model/work-schedule-assignment.md) to this schedule, newest first: the user (avatar, full name or username, and an Active or Inactive badge) and the start date. Only explicit assignments are listed: users who work to the default schedule because they have no assignment are not. A green Current badge beside the date marks an assignment in force today for its user: the latest start on or before today among that user's assignments to any schedule, as they stand in this editor. It moves at once when an assignment is added, edited, or removed here.

Multiple selection, with Add, Edit, and Remove. Their changes reach the database only when the schedule is saved, a new schedule included.

- **Add** — opens the [add users dialog](#add-users-dialog).
- **Edit** — opens the [assignment editor](#assignment-editor) with the user shown read-only. An assignment moved to another schedule there leaves the table, and the schedule's OK saves it with that schedule.
- **Remove** — the usual confirmation, then removes every selected assignment.

### Add users dialog

A 480px dialog, titled "Work schedule assignment", that assigns several users to the schedule from one date.

- **Users** — required: a multi-select drop-down of users, inactive ones included, searched by username, first name, or last name, case-insensitively; its picker opens the user list with its filter, so any number of users can be picked there.
- **Start date** — required.
- A muted line names the schedule, such as "Schedule: Full time", while the schedule has a name.

OK adds one assignment per selected user, from that date, to this schedule. It is refused, with both fields marked, when any selected user already has an assignment from that date to any schedule, saved or added in this editor and not removed: "Already assigned from this date: Thomas Anderson [anderson]", listing every such user by name and username. Nothing is skipped or overwritten. Changing either field clears the refusal.

### Assignment editor

A 480px dialog that edits one assignment, opened from the Users section above and from the [user editor](users.md#work-schedule). Its OK changes the assignment only in the editor that opened it, which saves it.

- **User** — read-only; hidden when opened from the user editor.
- **Start date** — required. A date the user already has on another assignment, saved or in the opening editor and not removed there, is refused: "Already assigned from this date".
- **Work schedule** — required: a drop-down of the non-deleted schedules, searched by name. An assignment of a deleted schedule shows that schedule with the helper "This schedule is deleted" and saves with only its date changed; choosing another schedule drops the helper.

## Deleting the default schedule

A 480px dialog, titled "Delete the default schedule", that Remove opens on the default schedule. Its text names the schedule: "«Full time» is the default schedule. Choose which schedule becomes the default instead."

- **New default schedule** — required: a drop-down of the other non-deleted schedules, with the helper "Users with no assignments will work to it, past months included", since changing the default changes their past plans ([decision 0002](../../decisions/0002-work-schedules-have-no-versions.md)).
- **Delete**, red, marks the chosen schedule as the default and deletes the old one in one save, so a failed save changes neither; the list then reloads. **Cancel** closes the dialog.

## Holiday calendars

`holiday-calendars`. Lists [holiday calendars](../data-model/holiday-calendar.md), one selected at a time, with the name; Filled until, the latest date among the calendar's days, empty when it has none, so a calendar missing next year shows at a glance; and the description, cut short with the full text in a popover. It offers a name filter and pagination, both kept in the URL; Create, Edit, and Remove. Remove asks the usual confirmation even when schedules use the calendar: they keep its days. It is also the calendar picker of the work schedule editor.

## Holiday calendar editor

`holiday-calendars/:id`, opened as a page.

- **Name** — required.
- **Description** — a plain text area.
- **Days** — Create, Edit, Remove, and a table of the calendar's days, newest first: the date with its short weekday name before it, such as "Fri, 01/01/2027" («Пт, 01.01.2027»); the type as a badge, Day off neutral and Workday plain; and the name. The weekday is there to catch a wrong date: a Workday on a Friday or a Day off on a Sunday reads as wrong at once. A day added or edited takes its place by date. Multiple selection: Remove asks the usual confirmation and removes every selected day. Create opens the [calendar days dialog](#calendar-days-dialog), Edit the [day editor](#day-editor).

Changes to the days, including those made in the dialogs, reach the database only when the calendar is saved: OK saves the calendar and its days together, and Cancel discards both.

### Calendar days dialog

A 480px dialog, titled "Calendar days", that adds one or more days at once.

- **Name** — required.
- **Dates** — required: the first day, a dash, and an optional last day. A line under them names the range as it changes, such as "8 days, Friday to Friday" («8 дней, с пятницы по пятницу»), or "1 day, Friday" («1 день, пятница») for one day; it is empty while the range ends before it starts.
- **Type** — required, Day off by default: two buttons, Day off and Workday, in the style of the workday toggles.

OK adds one day per date of the range, weekends included, each with the same name and type; an empty last day adds one day. It is refused, with the last day marked, when the last day is before the first ("Must not be before the first day") or the range is longer than 31 days ("No more than 31 days"), so a typo in a year does not add a year of days. It is refused, with the dates marked, when any date of the range is among the calendar's days, saved or added in this editor and not removed: "Already in the calendar: 31/12/2026", listing every such date. Existing days are neither skipped nor overwritten. Changing either date clears the refusal.

### Day editor

A 480px dialog, titled "Holiday calendar day", that edits one day: **Name**, **Date**, and **Type**, the last as the same two buttons. A date that another day of the calendar has, saved or added in this editor and not removed, is refused: "This day is already in the calendar".

## Not covered

- Task types and project roles: no UI test exists for them.
- How the work schedule and holiday calendar screens look, in English and Russian, light and dark: the day chips, the workday toggles and the day type buttons with their focus ring and keyboard use, the badges and their colors, a badge added after the table first shows fitting its column, the counted lines, the refusal listing several dates, and the red Delete. Only a live check saw them. The UI tests check the texts of the counted lines and of the refusal, the workdays line only in English.
- Cancel closing an unchanged work schedule editor without asking to save: checked live only.
- Saves on PostgreSQL that remove a saved day or assignment and add another with the same date in one editor session: they pass live, but no test can see them, since HSQLDB has no partial unique indexes (`docs/guidelines/testing.md`).
- An assignment moved to another schedule and given another date in one session of the work schedule editor is checked at its saved date: in that session the add users dialog and the assignment editor still refuse its old date for that user, and accept its new date, which the database then refuses on PostgreSQL with "The user already has a work schedule from this date". No test covers it.
- The name filters and pagination of both lists, Remove of a schedule that assignments refer to, Remove of a calendar, and a failed Delete in the default deletion dialog: no test.
