# Time entries: approval

Source: `security/TimeEntryApprovalRole.java`, `demo/DemoDataLoader.java`

Resource role `time-entry-approval`, named "Time entries: approval". It includes [User: read only](user-read-only.md), as the screen shows people.

## Grants

- [Time entries](../data-model/time-entry.md): reading them, every attribute view-only except the status and the rejection reason, which a holder may change. Creating and deleting are not granted.
- Reading what the approval screen shows of an entry, every attribute view-only: [tasks](../data-model/task.md), [projects](../data-model/project.md), [project participants](../data-model/project-participant.md), [project roles](../data-model/project-role.md), [tags](../data-model/tag-group.md), and tag groups. Users come from User: read only.
- The [approval screen](../views/approval.md) and its menu item, and, from User: read only, the user list and the user editor with their menu item.

The attribute policies bind the screens, not the data layer, so only the screens limit a holder to changing those two attributes: code running as a holder could save other attributes of an entry it may update.

It grants no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## What it does not decide

The role only lets a holder open the screen and change the status and the rejection reason. Whose entries a holder sees and approves is decided by their roles on projects, not by this role: a holder who is no Manager or Approver on any project sees an empty screen ([approval](../views/approval.md#which-entries-it-shows)). The role grants no project screens, yet the screen still offers a holder the projects they approve on; a holder does not see the Projects or Time entries lists unless another role grants them.

## With Edit active time entries

A holder of the row-level role [Edit active time entries](edit-active-time-entries.md) can reject but not approve. The data store checks that role's rule against the entry as it is saved, with the change applied, so an update that saves an entry as Approved is refused with an access-denied exception, and approving fails for such a holder. Rejecting works for every entry the screen lets be rejected, New, Approved, and Rejected, an Approved one included, as the entry is saved as Rejected; a Closed one is refused, as for every holder.

## Holders

- The demo user `mossman`, in the `dev` demo data only, with [UI: minimal access](ui-minimal.md) and [Work schedule management](work-schedule-management.md). She is the Approver of the Borealis Recovery project and approves nowhere else.

Holders of [Full access](system-full-access.md) can open the screen without it, and the rule of the screen still narrows what they see to the projects where they are a Manager or an Approver.

## Not covered

- The screen rendered for a holder: the UI test checks that a holder opens it and switches its mode, and the role test that a holder loads and changes what it needs; no test sees the cards, avatars, and badges under the role. A live check as `mossman`, who has only granular roles, saw Project Management → Approve time entries, only the people of Borealis Recovery with their avatars and tag badges, and approving and rejecting working.
- A holder who lacks UI: minimal access: not checked.
- A change of another attribute of an entry by code running as a holder: no test.
- The combination with Edit active time entries: no test in the suite; confirmed with a throwaway test on 2026-10-31 through the approve and reject operations. What the screen shows a holder whose approval is refused was not checked.
