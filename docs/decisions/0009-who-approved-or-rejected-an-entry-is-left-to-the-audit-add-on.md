# 0009 Who approved or rejected an entry is left to the Audit add-on

Status: accepted

## Context

A time entry has a status and, for a rejection, a reason. The approval screen sets exactly these two, and the role that opens it grants the change of exactly these two attributes of a time entry. Who changed an entity and when is a trace Jmix keeps without the project's own data: the Audit of modification trait records who changed an entity last and when, and the Audit add-on's entity log records each change of the attributes it is set up for, with its user and time.

Alternatives considered and rejected:

- **Attributes or an entity of the project's own for the approver and the time, written by the approval operations.** It would duplicate what the trait and the Audit add-on already do, and it would miss a status set anywhere else, such as in the time entry editor.
- **Adding the trait and the Audit add-on in this work.** The screen does not need them to work, and they are a change of their own: a dependency, its tables, its setup, and where to show the trace. Left to issue #51.

## Decision

Approving sets the status Approved, and rejecting sets Rejected with the reason; the project keeps no attribute or entity of its own for who did it or when, and nothing else of the entry changes. When a trace is needed, it comes from the Audit of modification trait on the time entry and the Audit add-on (#51), not from data of the project's own. Decided with the user.

## Consequences

- Until #51 lands, nothing in the data says who approved or rejected an entry, or when. The editor lets an author set any status, so an Approved entry may have been set by its author as well, and the data cannot tell the two apart.
- A trace added later starts empty: approvals made before it have none and cannot get one.
- A rejection's reason is the only thing a rejection leaves behind: it is kept when the entry is approved afterwards, and replaced by a later rejection's.
