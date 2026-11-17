# Time entry screens

Source: `view/timeentry/`, `action/TimeEntryCopyAction.java`, `app/util/ComponentSupport.java`

The screens for logging and reviewing time: My time entries, Time entries, and the time entry editor. What a time entry is and which rules apply to it: [time entry](../data-model/time-entry.md). Approving and rejecting other people's entries by project role is a screen of its own: [Approve time entries](approval.md). Who opens them: My time entries and the editor on one's own entries, every job role but Observer, through [Time logging](../roles/time-logging.md); Time entries and the editor on other people's entries, read only, a [Manager](../roles/manager.md) and an [Observer](../roles/observer.md), through [Time entries: read only](../roles/time-entry-read.md), on the projects where they are a Manager, an Approver, or an Observer; and Full access, on every entry. The editor opened by its URL, `time-entries/:id`, also shows every holder of Time logging any entry their row-level role lets them read, read only unless they may change it: under Project scope, the entries of the projects where they are a Manager, an Approver, or an Observer; for [Project office](../roles/project-office.md), every entry ([who can do what](../roles/README.md#who-can-do-what)).

## Lists

**My time entries** (`my-time-entries`, menu Timesheets) lists the current user's own entries; **Time entries** (`time-entries`, menu Project Management) lists everyone's and adds a user column. Both:

- order entries by date, newest first;
- show the date, the task with its project beneath it, the spent time, the status as a badge (New amber, Approved green, Rejected red with an exclamation mark icon, Closed neutral), the description, and the tags as badges; in a Task column too narrow for them, the task's name and its project each end with an ellipsis; a long description is cut short, with the full text in a popover that renders the Markdown;
- tint every cell of a Rejected entry's row in the error color, under the row's selection, and show its rejection reason, rendered from Markdown, in a tooltip over its status cell, on hover and when the keyboard focus is on the cell; a Rejected entry with a blank reason shows no tooltip;
- fit the columns to the rows shown again whenever the rows change, as with another page, a filter, or a reload;
- offer a filter on any attribute and pagination, both kept when the browser page is reloaded;
- while empty, show the list's own menu icon, My time entries or Time entries, and "No time entries" («Нет записей времени»), with Refresh ([empty lists and tables](README.md#empty-lists-and-tables));
- offer Create, Edit, Remove, and Copy.

In My time entries, Create, Edit, and Copy open the editor for an *own* entry, Create and Edit in the list's own tab, replacing the list until the editor closes. In Time entries they open it for a *general* one (see below), so Time entries offers Create and Copy only to a user who may change an entry's user, as a holder of [Full access](../roles/system-full-access.md) may; everyone else logs their own time in My time entries, the [calendar](calendar.md), My tasks, or the [dashboard](dashboard.md).

**Copy** needs one selected entry, in any status, and is available only to users who may create time entries. It opens the editor, in a dialog, on a new entry prefilled from the selected one ([copying](../data-model/time-entry.md#copying)), so a copy of an Approved entry is a New one; the copy's user and task cannot be changed. OK saves the copy even when nothing in it was changed. The [time entry rules](../roles/time-entry-rules.md) judge the copy when it is saved, and refuse one on a project where the user no longer logs time: the editor's save then ends in the framework's access-denied error, where the [calendar](calendar.md)'s Copy warns instead.

**Remove** is enabled only on an entry the user may remove: under the time entry rules, their own entry while it is New or Rejected.

**Edit** reads Read on an entry the user may not change, and opens it read-only: under the time entry rules, in My time entries their own Approved or Closed entry, and in Time entries another person's entry.

## Editor

The time entry editor (`time-entries/:id`, a 640px dialog when opened as one) has these fields:

- **Date** — today's date for a new entry, in the time zone of the user creating it ([user](../data-model/user.md#attributes)), also when the entry is another user's.
- **User** and **task** — see the two modes below.
- **Spent time** — accepts the input forms of [work time](../data-model/work-time.md#input), with the helper "Such as 1:30 or 1h 30m" («Например, 1:30 или 1ч 30м») always under it.
- **Status** — hidden while the entry is being created, so a new entry is always saved as New.
- **Description**.
- **Tags** — see below.

The description is a Markdown editor with a preview.

A Rejected entry opens with a notice at the top of the editor, above the fields and across their width, which scrolls with them: an error icon, the title Rejected, and below it the rejection reason rendered from Markdown, or the title alone when the reason is blank. It shows the entry as it was loaded, so choosing another status in the editor neither shows nor hides it. An entry of any other status, and a new entry, shows none. The editor has no field for the reason, which the [approval screen](approval.md#rejecting) writes.

Saving a change to the date, user, task, spent time, description, or tags of an Approved or Rejected entry sends it back to New, unless another status is chosen as well ([status](../data-model/time-entry.md#status)).

### Own and general entries

An *own* entry is opened from My time entries, from My tasks, from the calendar, by Copy in My time entries, and from the dashboard by Log time, a recent entry's task, and Copy to today. Its user is the current user and cannot be changed.

A *general* entry is opened from Time entries. While it is being created:

- the task field stays locked until a user is chosen; the user field offers an "Assign to yourself" action and a search by username, first name, or last name;
- choosing a task locks the user field, and clearing the task unlocks it again;
- clearing the user locks the task field.

When an existing general entry is edited, the user field starts locked and the same task–user interplay applies.

A user who may not change an entry's user, such as a holder of [Time logging](../roles/time-logging.md) without [Full access](../roles/system-full-access.md), gets a general entry as an own one: a new entry, as the editor's URL `time-entries/new` opens it, takes them as its user, and the user field stays locked, whatever happens to the task.

A field locked by this interplay says why in a tooltip, on hover and on keyboard focus: the task field, while no user is chosen, "Choose a user first" («Сначала выберите пользователя»); the user field, while a task is chosen and the task field is not locked, "Clear the task to change the user" («Чтобы сменить пользователя, очистите задачу»). A field locked for any other reason has none: an own entry's user, a copy's user and task, and every field of an editor that opens read-only.

The task drop-down and its picker offer only tasks [bookable](../data-model/task.md#bookable-tasks) for the entry's user: the drop-down filters by task name, the picker by project and by name. The drop-down shows each task as the lists' Task column does, its name with its project beneath, each line ending with an ellipsis where it does not fit; the chosen task shows in the field as its project in brackets followed by its name. A task chosen before the editor opened, as by Create time entry in My tasks, by Copy, or by Copy to today on the dashboard, is locked.

### Tags

The tags section appears once a task is chosen and the task's project has tag groups. It holds one required field per required tag group of the task, labeled with the group's name and offering that group's tags, and one optional "Tags" field offering the tags of the project's other groups. Saved tags start in the field of their group.

Choosing another task rebuilds the section for the new task, dropping tag choices made since the last save. Saved tags that belong to none of the new task's groups land in the "Tags" field and are saved again unless removed. Changing tags counts as an unsaved change. Saving stores the tags of all the fields together.

In an editor that opens read-only, as for a user who may not update the entry, the tag fields are read-only too.

## Not covered

- Since the brand restyling, the badges' fixed tints and the blue of the tag badges were looked at live in My time entries, in the dark theme, on 2026-11-15; the other lists with badges were not.
- The screens apart from the rejection's marks, the tag fields' read-only mode, a new entry's date, the spent time helper, and the locked fields' tooltips. `TimeEntryListViewsUiTest` checks, in both lists, that there is no rejection reason column, that a Rejected row gets its part name, that the status cell's tooltip is the reason, rendered as Markdown, and that a blank reason and other statuses have none, the icon on the Rejected badge only, and that a reload asks the browser to size the columns again. `TimeEntryDetailViewUiTest` checks the notice for each status, a new entry, a blank reason, a status chosen in the editor, and another entry put in the editor's container, the tag fields following the read-only mode, the spent time helper with no placeholder, the locked fields' tooltips in a new and an existing general entry and none in a copy, an own entry, or a read-only editor, all in English, and that saving a changed Rejected entry makes it New and saving an unchanged one keeps it Rejected. `UserTimeZoneSupportUiTest` checks that a new entry is dated the user's today when the user's date is a day ahead of the server's. `TimeLoggingRoleUiTest` checks that a holder of Time logging gets a new general entry with their own user, locked also after a task is chosen and cleared, and `TimeEntryDetailViewUiTest` that Full access gets it with the user empty and editable. Own and general entries beyond the locked fields' tooltips, the task choice, Copy, the tags section, and the lists' filter, ordering, and actions have no UI test, apart from the time entry rules' checks below; `GridEmptyStatesUiTest` checks the lists' empty states.
- How the rejection's marks look, which no test sees (`docs/guidelines/testing.md`). A live check on 2026-11-03, as admin on a copy of the demo data, mostly in English and in Russian for the badge's width, in the light and the dark theme and at 400 pixels wide, looked at the notice and its full width, the row tint with a row selected, the badge's icon, the reason's tooltip on hover and on keyboard focus, and the status column growing on a page change to fit a Rejected badge it had not shown before.
- How the Task column cuts its lines, which no test sees (`docs/guidelines/testing.md`). A live check on 2026-11-14, as admin on `dev` demo data, in English, in the dark theme, in an 800-pixel-wide window, looked at both lists: with the Task column 84 and 110 pixels wide, every project line ended with an ellipsis, and at 320 pixels none was cut; no line was clipped vertically. Once the editor's task drop-down took the same lines, a live check on 2026-11-15, as freeman on `dev` demo data, in English, in the dark theme, in a 1024-pixel-wide window, looked at the lists again, with the Task column 131 pixels wide in My time entries: long names and every project line ended with an ellipsis, and no line was clipped vertically.
- How the task drop-down shows its tasks, which no test sees (`docs/guidelines/testing.md`). The same live check opened it on a new own entry: each task showed its name with its project beneath, and with the drop-down narrowed to 200 pixels, long names and projects ended with an ellipsis and no item's content ran out of the item; choosing a task filled the field with its bracketed project and name.
- How the spent time helper and the locked fields' tooltips look, which no test sees (`docs/guidelines/testing.md`). A live check on 2026-11-16, on a copy of the working tree with `dev` demo data, in English and Russian, in the light theme, looked at the spent time helper and the field beside it in its row; the task field's tooltip on hover and on keyboard focus, gone once a user was chosen; the user field's tooltip once a task was chosen, gone when the task was cleared; and none in a copy or an own entry. A read-only editor and the dark theme were not looked at.
- Required tag fields blocking the save: not checked live.
- The screens under the [time entry rules](../roles/time-entry-rules.md): `TimeEntryRulesUiTest`, as holders of Full access and Project scope, checks in My time entries that Copy is enabled on an Approved entry and opens the editor on a New copy, that a copy of a tagged Approved entry saved as it opened is stored New with the tags and listed, and that Remove is disabled on an Approved entry and enabled on a New one; that a Manager's own Approved entry opens read-only while the author's New one does not; and that an author who has left the project fixes the description of their Rejected entry in the editor, which keeps its tags and goes back to New. It checks that Time entries hides Create and Copy from a user who may not change an entry's user as a holder of UI: minimal access, a test role with a Manager's grants on time entries, and Project scope, and shows them to Full access alone, as `admin`. A copy refused when the editor saves it, with the framework's access-denied error; Edit reading Read on a locked entry in My time entries; and Remove and Copy in Time entries: no UI test.
- An Approver's or a Manager's own entry still shows an editable Status field in the editor until [#105](https://github.com/glebfox/timesheets/issues/105), and a status saved there that the time entry rules refuse ends in the framework's access-denied message.
