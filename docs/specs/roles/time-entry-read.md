# Time entries: read only

Source: `security/TimeEntryReadOnlyRole.java`

Resource role `time-entry-read`, named "Time entries: read only".

## Grants

- [Time entries](../data-model/time-entry.md): reading them, every attribute view-only. Creating, changing, and removing them are not granted.
- Reading what the time entry screens show of an entry, every attribute view-only: its [task](../data-model/task.md), [project](../data-model/project.md), [tags and tag groups](../data-model/tag-group.md), and [user](../data-model/user.md).
- The Time entries list ([time entry screens](../views/time-entries.md#lists)) and its Project Management menu item, and the [time entry editor](../views/time-entries.md#editor), with no menu item, which opens read-only; it also opens by its URL, `time-entries/:id`.

It grants no My time entries, calendar, or My tasks: logging one's own time is [Time logging](time-logging.md)'s. It grants no project or user screens and no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## What it does not decide

Which entries a holder reads is decided by a row-level role: [Project scope](project-scope.md) narrows them to the holder's own and every entry of the projects where they are a Manager, an Approver, or an Observer. A holder without one reads every entry.

## Holders

None in code or seed data.

## Not covered

- The screens as a holder sees them: `ProjectBlocksRoleTest` checks the grants with the UI's entity, attribute, view, and menu contexts, and no UI test opens the Time entries list or the editor as a holder.
- A holder who lacks UI: minimal access: not checked.
