# Task screens

Source: `view/task/`

My tasks, the task editor, and the task picker. What a task is: [task](../data-model/task.md). Who opens them: My tasks and the task picker, every job role but Observer, through [Time logging](../roles/time-logging.md), which also opens the task editor read-only, from My tasks and by its URL; the task editor, whoever opens the [project editor](projects.md), from its Tasks tab ([who can do what](../roles/README.md#who-can-do-what)).

## My tasks

`my-tasks`, menu Timesheets. Lists the tasks [bookable](../data-model/task.md#bookable-tasks) for the current user, by project name and then task name, with the project, name, type, and description, which is cut short with the full text in a popover. It offers a filter on any attribute and pagination, both kept when the browser page is reloaded. Empty, it shows the My tasks icon and "No tasks" («Нет задач»), with Refresh ([empty lists and tables](README.md#empty-lists-and-tables)).

- **Create time entry** — the primary action; needs one selected task and is available only to users who may create time entries. It opens the [time entry editor](time-entries.md#editor) in a dialog on a new own entry for the selected task, with the task locked.
- **Read** — opens the task editor read-only, in a dialog.

## Task editor

`tasks/:id`, an 800px dialog when opened as one; it is opened from the Tasks tab of the [project editor](projects.md#project-editor).

- **Name**, **description**.
- **Project** — shown only when the task has no project yet, which happens only when the editor is opened by URL; opened from a project, the task's project is fixed.
- **Type** — a drop-down of task types by name, or the task type picker.
- **Status** — Active or Inactive.
- **Required tag groups** — a multi-select of the tag groups of the task's project, or the tag group picker in multi-select mode limited to that project; the groups chosen in the picker are added to the field's current ones, which stay. The drop-down lists the groups of the project the task had when the editor opened, so a task created by URL without a project offers none there; its picker then shows the groups of the project chosen so far, or of every project when none is chosen. A helper under the field says what the groups mean: "Every time entry on this task needs a tag from each of these groups" («В каждой записи времени по этой задаче нужен тег из каждой выбранной группы»).

## Task picker

"Select a task", opened from the task field of the time entry editor for the entry's user. It lists the tasks bookable for that user, by project name and then task name, with a project filter (the Open projects where the user takes part in a role other than Observer) and a name filter, and pagination. Opened without a user, it lists every Active task of an Open project. Empty, it shows the My tasks icon and "No tasks" («Нет задач»), with Refresh.

## Not covered

- What the tests check: the empty states of My tasks and the task picker (`GridEmptyStatesUiTest`); both leaving out the tasks of a project where the user is an Observer, and the picker's project filter leaving out that project (`MyTaskListViewUiTest`, `TaskLookupViewUiTest`); and the task editor's Required tag groups helper, in English (`TaskDetailViewUiTest`). Nothing else on these screens has a UI test. The helper was seen live on 2026-11-16, on a copy of the working tree with `dev` demo data, in English and Russian, in the light theme. The Required tag groups picker, which no test opens, was seen live on 2026-11-21, on a copy of the working tree with `dev` demo data, as admin, in English, in the light theme: it opened in multi-select mode with only the project's groups, and the group picked there was added to the one already in the field.
