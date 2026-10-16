# Task

Source: `entity/task/Task.java`, `entity/task/TaskStatus.java`, `app/TaskSupport.java`

A piece of work within a project that people log time against.

## Attributes

- **Name** — required; unique within its project among non-deleted tasks (`docs/guidelines/database.md`).
- **Project** — required. A task is part of its [project](project.md): it is edited inside the project and deleted with it.
- **Type** — required, from the [task type](task-type.md) catalog.
- **Description** — optional.
- **Status** — Active or Inactive; every new task starts as Active. Inactive retires a task without deleting it.
- **Required tag groups** — tag groups from which every time entry on this task must carry at least one tag ([time entry](time-entry.md#rules-applied-when-editing)). They are meant to be groups of the task's own project: the editor offers only those, but a save does not check it.
- Shown as `[<project>] <name>`.

## Bookable tasks

A task is bookable for a user when the task is Active, its project is Open, and the user [participates](project-participant.md) in the project. Only bookable tasks are offered when a task is chosen for a time entry, and only they are listed in My tasks, ordered by project name and then task name. The rule applies when a task is chosen, not on save: time entries already logged on a task that later turns Inactive, or whose project closes, stay as they are.

## Deletion

Deleting a task leaves the time entries on it in place, still referring to it.

## Not covered

- The Active and Open conditions of the bookable rule: the test checks participation only.
- The required tag groups belonging to the task's project: not checked anywhere but the editor.
