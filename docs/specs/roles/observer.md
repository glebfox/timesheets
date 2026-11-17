# Observer

Source: `security/ObserverRole.java`

Resource role `observer`, named "Observer", a job role: a person who follows projects without working on them, such as a project's client. It includes [UI: minimal access](ui-minimal.md), [Projects: read only](project-read.md), [Time entries: read only](time-entry-read.md), and [Filters: own configurations](own-filters.md), and grants nothing besides.

## Grants

- Signing in, the main view, and the [dashboard](../views/dashboard.md)'s header: its My time is for a user who logs time, and its My team for one who approves.
- The [project list](../views/projects.md#projects) and its menu item, and the project editor with its dialogs, read only, with what they show: the people taking part and their project roles, the tasks and their types, the tag groups and tags, and the project's client.
- The Time entries list ([time entry screens](../views/time-entries.md#lists)) and its menu item, and the time entry editor, read only.
- In a list's filter, adding conditions and saving one's own configurations.

It logs no time: no calendar, My time entries, or My tasks, and no creating time entries. It has no Users screen, though the project screens show the people taking part, and no client screen.

## Row-level role

[Client scope](client-scope.md), assigned beside the job role, when Observer is the person's only job role; [Project scope](project-scope.md) when they also hold Worker, Approver, Manager, or HR; [Time entry rules](time-entry-rules.md) with Project office; and none with Full access ([job roles and their row-level role](README.md#job-roles-and-their-row-level-role)). With Client scope a holder reads only the projects where they take part and their data, every time entry of the projects where they are an Observer, and only the people and the clients of those projects.

Without a row-level role the holder reads every project, every time entry, and every person and client the screens show, though they change nothing, as the role grants no change. Nothing assigns it by itself, and no screen shows that it is missing.

## Holders

None in code or seed data.

## Not covered

- The menu items a holder opens and those they do not, signing in with the job role alone, and creating no time entries, for a holder with Client scope: `JobRolesTest`, which also checks that a holder's save of their own entry is refused and that they read a person taking part in the project they observe but may open neither the user list nor the user editor. What each included role grants is checked by that role's own tests.
- The project list opened by a holder with Client scope, listing the observed project and not another, and the observed project's editor opening read-only: `JobRolesUiTest`. The Time entries list and the editor's dialogs are not opened as a holder.
- A holder without a row-level role, or with another one: not checked.
