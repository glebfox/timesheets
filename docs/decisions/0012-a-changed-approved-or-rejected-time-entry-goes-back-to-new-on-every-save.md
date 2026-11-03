# 0012 A changed approved or rejected time entry goes back to New on every save

Status: accepted

## Context

An author fixes a rejected entry by changing it, in the editor or by dragging it to another day on the calendar, and an approved entry can be changed the same ways. A changed entry used to keep its status, so a fixed entry stayed Rejected and nobody was asked to look at it again, and a changed Approved entry stayed Approved with time no approver had seen. Issue #61 asked that saving an entry send it back for review.

Alternatives considered and rejected:

- **The editor alone sends a changed entry back.** The calendar's move, a frequent fix of a rejection, saves without the editor, and every later save path would need code of its own.
- **Each screen tracks its own edits.** The rule would again depend on every screen; the listener compares the saved entry with the stored one instead.
- **Closed entries go back too.** Closing is meant to lock an entry, and an edit by its author must not reopen it.
- **The rule applies even when the same save sets a status.** A status chosen in the editor together with an edit would be overridden.

## Decision

An `EntitySavingEvent` listener on `TimeEntry` saves an existing Approved or Rejected entry as New when the save changes its date, user, task, spent time, description, or tags, compared with the stored entry loaded through `UnconstrainedDataManager`, unless the same save sets a status other than the stored one. The rejection reason is not content: changing only it changes nothing, and it is kept when the entry goes back to New. Decided with the user (issue #61).

## Consequences

- It is the second rule of the data model, after the one default work schedule ([decision 0001](0001-one-default-work-schedule-enforced-by-a-listener.md)), beyond required attributes, value ranges, and uniqueness, that holds on every save.
- The data store checks row-level update constraints after this listener, on the entry as it leaves it, so the predicate of Edit active time entries sees New and no longer refuses, on its own, a change to what an Approved entry says. The screens stop a holder, as they check the entry as loaded: the editor opens an Approved entry read-only for them, its tag fields included, and the calendar checks before a move. A save path that checks neither lets such a change through; whether the role should judge the stored status instead is left to the roles work (#23).
- Every update of an entry that is Approved or Rejected as saved loads the stored entry once, each entry of a bulk approval included.
- A save that changes nothing, such as OK without an edit, leaves a Rejected entry Rejected: an author who disagrees with a rejection says so by editing the description.
