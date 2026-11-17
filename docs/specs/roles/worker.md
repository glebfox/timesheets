# Worker

Source: `security/WorkerRole.java`, `demo/DemoDataLoader.java`

Resource role `worker`, named "Worker", a job role: a person who logs their own time. It includes [UI: minimal access](ui-minimal.md), [Time logging](time-logging.md), and [Filters: own configurations](own-filters.md), and grants nothing besides.

## Grants

- Signing in, the main view, and the [dashboard](../views/dashboard.md), with My time, Log time, and Copy to today.
- Logging one's own time: the [calendar](../views/calendar.md), My time entries ([time entry screens](../views/time-entries.md#lists)), and [My tasks](../views/tasks.md#my-tasks), with their Timesheets menu items, and the time entry editor, the task picker, and the task editor they open.
- In a list's filter, adding conditions and saving one's own configurations.

The menu offers nothing else: no clients, projects, Time entries list, approval screen, settings, users, absences, or administration.

## Row-level role

[Project scope](project-scope.md), assigned beside the job role ([job roles and their row-level role](README.md#job-roles-and-their-row-level-role)); [Time entry rules](time-entry-rules.md) instead when the person also holds Project office, and none with Full access. With Project scope a holder logs time only on the projects where they take part in a role other than Observer, writes only their own time entries, by the time entry rules, and reads only what Project scope opens to them by their project roles. No screen lists other people's entries to them, but the time entry editor opened by its URL, `time-entries/:id`, shows any entry Project scope opens, read only: every entry of the projects where they are a Manager, an Approver, or an Observer ([Time logging](time-logging.md#grants)).

Without a row-level role the holder is unrestricted: they read every project and every time entry, and change and remove anyone's entries in any status, Closed ones included. Nothing assigns it by itself, and no screen shows that it is missing.

## Holders

- The demo users `freeman`, `barney`, `rosenberg`, `gina`, `colette`, `alyx`, `odessa`, `shephard`, `rattmann`, and `chell`, in the `dev` demo data only, each with [Project scope](project-scope.md).

[Approver](approver.md), [HR](hr.md), and [Project office](project-office.md) include it, and through Approver [Manager](manager.md), so in the `dev` demo data every user but `admin` and `gman` holds it.

## Not covered

- The menu items a holder opens and those they do not, signing in with the job role alone, and creating time entries, for a holder with Project scope: `JobRolesTest`. What each included role grants is checked by that role's own tests.
- The calendar and My time entries opened by a holder with Project scope, each showing the holder's entry and not a project mate's of the same day: `JobRolesUiTest`. My tasks, the task picker, and the editors are not opened as a holder.
- A holder without a row-level role, or with another one: not checked.
