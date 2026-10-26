# 0003 Work schedule management grants no update on users

Status: accepted

## Context

A holder of Work schedule management assigns users to work schedules, and the user editor is one of the two places where a user's assignments are edited. The simplest setup grants the update permission on users with every user attribute view-only: the user editor then opens editable, and only its assignment table can change anything.

In Jmix 3.1.0 the user list's Change password and Reset password actions check only the entity-level permission to update users, and UI: minimal access, the role users sign in with, grants its holders the change password screen, meant for one's own password. With the update permission on users, a holder could change any user's password from the user list. Roles add up, so no other role could take that back.

Alternatives considered and rejected:

- **The update permission on users, with every user attribute view-only.** The user editor opens editable, but the holder can change any user's password, as above.

## Decision

Work schedule management grants nothing on users beyond what it includes from User: read only: reading users, with every attribute view-only. Decided by the user.

## Consequences

- The user editor opens read-only for a holder. Its assignment actions are the project's own and do not follow the view's read-only mode, so they stay enabled as far as the holder's permissions on assignments allow, and OK saves only the assignment changes (`docs/guidelines/views.md`).
- In the user list, Edit reads as Read for a holder.
- The user list hides Change password and Reset password from any user who may not update users, whatever screens the user may open.
- Any role that grants the update permission on users lets those of its holders who also hold UI: minimal access change every user's password from the user list. A role that must change some user attributes, but not passwords, needs the password actions to check more than that permission first.
