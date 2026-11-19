# HR

Source: `security/HrRole.java`, `demo/DemoDataLoader.java`

Resource role `hr`, named "HR", a job role: a person who keeps work schedules and absences and logs their own time. It includes [Worker](worker.md), [Work schedule management](work-schedule-management.md), and [Absence management](absence-management.md), and through the last two [User: read only](user-read-only.md), and grants nothing besides.

## Grants

- Everything [Worker](worker.md) grants.
- Work schedules, holiday calendars, and who works to which schedule from which date, with their screens and their Settings menu items ([settings](../views/settings.md)).
- Absences and absence types, with their screens and menu items ([absences](../views/absences.md), [absence types](../views/settings.md#absence-types)).
- The [user list](../views/users.md#users) and the user editor, read only, with the menu item; the editor's work schedule assignments still change there.

No project screens, no Time entries list, and no approval screen.

## Row-level role

[Project scope](project-scope.md), assigned beside the job role ([job roles and their row-level role](README.md#job-roles-and-their-row-level-role)); [Time entry rules](time-entry-rules.md) instead when the person also holds Project office, and none with Full access. Project scope leaves people, work schedules, holiday calendars, and absences open, so a holder keeps them for everyone, while time entries are narrowed as for a [Worker](worker.md#row-level-role), the time entry editor opened by its URL included.

Without a row-level role the holder is unrestricted, as a Worker without one is: they read every project and every time entry, and change and remove anyone's entries in any status, Closed ones included. Nothing assigns it by itself, and no screen shows that it is missing.

## Holders

- The demo user `mossman`, in the `dev` demo data only, with [Approver](approver.md) and [Project scope](project-scope.md).

## Not covered

- The menu items a holder opens and those they do not, signing in with the job role alone, and creating time entries, for a holder with Project scope: `JobRolesTest`, which also checks that a holder reads a person they share no project with, may open the user list and editor, and may neither change, remove, nor create a person. What each included role grants is checked by that role's own tests.
- No test opens a screen as a holder.
- A holder without a row-level role, or with another one: not checked.
