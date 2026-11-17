# Time logging

Source: `security/TimeLoggingRole.java`, `demo/DemoDataLoader.java`

Resource role `time-logging`, named "Time logging".

## Grants

- [Time entries](../data-model/time-entry.md): creating, reading, changing, and removing them, with the date, task, spent time, description, and tags modifiable and the user, status, and rejection reason view-only, on a new entry too. Every screen that creates an own entry sets its user itself, so the [time entry editor](../views/time-entries.md#own-and-general-entries) shows that user read only, and so does the editor opened by its URL on a new entry, `time-entries/new`: it sets the holder as the user, as for an own entry, since a holder may not choose one. The status and the rejection reason are not the author's to set.
- Reading what these screens show, every attribute view-only: [tasks](../data-model/task.md), [projects](../data-model/project.md), [project participants](../data-model/project-participant.md), [project roles](../data-model/project-role.md), [tag groups and tags](../data-model/tag-group.md), [task types](../data-model/task-type.md), and [users](../data-model/user.md).
- The [calendar](../views/calendar.md), My time entries ([time entry screens](../views/time-entries.md#lists)), and [My tasks](../views/tasks.md#my-tasks), with their Timesheets menu items. The [time entry editor](../views/time-entries.md#editor), the [task picker](../views/tasks.md#task-picker), and the [task editor](../views/tasks.md#task-editor) have no menu item: those screens open them. The two editors also open by their URLs, `time-entries/:id` and `tasks/:id`; the task editor opens read-only, as the role does not let a holder change tasks.
- On the [dashboard](../views/dashboard.md), My time, with Copy to today in its Recent entries, and Log time, which show to every user who may create time entries.

It grants no Time entries list of everyone's entries, no project screens, and no user screens: those are other roles'. The time entry editor opened by its URL, `time-entries/:id`, still shows any entry the holder's row-level role lets them read, other people's included, read only unless they may change it. It grants no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

The attribute policies bind the screens, not the data layer, so only the screens keep a holder from changing an entry's user, status, or rejection reason: code running as a holder could save them.

## What it does not decide

The role grants every operation on every entry and reading every task, project, and user. Whose entries a holder writes, in which status, on which tasks, and which project data they read is decided by a row-level role: [Time entry rules](time-entry-rules.md), or [Project scope](project-scope.md), which includes it. A holder without one changes and removes anyone's entries, in any status. The screens narrow only what they show: My time entries and the calendar show the holder's own entries, and My tasks and the task picker the tasks [bookable](../data-model/task.md#bookable-tasks) for the holder.

## Holders

None directly. [Worker](worker.md) includes it, and through Worker every job role but Observer, so in the `dev` demo data every user but `admin` and `gman` holds it.

## Not covered

- The screens as a holder sees them: the UI test opens only the time entry editor. On a new own entry it sees the user set to the holder and read only and the date, task, spent time, and description editable, and saves the entry with a tag of the task's project; on a new entry opened by the editor's URL, `time-entries/new`, it sees the user set to the holder and read only, also after a task is chosen and cleared. The calendar, My time entries, My tasks, the task picker, and the task editor are checked only as views the holder may open, and the three menu items as items the holder may open.
- A holder who lacks UI: minimal access: not checked.
- A change of an entry's user, status, or rejection reason by code running as a holder: no test.
