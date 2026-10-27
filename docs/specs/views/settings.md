# Settings screens

Source: `view/tasktype/`, `view/projectrole/`, `view/workschedule/`, `view/holidaycalendar/`, `view/holidaycalendarday/`, `view/workscheduleassignment/`, `action/WorkScheduleMakeDefaultAction.java`, `action/WorkScheduleRemoveAction.java`, `action/WorkScheduleUsersAddAction.java`, `action/WorkScheduleAssignmentEditAction.java`, `action/WorkScheduleAssignmentRemoveAction.java`, `action/HolidayCalendarDaysAddAction.java`, `app/WorkScheduleAssignmentSupport.java`, `app/util/ComponentSupport.java`, `app/util/PluralForms.java`, `app/util/UnsavedInputSupport.java`

The catalogs under the Settings menu: task types, project roles, work schedules, and holiday calendars.

## Task types

`task-types`. Lists task types with the name and description, a name filter, and pagination, both kept in the URL; Create, Edit, and Remove. The description is cut short, with the full text in a popover that renders the Markdown. The editor (`task-types/:id`, a 640px dialog when opened as one) has the name and the description, in a Markdown editor with a preview. What a task type is: [task type](../data-model/task-type.md).

## Project roles

`project-roles`. Lists project roles with the name, the type as a badge (Manager primary, Approver green, Observer neutral, Member plain), and the description, which is cut short with the full text in a popover that renders the Markdown. It offers a name filter, a type filter, and pagination, all kept in the URL; Create, Edit, and Remove. The editor (`project-roles/:id`, a 640px dialog when opened as one) has the name, the type, and the description in a Markdown editor with a preview. What a project role is: [project role](../data-model/project-role.md).

## Work schedules

`work-schedules`. Lists [work schedules](../data-model/work-schedule.md), one selected at a time, with the name and a plain Default badge on the default schedule; the workday hours as a work time (`8h 00m`); the workdays as seven day chips in the week order of the user's locale, Sunday first in English and Monday first in Russian, the workdays plain and the other days muted; the holiday calendar, a deleted one included; and the description, cut short with the full text in a popover. It offers a filter and pagination; Create, Edit, Remove, and Make default.

The filter opens on its "By name" («По названию») configuration, a single condition that keeps the schedules whose name contains the text typed. A user who may change filter configurations, such as a holder of [Full access](../roles/system-full-access.md), can also add conditions on any attribute of a schedule. A holder of [Work schedule management](../roles/work-schedule-management.md) may not, so filters by name only. The filter keeps its conditions when the browser page is reloaded, which restores the open tab; opening the screen again, or returning to it with the browser's back or forward button, starts from "By name" with nothing typed.

- **Make default** — enabled for one selected schedule that is not the default. It asks first, under the usual confirmation header, with Yes and No: "Make «Half time» the default schedule? Users with no assignments will work to it, past months included." No changes nothing. Yes marks the schedule as the default and saves it at once; the previous default is unmarked in the same save ([the default schedule](../data-model/work-schedule.md#the-default-schedule)). The list reloads, and a notification says "«Half time» is now the default schedule". A schedule deleted in another session since the list was loaded is not saved: the list reloads, and a warning says the schedule is already deleted.
- **Remove** — on a schedule that is not the default, the usual confirmation, then deletion, even when assignments refer to the schedule: they keep it, and their plans do not change. On the default, the [default deletion dialog](#deleting-the-default-schedule). When the default is the only schedule, Remove deletes nothing and warns: "The only work schedule cannot be deleted. Create another and make it the default first."

## Work schedule editor

`work-schedules/:id`, opened as a page.

- **Name** and **workday hours**, side by side, both required. Workday hours are typed as a [work time](../data-model/work-time.md#input), such as `8h` or `7:30`; zero is refused with "Must be more than zero".
- **Workdays** — required, full width: seven separate toggle buttons, each pressed on its own, labeled with the short day names in the locale's week order; a checked day is filled with the accent color. The keyboard works as it does for checkboxes. A schedule with no day checked is refused, with the buttons marked: "Select at least one workday". A line under the buttons counts the week as the fields change, such as "5 workdays, 40h a week" («5 рабочих дней, 40ч в неделю»), or only the workdays while the hours are empty.
- **Holiday calendar** — optional: a drop-down of the non-deleted calendars, searched by name, with a picker that opens the [holiday calendar list](#holiday-calendars), and a clear button. A schedule whose calendar is deleted shows that calendar with the helper "This calendar is deleted" and saves as it is; choosing another calendar drops the helper.
- **Description** — a plain text area.
- **Users** — the schedule's assignments; see below.

OK saves the schedule and its assignment changes together; Cancel discards both. In a read-only editor the workday buttons cannot be changed either, and the checked days still stand out from the others. The editor does not change which schedule is the default: only [Make default](#work-schedules) and the [default deletion dialog](#deleting-the-default-schedule) do.

### Users

The [assignments](../data-model/work-schedule-assignment.md) to this schedule: the user (avatar, full name or username, and an Active or Inactive badge) and the start date, newest first as the editor opens. Assignments added here go to the top, whatever their dates, and an edited one stays where it is. Why they are not sorted into place: [decision 0004](../../decisions/0004-editor-tables-keep-their-load-order.md). The columns fit their content, and fit again when assignments are added or edited. Only explicit assignments are listed: users who work to the default schedule because they have no assignment are not. A green Current badge beside the date marks an assignment in force today for its user: the latest start on or before today among that user's assignments to any schedule, as they stand in this editor. It moves at once when an assignment is added, edited, or removed here.

Multiple selection, with Add, Edit, and Remove. Their changes reach the database only when the schedule is saved, a new schedule included.

- **Add** — opens the [add users dialog](#add-users-dialog).
- **Edit** — opens the [assignment editor](#assignment-editor) with the user shown read-only. An assignment moved to another schedule there leaves the table, and the schedule's OK saves it with that schedule.
- **Remove** — the usual confirmation, then removes every selected assignment.

### Add users dialog

A 480px dialog, titled "Work schedule assignment", that assigns several users to the schedule from one date.

- **Users** — required: a multi-select drop-down of users, inactive ones included, searched by username, first name, or last name, case-insensitively, with a clear button; its picker opens the user list with its filter, so any number of users can be picked there.
- **Start date** — required.
- A muted line names the schedule, such as "Schedule: Full time", while the schedule has a name.

An empty field is marked "Users required" or "Start date required" («Заполните поле "Пользователи"», «Заполните поле "Дата начала"»).

OK adds one assignment per selected user, from that date, to this schedule, at the top of the table. It is refused, with both fields marked, when any selected user already has an assignment from that date to any schedule, saved or added in this editor and not removed: "Already assigned from this date: Thomas Anderson [anderson]", listing every such user by name and username. Nothing is skipped or overwritten. Changing either field clears the refusal.

Closing the dialog any other way, with Cancel, Esc, or the close button, while a field differs from how the dialog opened, asks the framework's question "Do you want to discard unsaved changes?": Yes closes the dialog and adds nothing, No keeps it open as it was. A field changed and changed back counts as unchanged, and so do users picked and cleared again. OK, and closing an unchanged dialog, ask nothing.

### Assignment editor

A 480px dialog that edits one assignment, opened from the Users section above and from the [user editor](users.md#work-schedule). Its OK changes the assignment only in the editor that opened it, which saves it.

- **User** — read-only; hidden when opened from the user editor.
- **Start date** — required. A date the user already has on another assignment, saved or in the opening editor and not removed there, is refused: "Already assigned from this date".
- **Work schedule** — required: a drop-down of the non-deleted schedules, searched by name. An assignment of a deleted schedule shows that schedule with the helper "This schedule is deleted" and saves with only its date changed; choosing another schedule drops the helper.

## Deleting the default schedule

A 480px dialog, titled "Delete the default schedule", that Remove opens on the default schedule. Its text names the schedule: "«Full time» is the default schedule. Choose which schedule becomes the default instead."

- **New default schedule** — required: a drop-down of the other non-deleted schedules, with the helper "Users with no assignments will work to it, past months included", since changing the default changes their past plans ([decision 0002](../../decisions/0002-work-schedules-have-no-versions.md)). Delete with none chosen marks it: "New default schedule required" («Заполните поле "Новый график по умолчанию"»).
- **Delete**, red, marks the chosen schedule as the default and deletes the old one in one save, so a failed save changes neither; the list then reloads. **Cancel** closes the dialog.

## Holiday calendars

`holiday-calendars`. Lists [holiday calendars](../data-model/holiday-calendar.md), one selected at a time, with the name; Filled until, the latest date among the calendar's days, empty when it has none, so a calendar missing next year shows at a glance; and the description, cut short with the full text in a popover. Filled until sorts from its header, as the name does, with a calendar that has no days as the earliest, and the order holds when the list reloads. It orders only the calendars of the page shown: which calendars a page holds does not depend on it. The list offers a name filter and pagination; Create, Edit, and Remove. The name filter keeps its text when the browser page is reloaded; opening the screen again, or returning to it with the browser's back or forward button, starts it empty. Remove asks the usual confirmation even when schedules use the calendar: they keep its days. It is also the calendar picker of the work schedule editor.

## Holiday calendar editor

`holiday-calendars/:id`, opened as a page.

- **Name** — required.
- **Description** — a plain text area.
- **Days** — Create, Edit, Remove, and a table of the calendar's days, newest first as the editor opens: the date with its short weekday name before it, such as "Fri, 01/01/2027" («пт, 01.01.2027», the Russian weekday in lowercase); the type as a badge, Day off neutral and Workday plain; and the name. The weekday is there to catch a wrong date: a Workday on a Friday or a Day off on a Sunday reads as wrong at once. Days added go to the top, newest first among themselves, whatever their dates, and an edited day stays where it is. Why they are not sorted into place: [decision 0004](../../decisions/0004-editor-tables-keep-their-load-order.md). The date and type columns fit their content, and fit again when days are added or edited; the name takes the rest. Multiple selection: Remove asks the usual confirmation and removes every selected day. Create opens the [calendar days dialog](#calendar-days-dialog), Edit the [day editor](#day-editor).

Changes to the days, including those made in the dialogs, reach the database only when the calendar is saved: OK saves the calendar and its days together, and Cancel discards both.

### Calendar days dialog

A 480px dialog, titled "Calendar days", that adds one or more days at once.

- **Name** — required.
- **Dates** — required: the first day, a dash, and an optional last day, whose picker offers no day before the first. A line under them names the range as it changes, such as "8 days, Friday to Friday" («8 дней, с пятницы по пятницу»), or "1 day, Friday" («1 день, пятница») for one day; it is empty while the range ends before it starts.
- **Type** — required, Day off by default: one bar of two joined segments, Day off and Workday, the chosen one filled with the accent color.

An empty name or first day is marked "Name required" or "Dates required" («Заполните поле "Название"», «Заполните поле "Даты"»).

OK adds one day per date of the range, weekends included, each with the same name and type, at the top of the table; an empty last day adds one day. A last day typed before the first is refused with the framework's message, which names the first day, such as "Value should be greater than '08/01/2027'". A range longer than 31 days is refused, with the last day marked, so a typo in a year does not add a year of days: "No more than 31 days". A range with any date among the calendar's days, saved or added in this editor and not removed, is refused with the dates marked: "Already in the calendar: 31/12/2026", listing every such date. Existing days are neither skipped nor overwritten. Changing either date clears these refusals; a last day still before a changed first day stays refused, and the message names the new first day.

Closing the dialog other than with OK while a field differs from how it opened asks the unsaved-changes question, as the [add users dialog](#add-users-dialog) does.

### Day editor

A 480px dialog, titled "Holiday calendar day", that edits one day: **Name**, **Date**, and **Type**, the last as the same bar. A date that another day of the calendar has, saved or added in this editor and not removed, is refused: "This day is already in the calendar".

## Not covered

- Task types and project roles: no UI test exists for them.
- How the work schedule and holiday calendar screens look, in English and Russian, light and dark: the day chips; the workday buttons and the day type bar with their focus ring, keyboard use, hover and press, and read-only and invalid states; the badges and their colors; the counted lines; the refusal listing several dates; and the red Delete. Only a live check saw them. The UI tests check the texts of the counted lines and of the refusal, the workdays line only in English.
- What only a browser shows: the columns fitting their content after their rows change, a Default or Current badge that appears then included; the Current badge moving to another row after an edit; the look of the Make default confirmation; and the last day's picker greying out the days before the first. The UI tests check the server side only: that the browser is asked to measure the columns again, that an edit redraws every row, the confirmation's texts, and the last day's minimum. Checked live.
- Cancel closing an unchanged work schedule editor without asking to save: checked live only.
- Saves on PostgreSQL that remove a saved day or assignment and add another with the same date in one editor session: they pass live, but no test can see them, since HSQLDB has no partial unique indexes (`docs/guidelines/testing.md`).
- An assignment moved to another schedule and given another date in one session of the work schedule editor is checked at its saved date: in that session the add users dialog and the assignment editor still refuse its old date for that user, and accept its new date, which the database then refuses on PostgreSQL with "The user already has a work schedule from this date". No test covers it.
- Pagination of both lists, the filters after a reload of the browser page, Remove of a schedule that assignments refer to, Remove of a calendar, and a failed Delete in the default deletion dialog: no test. The work schedule filter's tests fill in its name condition, as `admin` and as a holder of Work schedule management, but do not check that the holder gets no Add condition and no settings button: checked live only.
- The refusal of a last day typed before the first: tested in English only.
- Searching the drop-downs by typed text: the users of the add users dialog by username, first name, or last name, case-insensitively, with inactive users offered too, and the calendars and schedules by name. The tests fetch only the options of an empty search.
