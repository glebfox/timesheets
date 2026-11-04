# Absence management

Source: `security/AbsenceManagementRole.java`, `demo/DemoDataLoader.java`

Resource role `absence-management`, named "Absence management". It includes [User: read only](user-read-only.md).

## Grants

- Every operation on [absences](../data-model/absence.md) and [absence types](../data-model/absence-type.md), with every attribute modifiable.
- Their screens: the absence list and editor ([absences](../views/absences.md)) and the absence type list and editor ([settings](../views/settings.md#absence-types)); the Administration → Absences and Settings → Absence types menu items.
- From User: read only, reading users in the user list and the user editor, which the absence editor's user field and its picker need.

It grants no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md). Apart from the user list and the user editor, it opens no other screen under Settings or Administration.

A user needs no permission on absences to see their own: the [calendar](../views/calendar.md#absences) shows them to every user, whatever the user may read.

## Holders

- The demo user `mossman`, in the `dev` demo data only.

## Not covered

- Saving as a holder: the UI test records an absence in the editor after opening and closing both pickers, and the other tests check permissions. Saving an absence type, and editing or removing an absence or a type, as a holder: no test.
- The menu as a holder sees it: the tests check only that the Absences and Absence types items are permitted to a holder and Work schedules is not. Checked live on 2026-11-04 as `mossman`, who saw both items and recorded an absence with both pickers.
