# Edit active time entries

Source: `security/EditActiveTimeEntriesRole.java`

Row-level role `edit-active-time-entries`, named "EditActiveTimeEntries".

## Grants

It grants nothing on its own; it narrows what other roles grant. A holder may not save an update that leaves a [time entry](../data-model/time-entry.md) Approved or Closed, or that moves it to one of those statuses; updates that leave an entry New or Rejected stay allowed. Creating, reading, and deleting entries are not affected.

The data store checks the rule against the entry as it is saved, with the change applied. An update through the data store therefore cannot save an entry as Approved or Closed, and can still change an Approved entry to another status: a holder of [Time entries: approval](time-entry-approval.md#with-edit-active-time-entries) can reject an Approved entry but cannot approve any.

The [calendar](../views/calendar.md) checks it before moving an entry to another day.

## Holders

None in code or seed data.

## Not covered

- No test checks the role. The as-saved check on an Approved entry was confirmed with a throwaway test on 2026-10-31, through the approve and reject operations of the [approval screen](../views/approval.md).
- How the time entry editor and the lists treat an Approved or Closed entry under this role: not checked live.
