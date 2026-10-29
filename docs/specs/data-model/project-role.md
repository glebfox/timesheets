# Project role

Source: `entity/project/ProjectRole.java`, `entity/project/ProjectRoleType.java`

A catalog entry naming the role a participant plays in a project, such as "Developer" or "Account manager".

## Attributes

- **Name** — required; unique among non-deleted roles (`docs/guidelines/database.md`).
- **Type** — required: Manager, Approver, Member, or Observer. The type has no behavior: it only colors the role's badge on screens.
- **Description** — optional.

## Deletion

Deleting a role leaves the participants that have it in place.
