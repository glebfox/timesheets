# Time entry rules

Source: `security/TimeEntryRulesRole.java`, `app/TimeEntryAccessSupport.java`

Row-level role `time-entry-rules`, named "Time entry rules".

## Grants

It grants nothing on its own; it narrows what resource roles grant on [time entries](../data-model/time-entry.md): who creates, changes, and removes an entry, in which [status](../data-model/time-entry.md#status), and which of its attributes. The rules hold on every save, whatever screen or `DataManager` call makes it, the [approval screen](../views/approval.md)'s operations included, because attribute policies bind only the screens. A save they refuse fails with an access-denied exception and saves nothing; reading is not narrowed.

1. A person creates entries only for themselves, only as New, and without a rejection reason.
2. The author changes or removes their entry while it is stored New or Rejected. They never change its user or its rejection reason, and their change sets no status but New, the one a changed Rejected entry goes back to.
3. An entry is created, or moved to another task, only on a task of a project where the person takes part in a role other than Observer ([project participant](../data-model/project-participant.md)), whatever saves it, a copy included. An entry already logged keeps its task: an author who has left a project still fixes their Rejected entry there, as long as its task stays.
4. Someone the approval rule lets review the entry ([approval](../views/approval.md#which-entries-it-shows)) changes only its status and rejection reason, sets only Approved or Rejected, and only while the entry is stored New, Approved, or Rejected.
5. An Approved entry is therefore locked for its author, a Manager reviewing their own entries included: only a rejection opens it again, and the author's fix then sends it back to New ([decision 0012](../../decisions/0012-a-changed-approved-or-rejected-time-entry-goes-back-to-new-on-every-save.md)). A worker who finds a mistake in an approved entry asks for it to be rejected.
6. A Closed entry, whose time has been paid for, is changed or removed by nobody holding the role. [Full access](system-full-access.md) holds no row-level role, so a holder of Full access alone still changes one.

A save is judged against the entry as stored before it, not as the save leaves it, so the rule that sends a changed Approved entry back to New does not open it to its author:

- A save that changes the status or the rejection reason, and neither what the entry says (its date, task, user, spent time, description, or tags) nor whether it is deleted, is a review: rule 4.
- Any other save, one that changes nothing included, is the author's: rules 2 and 3.

So a change of the tags alone is a change of what the entry says: its author saves it on a Rejected entry, which goes back to New, and a reviewer who changes the tags together with the status is refused. Two entries approved in one save are each judged on their own.

A screen asks about an entry it has loaded and not changed, so its question is judged by rule 2 alone: an entry the person did not write, and their own Approved or Closed one, a Manager's own Approved entry included, get no. The time entry editor then opens read-only, and the calendar refuses a move; the same Manager can still reject that entry on the approval screen, as a review. An entry loaded without its user or its status gets no, as its author or its status is unknown.

## Holders

None in code or seed data.

## Not covered

- The rules through a constrained `DataManager` save and through the approve and reject operations, each acting user holding Full access and this role: `TimeEntryRulesRoleTest`. A user working as a substitute is judged as the substituted user, which only `ProjectAccessSupportTest` checks, for the user's part in a project.
- The screens under the role: no test opens the editor, the lists, the calendar, or Copy as a holder. Read from the code: the calendar loads its entries without their user, so it refuses every move of a holder; Copy asks rule 1 about the source entry, so it is enabled only on the person's own New entry without a reason; remove actions check only the entity permission, so a refused removal fails on save.
