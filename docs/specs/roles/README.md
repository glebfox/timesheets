# Security roles

Source: `security/`

Access control is Jmix security roles only. The code defines two resource roles and one row-level role, each with its own spec here:

- [Full access](system-full-access.md) — everything.
- [UI: minimal access](ui-minimal.md) — signing in and the main view, nothing else.
- [Edit active time entries](edit-active-time-entries.md) — a row-level role that forbids changing Approved and Closed time entries.

Further roles of either kind can be created at runtime in the Security screens and assigned to users with Role assignments ([users](../views/users.md)). A user's roles are role assignments, not attributes of the user ([user](../data-model/user.md#access)).

Project roles and their types (Manager, Approver, Member, Observer) are project data, not security roles: they grant no permissions ([project role](../data-model/project-role.md)).
