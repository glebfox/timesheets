# Projects: read only

Source: `security/ProjectReadOnlyRole.java`, `demo/DemoDataLoader.java`

Resource role `project-read`, named "Projects: read only".

## Grants

- [Projects](../data-model/project.md), their [tasks](../data-model/task.md), [participants](../data-model/project-participant.md), and [tag groups and tags](../data-model/tag-group.md): reading them, every attribute view-only.
- Reading what the project screens show with them, every attribute view-only: the participants' [project roles](../data-model/project-role.md) and [users](../data-model/user.md), the tasks' [types](../data-model/task-type.md), and the project's [client](../data-model/client.md).
- The [project list](../views/projects.md#projects) and its Project Management menu item, and the [project editor](../views/projects.md#project-editor), which opens read-only, as the role lets a holder change nothing; there Edit opens each task, participant, and tag group read-only, and a tag group's editor opens each of its tags read-only. The dialogs the editor opens have no menu item: the [task editor](../views/tasks.md#task-editor), the [participant editor](../views/projects.md#participant-editor), the [tag group editor](../views/tags.md#tag-group-editor), and the [tag editor](../views/tags.md#tag-editor). Each editor also opens by its URL, read-only.

It grants no user, client, task type, or project role screen: users and clients are read for what the project screens show of them, and the lists the editors' fields pick from are [Projects: management](project-management.md)'s. It grants no time entries and no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## What it does not decide

Which projects a holder reads is decided by a row-level role: [Project scope](project-scope.md) narrows them to the projects where the holder takes part, and [Client scope](client-scope.md) also narrows users and clients. A holder without one reads every project.

## Holders

None directly. [Projects: management](project-management.md) and [Observer](observer.md) include it, and through Projects: management [Manager](manager.md) and [Project office](project-office.md), so in the `dev` demo data the Managers `kleiner`, `eli`, `magnusson`, `cave`, and `caroline` and `gman` hold it.

## Not covered

- The screens as a holder of this role alone sees them: `ProjectBlocksRoleTest` checks the grants with the UI's entity, attribute, view, and menu contexts, and no UI test opens a screen as such a holder. The project editor opening read-only on a project its holder does not manage, and its children opening read-only from it, are checked for Projects: management with Project scope (`ProjectManagementRoleUiTest`).
- A holder who lacks UI: minimal access: not checked.
