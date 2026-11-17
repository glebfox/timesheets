# Projects: create and remove

Source: `security/ProjectCreationRole.java`

Resource role `project-creation`, named "Projects: create and remove".

## Grants

- [Projects](../data-model/project.md): creating, reading, and removing them, every attribute modifiable on a new project. Changing a saved project is not granted: [Projects: management](project-management.md) grants it.

It opens no screen: a holder creates and removes projects in the [project list](../views/projects.md#projects) and the [project editor](../views/projects.md#project-editor), which [Projects: read only](project-read.md) or Projects: management opens, and whose Create and Remove this role enables. It grants no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## What it does not decide

[Project scope](project-scope.md) does not narrow creating and removing projects: whoever holds this role creates projects and removes those they read.

## Holders

None in code or seed data.

## Not covered

- The grants: `ProjectBlocksRoleTest`, with the UI's entity, attribute, view, and menu contexts. Creating and removing a project in the screens as a holder: no test.
- A holder who lacks UI: minimal access: not checked.
