# 0025 An approved time entry is locked for its author

Status: accepted

## Context

Before issue #23 the role Edit active time entries kept its holders from changing an Approved or Closed entry, by a predicate on the status as saved. Then [decision 0012](0012-a-changed-approved-or-rejected-time-entry-goes-back-to-new-on-every-save.md) sent a changed Approved entry back to New in a listener that runs before that predicate, so the predicate saw New, and only the screens, which ask about the entry as loaded, still kept an author out (issue #23, comment b). The time entry rules that replaced the role had to say whether an author changes their Approved entry at all, and whether a Manager, whom the approval rule lets review their own entries, is an exception.

Alternatives considered and rejected:

- **The author changes an Approved entry, and decision 0012 sends it back to New.** An approval would be withdrawn by the person it reviews rather than by the reviewer.
- **A Manager changes their own Approved entry**, as the approval rule lets them review it. The lock would then depend on who reviews the author, while a Manager can reopen their own entry as any entry is reopened, by rejecting it on the approval screen.

## Decision

The time entry rules judge a save against the entry as stored: its author changes or removes it only while it is stored New or Rejected, and a reviewer, by the approval rule, moves an Approved entry only to Rejected. So an Approved entry is locked for its author, a Manager reviewing their own entries included: only a rejection opens it again, and the author's fix then sends it back to New. A worker who finds a mistake in an approved entry asks for it to be rejected. A Closed entry is locked for everyone who holds a row-level role. Decided by the user (issue #23).

## Consequences

- The lock holds on every save, as the rules read the stored status from change tracking ([decision 0024](0024-the-per-project-write-rules-live-in-row-level-predicates-not-in-listeners.md)): the gap decision 0012 left to the roles work is closed.
- The time entry editor opens an author's Approved entry read-only, the calendar refuses to move it, and Remove is not offered on it. Copy still is: the copy is a new New entry.
- A Manager's own Approved entry is read-only for them everywhere but the approval screen, where they can reject it and then fix it.
- Full access holds no row-level role, so the Administrator still changes an Approved or a Closed entry, and decision 0012 sends a changed Approved one back to New.
