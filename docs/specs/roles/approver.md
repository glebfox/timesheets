# Approver

Source: `security/ApproverRole.java`, `demo/DemoDataLoader.java`

Resource role `approver`, named "Approver", a job role: a person who logs their own time and reviews the time logged on their projects. It includes [Worker](worker.md) and [Time entries: approval](time-entry-approval.md), and through it [User: read only](user-read-only.md), and grants nothing besides.

## Grants

- Everything [Worker](worker.md) grants.
- The [approval screen](../views/approval.md) and its menu item: approving and rejecting the entries the approval rule lets the holder review, and the [dashboard](../views/dashboard.md#my-team)'s My team where they approve.
- The [user list](../views/users.md#users) and the user editor, read only, with the menu item.

No project screens and no Time entries list: the approval screen offers the projects the holder approves on.

On the holder's own New or Rejected entry, which the [time entry editor](../views/time-entries.md#editor) opens for changes, the editor's Status field is editable, as Time entries: approval lets a holder change an entry's status. A save that changes it is refused with an access-denied error: the approval rule does not let an Approver review their own entries.

## Row-level role

[Project scope](project-scope.md), assigned beside the job role ([job roles and their row-level role](README.md#job-roles-and-their-row-level-role)); [Time entry rules](time-entry-rules.md) instead when the person also holds Project office, and none with Full access. With Project scope a holder reads the entries of the projects where they are an Approver or a Manager, and approves and rejects them as the time entry rules let a reviewer; otherwise they work as a [Worker](worker.md#row-level-role) does.

Without a row-level role the holder is unrestricted: the approval screen still shows only what the approval rule names, but they read every project and every time entry, and change and remove anyone's entries in any status, Closed ones included. Nothing assigns it by itself, and no screen shows that it is missing.

## Holders

- The demo user `mossman`, in the `dev` demo data only, with [HR](hr.md) and [Project scope](project-scope.md).

[Manager](manager.md) includes it, so the demo Managers `kleiner`, `eli`, `magnusson`, `cave`, and `caroline` hold it too.

## Not covered

- The menu items a holder opens and those they do not, signing in with the job role alone, and creating time entries, for a holder with Project scope: `JobRolesTest`. What each included role grants is checked by that role's own tests.
- A holder of Project office and Approver with Time entry rules, who reads every project and approves only by the approval rule: `JobRolesTest`, through the approval support bean. No test opens the approval screen as an Approver; `JobRolesUiTest` opens it as a Manager.
- The Status field on the holder's own entry, editable and refused on save: no test; seen once in a UI test written to look and not kept.
- A holder without a row-level role, or with another one: not checked.
