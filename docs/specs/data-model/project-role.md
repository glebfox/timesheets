# Project role

Source: `entity/project/ProjectRole.java`, `entity/project/ProjectRoleType.java`, `app/TimeEntryApprovalSupport.java`, `app/ProjectAccessSupport.java`

A catalog entry naming the role a participant plays in a project, such as "Developer" or "Account manager".

## Attributes

- **Name** — required; unique among non-deleted roles (`docs/guidelines/database.md`).
- **Type** — required: Manager, Approver, Member, or Observer. Manager and Approver decide which [time entries](time-entry.md) a participant approves on the project ([approval screen](../views/approval.md#which-entries-it-shows)) and whom the dashboard's My team shows ([dashboard](../views/dashboard.md#the-people)). A participant of type Observer follows the project without working on it: the project's tasks are not [bookable](task.md#bookable-tasks) for them, and they are on no one's My team through it. For a holder of the row-level role [Project scope](../roles/project-scope.md), the type also decides what they read and change on the project: every type opens the project's data to reading, Manager, Approver, and Observer every time entry logged on it, and Manager changing its data; what each type unlocks, and the job role a person needs to act on it, is in the [security roles](../roles/README.md#project-roles-on-a-project). The type also colors the role's badge on screens.
- **Description** — optional.

## Deletion

Deleting a role leaves the participants that have it in place, but a deleted role grants nothing, whatever its type: its participants approve nothing and do not count as Managers when the entries of an Approver are decided; the project's tasks are not [bookable](task.md#bookable-tasks) for them; they are on no one's [My team](../views/dashboard.md#the-people) through it; and the [time entry rules](../roles/time-entry-rules.md) do not count them as taking part in the project, so they create no entry there and move none to its tasks. Under [Project scope](../roles/project-scope.md), a participant whose role is deleted still reads the project's data, but of its time entries only their own, and changes nothing.

## Not covered

- A participant whose role is deleted: that the deleted role of an Approver grants no approving and no team is checked (`TimeEntryApprovalSupportTest`, `TeamDashboardSupportTest`). That the project's tasks are not bookable for such a participant, that they are on no one's My team, that the time entry rules do not count them as taking part, and what Project scope lets them read: read from the queries, no test.
