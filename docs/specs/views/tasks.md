# Task screens

Source: `view/task/`

My tasks, the task editor, and the task picker. What a task is: [task](../data-model/task.md).

## My tasks

`my-tasks`, menu Timesheets. Lists the tasks [bookable](../data-model/task.md#bookable-tasks) for the current user, by project name and then task name, with the project, name, type, and description, which is cut short with the full text in a popover that renders the Markdown. It offers a filter on any attribute and pagination, both kept in the URL.

- **Create time entry** — the primary action; needs one selected task and is available only to users who may create time entries. It opens the [time entry editor](time-entries.md#editor) in a dialog on a new own entry for the selected task, with the task locked.
- **Read** — opens the task editor read-only, in a dialog.

## Task editor

`tasks/:id`, an 800px dialog when opened as one; it is opened from the Tasks tab of the [project editor](projects.md#project-editor).

- **Name**, **description** — the description in a Markdown editor with a preview.
- **Project** — shown only when the task has no project yet, which happens only when the editor is opened by URL; opened from a project, the task's project is fixed.
- **Type** — a drop-down of task types by name, or the task type picker.
- **Status** — Active or Inactive.
- **Required tag groups** — a multi-select of the tag groups of the task's project, or the tag group picker in multi-select mode limited to that project; the groups chosen in the picker replace the field's current ones rather than adding to them. The drop-down lists the groups of the project the task had when the editor opened, so a task created by URL without a project offers none there; its picker then shows the groups of the project chosen so far, or of every project when none is chosen.

## Task picker

"Select a task", opened from the task field of the time entry editor for the entry's user. It lists the tasks bookable for that user, by project name and then task name, with a project filter (the user's Open projects) and a name filter, and pagination. Opened without a user, it lists every Active task of an Open project.

## Not covered

- All of these screens: no UI test exists for them.
