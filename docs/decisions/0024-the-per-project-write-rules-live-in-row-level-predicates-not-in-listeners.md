# 0024 The per-project write rules live in row-level predicates, not in listeners

Status: accepted

## Context

Project scope narrows reading by JPQL read policies ([decision 0023](0023-project-roles-narrow-job-roles-through-one-row-level-role.md)). The write rules depend on the project and the entry: a Manager changes only the projects they manage, with their tasks, participants, tag groups, and tags; an author changes only their own time entries, and only while they are stored New or Rejected; a reviewer changes only the status and the rejection reason of an entry the approval rule lets them review; Approved and Closed entries are locked ([decision 0025](0025-an-approved-time-entry-is-locked-for-its-author.md)). They must hold on every save, whatever makes it. The project already keeps two rules of every save in entity listeners ([decision 0001](0001-one-default-work-schedule-enforced-by-a-listener.md), [decision 0012](0012-a-changed-approved-or-rejected-time-entry-goes-back-to-new-on-every-save.md)).

The data store checks the create, update, and delete predicates inside the save transaction, after the `EntitySavingEvent` listeners and the merge, on the merged instance: the back-to-New listener has by then turned a changed Approved entry New, and a load from a predicate would return the saved state, not the stored one.

Alternatives considered and rejected:

- **Reads in the row-level role, writes in `EntitySavingEvent` listeners**, through the same beans, with Project office and Full access let through by specific permissions. It matches the project's listeners, but splits the security model: the Row-level roles screen would show only the reads, two specific permissions would be added, and the listeners would have to let system saves through.
- **The screens alone keep the rules**: rejected as for reading, since any other channel would write everything.

## Decision

The write rules are the create, update, and delete predicates of the row-level roles Project scope and Time entry rules. The predicates call beans in `app/` that hold the rules and tell the person's part in a project, such as whether the approval rule lets them review an entry's author. A predicate judges a save against the stored values, which it reads from the instance's change tracking. Decided by the user (issue #23).

## Consequences

- Every save is checked whatever makes it, and the Row-level roles screen lists the write rules beside the reads. Nothing exempts a person in code: Full access holds no row-level role, and Project office holds Time entry rules alone, so it changes every project but writes only its own time entries.
- A save of project data or of an entry costs a query or two. The predicates never load an entity and never flush: an entity query inside a save refreshes the rows the save holds, and a flush clears the change tracking they read (`docs/guidelines/views.md`).
- The screens ask the same predicates about an entity as they loaded it, so each screen offers only what a save would let through: the editor opens read-only, the calendar refuses a move, and Remove is disabled where the rules refuse.
- The approval rule is stated twice: by participation for the predicates, and as a query of the entries for the approval screen and the dashboard (`TimeEntryApprovalSupport`), so a change to it touches both.
- The back-to-New rule and the one default work schedule stay in listeners: they are rules of the data, not of who may write it.
