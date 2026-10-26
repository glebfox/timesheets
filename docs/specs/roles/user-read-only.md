# User: read only

Source: `security/UserReadOnlyRole.java`

Resource role `user-read-only`, named "User: read only".

## Grants

- Reading [users](../data-model/user.md), with every attribute view-only.
- The user list and the user editor, and the Administration → Users menu item.

It grants nothing else: no signing in, no creating, changing, or deleting users, and no security screen. A user who has only this role and [UI: minimal access](ui-minimal.md) can open users in the [user list](../views/users.md#users) but not create or remove them, does not see Role assignments, Change password, Reset password, or User substitutions there, and gets the user editor read-only. It is meant to be included by a role that shows or picks users, as [Work schedule management](work-schedule-management.md) includes it.

## Holders

None in code or seed data.

## Not covered

- The role on its own: the tests check it only as part of Work schedule management.
