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

The role only lets a holder open the screen and change the status and the rejection reason. Whose entries a holder sees and approves is decided by their roles on projects, not by this role: a holder who is no Manager or Approver on any project sees an empty screen ([approval](../views/approval.md#which-entries-it-shows)). The role grants no project screens, yet the screen still offers a holder the projects they approve on; a holder does not see the Projects or Time entries lists unless another role grants them. On the [dashboard](../views/dashboard.md#my-team), a holder who approves on at least one project also sees My team, whose blocks open the approval screen; a holder who approves nowhere sees none. Team this week and Team away show a holder the people's time on other projects, their plans, and their absences with types, which neither this role nor User: read only grants ([decision 0020](../../decisions/0020-the-dashboard-shows-an-approver-their-teams-other-time-plans-and-absences.md)).

## With Time entry rules

A holder of the row-level role [Time entry rules](time-entry-rules.md) approves and rejects only as that role lets a reviewer save: an entry the approval rule lets them review, asked about the entry as stored, so not one of a deleted task or project, stored New, Approved, or Rejected, saved as Approved or Rejected with nothing else changed. That is the rule the screen applies, so approving and rejecting work for every entry the screen offers, a Manager's own Approved entry included, which is locked for them as its author but can be rejected here; a Closed one is refused, as for every holder. That role also refuses a holder's change to any other attribute of someone else's entry, which the attribute policies of this role leave to the screens.

## Holders

None directly. [Approver](approver.md) includes it, and through Approver [Manager](manager.md), so in the `dev` demo data `mossman`, an Approver, and the Managers `kleiner`, `eli`, `magnusson`, `cave`, and `caroline` hold it. `mossman` is the Approver of the Borealis Recovery project and approves nowhere else. She sees My team for the people of Borealis Recovery.

Holders of [Full access](system-full-access.md) can open the screen without it, and the rule of the screen still narrows what they see to the projects where they are a Manager or an Approver.

## Not covered

- The screen rendered for a holder: the UI test checks that a holder opens it and switches its mode, and the role test that a holder loads and changes what it needs; no test sees the cards, avatars, and badges under the role. A live check as `mossman`, when she held only granular roles, saw Project Management → Approve time entries, only the people of Borealis Recovery with their avatars and tag badges, and approving and rejecting working.
- A holder who lacks UI: minimal access: not checked.
- A change of another attribute of an entry by code running as a holder: no test.
- The combination with Time entry rules: the approve and reject operations under that role are checked for users who hold it with Full access ([Time entry rules](time-entry-rules.md#not-covered)), and approving, through the approval support bean, for a holder of [Approver](approver.md), which includes this role, with [Project office](project-office.md) (`JobRolesTest`). Rejecting as a holder of this role, and what the screen shows a holder of both, were not checked.
