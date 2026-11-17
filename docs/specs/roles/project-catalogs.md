# Task types and project roles

Source: `security/ProjectCatalogsRole.java`

Resource role `project-catalogs`, named "Task types and project roles".

## Grants

- [Task types](../data-model/task-type.md) and [project roles](../data-model/project-role.md): creating, reading, changing, and removing them, every attribute modifiable.
- The [task type list](../views/settings.md#task-types) and the [project role list](../views/settings.md#project-roles) with their Settings menu items, and their editors.
- In the [participant editor](../views/projects.md#participant-editor), which another role opens, the role field's Create, which shows only to a user who may create project roles.

It grants no project screens and no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

A project role's type decides what [Project scope](project-scope.md) lets the people with that role do on their projects, so a holder who changes a role's type changes it for everyone who has the role.

## Holders

None in code or seed data.

## Not covered

- The screens as a holder sees them: `ProjectBlocksRoleTest` checks the grants with the UI's entity, attribute, view, and menu contexts, and no UI test opens the lists or editors as a holder. The participant editor's Create on the role field is checked for the system user (`ProjectParticipantDetailViewUiTest`), not for a holder of this role.
- A holder who lacks UI: minimal access: not checked.
