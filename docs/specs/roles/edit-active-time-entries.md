# Edit active time entries

Source: `security/EditActiveTimeEntriesRole.java`

Row-level role `edit-active-time-entries`, named "EditActiveTimeEntries".

## Grants

It grants nothing on its own; it narrows what other roles grant. A holder may not change a [time entry](../data-model/time-entry.md) whose status is Approved or Closed; New and Rejected entries stay editable. Creating, reading, and deleting entries are not affected.

The [calendar](../views/calendar.md) checks it before moving an entry to another day.

## Holders

None in code or seed data.

## Not covered

- No test checks the role.
- How the time entry editor and the lists treat an Approved or Closed entry under this role: not checked live.
