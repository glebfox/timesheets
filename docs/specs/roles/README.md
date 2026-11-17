# Security roles

Source: `security/`

Access control is Jmix security roles only. The code defines fourteen resource roles and three row-level roles, each with its own spec here:

- [Full access](system-full-access.md) — everything.
- [UI: minimal access](ui-minimal.md) — signing in and the main view, nothing else.
- [User: read only](user-read-only.md) — reading users in the user list and the user editor.
- [Work schedule management](work-schedule-management.md) — work schedules, holiday calendars, and who works to which schedule from which date; it includes User: read only.
- [Absence management](absence-management.md) — absences and absence types; it includes User: read only.
- [Time logging](time-logging.md) — logging one's own time: the calendar, My time entries, My tasks, and the time entry editor; creating, changing, and removing time entries, whose user, status, and rejection reason stay view-only; and reading what these screens show.
- [Time entries: approval](time-entry-approval.md) — the approval screen and changing the status and rejection reason of time entries; it includes User: read only.
- [Time entries: read only](time-entry-read.md) — the Time entries list and the time entry editor, read only, with what they show of an entry.
- [Projects: read only](project-read.md) — the project list and the project editor with its dialogs, read only, with what they show: participants and their project roles, tasks and their types, tag groups, tags, and the project's client.
- [Projects: management](project-management.md) — changing projects, and creating, changing, and removing their tasks, participants, tag groups, and tags, with the lists the editors' fields pick from, not creating or removing projects; it includes Projects: read only.
- [Projects: create and remove](project-creation.md) — creating and removing projects, with no screen of its own.
- [Client management](client-management.md) — the client list and the client editor.
- [Task types and project roles](project-catalogs.md) — the task type and project role lists and editors.
- [Filters: own configurations](own-filters.md) — adding conditions to a list's filter and saving one's own filter configurations, not configurations for everybody and not JPQL conditions.
- [Time entry rules](time-entry-rules.md) — a row-level role with the write rules of time entries: who creates, changes, and removes an entry, in which status, and which of its attributes.
- [Project scope](project-scope.md) — a row-level role that narrows project data and time entries to the projects where the person takes part, and lets them change project data only where they are the project's Manager; it includes Time entry rules.
- [Client scope](client-scope.md) — a row-level role for a project's client: Project scope, and users and clients narrowed to those of the projects where the person takes part.

Apart from Full access, the roles are granular and meant to be combined: UI: minimal access to sign in, plus the roles for the data and screens a user works with (`docs/conventions.md`).

Further roles of either kind can be created at runtime in the Security screens and assigned to users with Role assignments ([users](../views/users.md)). A user's roles are role assignments, not attributes of the user ([user](../data-model/user.md#access)).

Project roles and their types (Manager, Approver, Member, Observer) are project data, not security roles: they grant no permissions. They decide whose time entries a user approves on a project, on a screen that a security role such as [Time entries: approval](time-entry-approval.md) lets the user open, and whom the [dashboard](../views/dashboard.md#my-team)'s My team shows that user ([project role](../data-model/project-role.md), [approval](../views/approval.md#which-entries-it-shows)). For a holder of [Project scope](project-scope.md) or [Client scope](client-scope.md), they also narrow what the user's security roles grant to the projects where the user takes part.
