# 0009 Approving and rejecting keep no record of who did it or when

Status: accepted

## Context

A time entry has a status and, for a rejection, a reason. The approval screen sets exactly these two, and the role that opens it grants the change of exactly these two attributes of a time entry. A record of who approved or rejected an entry, and when, would take new attributes or an entity of its own, a changelog, and a place on the screens to show it.

Alternatives considered and rejected:

- **Record the approver and the time, on the entry or in a history of its status changes.** Not needed yet, as the user decided: the screen moves entries between statuses and keeps nothing besides.

## Decision

Approving sets the status Approved, and rejecting sets Rejected with the reason; no attribute records who did it or when, and nothing else of the entry changes. Decided with the user. If a trace is ever needed, it becomes a follow-up issue.

## Consequences

- Nothing in the data says who approved or rejected an entry, or when. The editor lets an author set any status, so an Approved entry may have been set by its author as well, and the data cannot tell the two apart.
- A trace added later starts empty: approvals made before it have none and cannot get one.
- A rejection's reason is its only memory: it is kept when the entry is approved afterwards, and replaced by a later rejection's.
