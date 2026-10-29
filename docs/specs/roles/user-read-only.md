# User: read only

Source: `security/UserReadOnlyRole.java`, `demo/DemoDataLoader.java`

Resource role `user-read-only`, named "User: read only".

## Grants

- Reading [users](../data-model/user.md), with every attribute view-only.
- The user list and the user editor, and the Administration → Users menu item.

It grants nothing else: no signing in, no creating, changing, or deleting users, and no security screen. A user who has only this role and [UI: minimal access](ui-minimal.md) can open users in the [user list](../views/users.md#users) but not create or remove them, does not see Role assignments, Change password, Reset password, or User substitutions there, and gets the user editor read-only, with an empty Work schedule section, since the role does not read assignments ([users](../views/users.md#work-schedule)). It is meant to be included by a role that shows or picks users, as [Work schedule management](work-schedule-management.md) includes it.

## Holders

None directly. The demo user `mossman` gets it through [Work schedule management](work-schedule-management.md), in the `dev` demo data only.

## Not covered

- The role on its own, including the empty Work schedule section: the tests check the role only as part of Work schedule management.
