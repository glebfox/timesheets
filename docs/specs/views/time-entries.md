# Time entry screens

Source: `view/timeentry/`, `action/TimeEntryCopyAction.java`

The screens for logging and reviewing time: My time entries, Time entries, and the time entry editor. What a time entry is and which rules apply to it: [time entry](../data-model/time-entry.md).

## Lists

**My time entries** (`my-time-entries`, menu Timesheets) lists the current user's own entries; **Time entries** (`time-entries`, menu Project Management) lists everyone's and adds a user column. Both:

- order entries by date, newest first;
- show the date, the task with its project beneath it, the spent time, the status as a badge (New yellow, Approved green, Rejected red, Closed neutral), the description, the tags as badges, and the rejection reason; a long description or rejection reason is cut short, with the full text in a popover that renders the Markdown;
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
- **Rejection reason** — shown only while the status is Rejected, and then required.
- **Tags** — see below.

The description and the rejection reason are Markdown editors with a preview.

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

## Not covered

- All of these screens: no UI test exists for them.
- Required tag fields blocking the save: not checked live.
