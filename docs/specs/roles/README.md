# Security roles

Source: `security/`

Access control is Jmix security roles only. The code defines four resource roles and one row-level role, each with its own spec here:

- [Full access](system-full-access.md) — everything.
- [UI: minimal access](ui-minimal.md) — signing in and the main view, nothing else.
- [User: read only](user-read-only.md) — reading users in the user list and the user editor.
- [Work schedule management](work-schedule-management.md) — work schedules, holiday calendars, and who works to which schedule from which date; it includes User: read only.
- [Edit active time entries](edit-active-time-entries.md) — a row-level role that forbids changing Approved and Closed time entries.

Apart from Full access, the roles are granular and meant to be combined: UI: minimal access to sign in, plus the roles for the data and screens a user works with (`docs/conventions.md`).

Further roles of either kind can be created at runtime in the Security screens and assigned to users with Role assignments ([users](../views/users.md)). A user's roles are role assignments, not attributes of the user ([user](../data-model/user.md#access)).

Project roles and their types (Manager, Approver, Member, Observer) are project data, not security roles: they grant no permissions ([project role](../data-model/project-role.md)).
