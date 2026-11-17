# Project role

Source: `entity/project/ProjectRole.java`, `entity/project/ProjectRoleType.java`, `app/TimeEntryApprovalSupport.java`

A catalog entry naming the role a participant plays in a project, such as "Developer" or "Account manager".

## Attributes

- **Name** — required; unique among non-deleted roles (`docs/guidelines/database.md`).
- **Type** — required: Manager, Approver, Member, or Observer. Manager and Approver decide which [time entries](time-entry.md) a participant approves on the project ([approval screen](../views/approval.md#which-entries-it-shows)) and whom the dashboard's My team shows ([dashboard](../views/dashboard.md#the-people)). A participant of type Observer follows the project without working on it: the project's tasks are not [bookable](task.md#bookable-tasks) for them, and they are on no one's My team through it. For a holder of the row-level role [Project scope](../roles/project-scope.md), the type also decides what they read and change on the project: every type opens the project's data to reading, Manager, Approver, and Observer every time entry logged on it, and Manager changing its data. The type also colors the role's badge on screens.
- **Description** — optional.

## Deletion

Deleting a role leaves the participants that have it in place, but a deleted role of type Manager or Approver grants nothing: its participants approve nothing and do not count as Managers when the entries of an Approver are decided. Under [Project scope](../roles/project-scope.md), a participant whose role is deleted still reads the project's data, but of its time entries only their own, and changes nothing.
