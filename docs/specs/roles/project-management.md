# Projects: management

Source: `security/ProjectManagementRole.java`, `view/projectparticipant/ProjectParticipantDetailView.java`

Resource role `project-management`, named "Projects: management". It includes [Projects: read only](project-read.md).

## Grants

- [Projects](../data-model/project.md): changing them, every attribute modifiable, the client included. Creating and removing projects are not granted: [Projects: create and remove](project-creation.md) grants them.
- [Tasks](../data-model/task.md), [project participants](../data-model/project-participant.md), and [tag groups and tags](../data-model/tag-group.md): creating, changing, and removing them, every attribute modifiable.
- From Projects: read only, the project list and its menu item, the project editor and the dialogs it opens, and reading the project roles, task types, clients, and users they show.
- The lists that the editors' fields pick from, with no menu item: the [client list](../views/clients.md#clients) for the project's client, the [user list](../views/users.md#users) for a participant's user and for Add participants, the [project role list](../views/settings.md#project-roles) for a participant's role, the [task type list](../views/settings.md#task-types) for a task's type, and Select tag groups ([tag screens](../views/tags.md#pickers)) for a task's required tag groups. Tabbed mode also opens each of these lists by its URL, so a holder reaches them that way, though the menu does not list them. What they list stays read-only: their editors are not granted, so a holder creates, changes, and opens no client, user, project role, or task type there, and the lists offer no Edit to a holder who lacks the roles that grant those editors.

So the [participant editor](../views/projects.md#participant-editor)'s role field offers no Create to a holder: they pick one of the existing project roles, which are [Task types and project roles](project-catalogs.md)' to create.

The attribute policies bind the screens, not the data layer. The role grants no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## What it does not decide

The role grants changing every project and its data. Which projects a holder changes is decided by a row-level role: [Project scope](project-scope.md) lets them change only the projects where they are the Manager, and the project editor opens read-only on the other projects where they take part. A holder without one changes every project.

## Holders

None in code or seed data.

## Not covered

- The grants: `ProjectBlocksRoleTest`, with the UI's entity, attribute, view, and menu contexts.
- The screens, as `ProjectManagementRoleUiTest` opens them for a holder who also has UI: minimal access and Project scope: the editor of a project they manage opens editable and saves a task added in its dialog; the editor of a project where they are an Approver opens read-only, with the Tasks tab's Create, Remove, and Close and the Participants tab's Add participants disabled, while Edit, labeled Read, opens a task read-only, is enabled on a participant, and opens a tag group read-only, whose Edit opens a tag read-only; in the managed project's editor, Edit, labeled Edit, opens a task editable, and Close closes it, saved with the project; and in the participant editor the role field offers no Create, while its picker and the user field's open the project role list and the user list. Each of the four pickers, opened from the editors' fields, offers no Edit on a selected row, while for a holder who also has Client management, Task types and project roles, and User: read only it opens the client, task type, project role, and user editor. Select tag groups is checked only as a view the holder may open; the participant and tag group dialogs' saves, and the Read label anywhere but on tasks, are not checked.
- Opening the pickers' lists by their URLs: checked only as views the holder may open, and their menu items as items the holder may not.
- A holder who lacks UI: minimal access: not checked.
