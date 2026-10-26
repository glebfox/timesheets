# Work schedule management

Source: `security/WorkScheduleManagementRole.java`

Resource role `work-schedule-management`, named "Work schedule management". It includes [User: read only](user-read-only.md).

## Grants

- Every operation on [work schedules](../data-model/work-schedule.md), [holiday calendars](../data-model/holiday-calendar.md) and their days, and [work schedule assignments](../data-model/work-schedule-assignment.md), with every attribute modifiable.
- Their screens: the work schedule list and editor, the default deletion dialog, the holiday calendar list and editor, the calendar days dialog, the day editor, the assignment editor, and the add users dialog ([settings](../views/settings.md)); the Settings → Work schedules and Holiday calendars menu items.
- From User: read only, reading users in the user list and the user editor.

It grants no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## Users stay read only

The role grants nothing on users beyond reading them, so a holder cannot create or delete users, change their attributes or passwords, assign roles, or set up substitutions.

- The [user editor](../views/users.md#user-editor) opens read-only for a holder, but its Work schedule section stays editable, and OK saves only the assignment changes.
- The [user list](../views/users.md#users) hides Role assignments, Change password, Reset password, and User substitutions from a holder, and the gear menu with them.

The role has no update permission on users because of Change password. The user list's Change password checks only the permission to edit users, and UI: minimal access lets its holders open the change password screen, for their own password. With update permission on users, a holder could change any user's password, and roles add up, so no other role could take that back. For the same reason the user list hides Change password and Reset password from a user who may not edit users, whatever screens the user may open.

## Holders

None in code or seed data.

## Not covered

- Saving as a holder: the UI tests save assignments in a read-only user editor and only open the other screens and dialogs. Saving a schedule with added users, saving a calendar, and Make default were checked live; Delete in the default deletion dialog was not.
- A holder opening a new user's editor by its URL, `users/new`: the editor opens, and OK is refused with "Access denied", creating nothing. Checked live only.
- The menu items a holder sees, and Edit in the user list reading as Read: checked live only.
