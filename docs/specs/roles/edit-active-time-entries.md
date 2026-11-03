# Edit active time entries

Source: `security/EditActiveTimeEntriesRole.java`

Row-level role `edit-active-time-entries`, named "EditActiveTimeEntries".

## Grants

It grants nothing on its own; it narrows what other roles grant. A holder may not save an update that leaves a [time entry](../data-model/time-entry.md) Approved or Closed, or that moves it to one of those statuses; updates that leave an entry New or Rejected stay allowed. Creating, reading, and deleting entries are not affected.

The data store checks the rule against the entry as it is saved, with the change applied, and after a change to an Approved entry's date, user, task, spent time, description, or tags has sent it back to New ([status](../data-model/time-entry.md#status)). An update through the data store therefore cannot save an entry as Approved or Closed, and can still change an Approved entry to another status: a holder of [Time entries: approval](time-entry-approval.md#with-edit-active-time-entries) can reject an Approved entry but cannot approve any. Nor does the rule alone refuse a change to what an Approved entry says, which is saved as New; such a change to a Closed entry stays refused, as the entry stays Closed.

What keeps a holder from changing an Approved entry is the screens, which check the entry as it was loaded: the [time entry editor](../views/time-entries.md#editor) opens an Approved or Closed entry read-only for them, its tag fields included, and the [calendar](../views/calendar.md) checks the rule before moving an entry to another day.

## Holders

None in code or seed data.

## Not covered

- No test checks the role. The as-saved check on an Approved entry was confirmed with a throwaway test on 2026-10-31, through the approve and reject operations of the [approval screen](../views/approval.md).
- How the time entry editor, its tag fields included, the lists, and the calendar treat an Approved or Closed entry under this role: not checked live. That a change to what an Approved entry says passes the rule once it is saved as New: no test. `TimeEntryDetailViewUiTest` checks the tag fields only under a read-only mode that the test sets itself.
