# Time entry screens

Source: `view/timeentry/`, `action/TimeEntryCopyAction.java`, `app/util/ComponentSupport.java`

The screens for logging and reviewing time: My time entries, Time entries, and the time entry editor. What a time entry is and which rules apply to it: [time entry](../data-model/time-entry.md). Approving and rejecting other people's entries by project role is a screen of its own: [Approve time entries](approval.md).

## Lists

**My time entries** (`my-time-entries`, menu Timesheets) lists the current user's own entries; **Time entries** (`time-entries`, menu Project Management) lists everyone's and adds a user column. Both:

- order entries by date, newest first;
- show the date, the task with its project beneath it, the spent time, the status as a badge (New yellow, Approved green, Rejected red with an exclamation mark icon, Closed neutral), the description, and the tags as badges; a long description is cut short, with the full text in a popover that renders the Markdown;
- tint every cell of a Rejected entry's row in the error color, under the row's selection, and show its rejection reason, rendered from Markdown, in a tooltip over its status cell, on hover and when the keyboard focus is on the cell; a Rejected entry with a blank reason shows no tooltip;
- fit the columns to the rows shown again whenever the rows change, as with another page, a filter, or a reload;
- offer a filter on any attribute and pagination, both kept when the browser page is reloaded;
- offer Create, Edit, Remove, and Copy.

In My time entries, Create, Edit, and Copy open the editor for an *own* entry, Create and Edit in the list's own tab, replacing the list until the editor closes. In Time entries they open it for a *general* one (see below).

**Copy** needs one selected entry and is available only to users who may create time entries. It opens the editor, in a dialog, on a new entry prefilled from the selected one ([copying](../data-model/time-entry.md#copying)); the copy's user and task cannot be changed.

## Editor

The time entry editor (`time-entries/:id`, a 640px dialog when opened as one) has these fields:

- **Date** — today's date for a new entry.
- **User** and **task** — see the two modes below.
- **Spent time** — accepts the input forms of [work time](../data-model/work-time.md#input); the placeholder suggests `hh:mm`.
- **Status** — hidden while the entry is being created, so a new entry is always saved as New.
- **Description**.
- **Tags** — see below.

The description is a Markdown editor with a preview.

A Rejected entry opens with a notice at the top of the editor, above the fields and across their width, which scrolls with them: an error icon, the title Rejected, and below it the rejection reason rendered from Markdown, or the title alone when the reason is blank. It shows the entry as it was loaded, so choosing another status in the editor neither shows nor hides it. An entry of any other status, and a new entry, shows none. The editor has no field for the reason, which the [approval screen](approval.md#rejecting) writes.

Saving a change to the date, user, task, spent time, description, or tags of an Approved or Rejected entry sends it back to New, unless another status is chosen as well ([status](../data-model/time-entry.md#status)).

### Own and general entries

An *own* entry is opened from My time entries, from My tasks, or by Copy in My time entries. Its user is the current user and cannot be changed.

A *general* entry is opened from Time entries. While it is being created:

- the task field stays locked until a user is chosen; the user field offers an "Assign to yourself" action and a search by username, first name, or last name;
- choosing a task locks the user field, and clearing the task unlocks it again;
- clearing the user locks the task field.

When an existing general entry is edited, the user field starts locked and the same task–user interplay applies.

The task drop-down and its picker offer only tasks [bookable](../data-model/task.md#bookable-tasks) for the entry's user: the drop-down filters by task name, the picker by project and by name. A task chosen before the editor opened, as by Create time entry in My tasks or by Copy, is locked.

### Tags

The tags section appears once a task is chosen and the task's project has tag groups. It holds one required field per required tag group of the task, labeled with the group's name and offering that group's tags, and one optional "Tags" field offering the tags of the project's other groups. Saved tags start in the field of their group.

Choosing another task rebuilds the section for the new task, dropping tag choices made since the last save. Saved tags that belong to none of the new task's groups land in the "Tags" field and are saved again unless removed. Changing tags counts as an unsaved change. Saving stores the tags of all the fields together.

In an editor that opens read-only, as for a user who may not update the entry, the tag fields are read-only too.

## Not covered

- The screens apart from the rejection's marks and the tag fields' read-only mode. `TimeEntryListViewsUiTest` checks, in both lists, that there is no rejection reason column, that a Rejected row gets its part name, that the status cell's tooltip is the reason, rendered as Markdown, and that a blank reason and other statuses have none, the icon on the Rejected badge only, and that a reload asks the browser to size the columns again. `TimeEntryDetailViewUiTest` checks the notice for each status, a new entry, a blank reason, a status chosen in the editor, and another entry put in the editor's container, the tag fields following the read-only mode, and that saving a changed Rejected entry makes it New and saving an unchanged one keeps it Rejected. Own and general entries, the task choice, Copy, the tags section, and the lists' filter, ordering, and actions have no UI test.
- How the rejection's marks look, which no test sees (`docs/guidelines/testing.md`). A live check on 2026-11-03, as admin on a copy of the demo data, mostly in English and in Russian for the badge's width, in the light and the dark theme and at 400 pixels wide, looked at the notice and its full width, the row tint with a row selected, the badge's icon, the reason's tooltip on hover and on keyboard focus, and the status column growing on a page change to fit a Rejected badge it had not shown before.
- Required tag fields blocking the save: not checked live.
