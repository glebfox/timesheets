# Project role

Source: `entity/project/ProjectRole.java`, `entity/project/ProjectRoleType.java`, `app/TimeEntryApprovalSupport.java`

A catalog entry naming the role a participant plays in a project, such as "Developer" or "Account manager".

## Attributes

- **Name** — required; unique among non-deleted roles (`docs/guidelines/database.md`).
- **Type** — required: Manager, Approver, Member, or Observer. Manager and Approver decide which [time entries](time-entry.md) a participant approves on the project ([approval screen](../views/approval.md#which-entries-it-shows)); Member and Observer have no behavior. The type also colors the role's badge on screens.
- **Description** — optional.

## Deletion

Deleting a role leaves the participants that have it in place, but a deleted role of type Manager or Approver grants nothing: its participants approve nothing and do not count as Managers when the entries of an Approver are decided.
