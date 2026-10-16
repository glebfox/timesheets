# Security roles

Source: `security/`

Access control is Jmix security roles only. The code defines two resource roles, each with its own spec here, and no row-level roles:

- [Full access](system-full-access.md) — everything.
- [UI: minimal access](ui-minimal.md) — signing in and the main view, nothing else.

Further roles of either kind can be created at runtime in the Security screens and assigned to users with Role assignments ([users](../views/users.md)). A user's roles are role assignments, not attributes of the user ([user](../data-model/user.md#access)).

Project roles and their types (Manager, Approver, Member, Observer) are project data, not security roles: they grant no permissions ([project role](../data-model/project-role.md)).
