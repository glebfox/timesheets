# Time entry

Source: `entity/timeentry/`, `app/TimeEntrySupport.java`, `app/TimeEntryApprovalSupport.java`, `listener/TimeEntryEventListener.java`, `event/TimeEntryStatusChangedEvent.java`

One user's time spent on one task on one day. Any number of entries may share a user, task, and date.

## Attributes

- **Date, user, task, spent time** — required. Spent time is a [work time](work-time.md) value.
- **Description** — optional, in Markdown.
- **Tags** — any number of [tags](tag-group.md).
- **Status** — New, Approved, Rejected, or Closed; every new entry starts as New.
- **Rejection reason** — Markdown text that explains a rejection. It is kept when the status later changes.
- Shown as `<date> [<project>] <task>`.

## Status

Nothing in the application moves an entry between statuses on its own. Two paths change a status:

- The editor: whoever can edit an entry can set any status there, except that a holder of [Edit active time entries](../roles/edit-active-time-entries.md) cannot save an update that makes or leaves an entry Approved or Closed. No transition is forbidden, so an author can set their own entry to Approved.
- The [approval screen](../views/approval.md): a Manager or Approver of the entry's project, by the rule that decides whose entries they approve, sets a New or Rejected entry to Approved, and a New, Approved, or Rejected entry to Rejected with a reason, which replaces an earlier one. An approved entry keeps its rejection reason. The screen never sets New or Closed and never changes a Closed entry.

Neither path records who changed the status, or when ([decision 0009](../../decisions/0009-who-approved-or-rejected-an-entry-is-left-to-the-audit-add-on.md)).

Apart from the approval screen, Rejected is the only status with behavior attached: the entry's user sees their count of Rejected entries in the main menu ([main view](../views/main.md)), and the editor asks for a rejection reason ([time entries](../views/time-entries.md)).

After an entry is created or deleted, or its status changes, by the editor or the approval screen alike, the open sessions of the entry's user are told to refresh that count, whether or not that user is deleted. A change that moves an entry to another user tells only the new user, and only if the status changed too.

## Rules applied when editing

These rules are applied by the time-entry editor, not on save; a save through `DataManager` skips them.

- The task must be [bookable](task.md#bookable-tasks) for the entry's user.
- Tags come from the tag groups of the task's project. Each of the task's required tag groups must contribute at least one tag; the other groups are optional. When an existing entry is moved to a task of another project, its saved tags from the old project stay unless the user removes them.
- A Rejected entry needs a rejection reason.

## Copying

A copy is a new entry with the source's date, user, task, spent time, description, tags, and rejection reason, and the status New.

## Deletion

Deleting an entry soft-deletes it. **Known defect:** it also deletes every tag attached to the entry, so the tag disappears from its tag group and from every other entry that carries it. Confirmed with a throwaway test on 2026-10-16; tracked in [#8](https://github.com/glebfox/timesheets/issues/8).

## Not covered

- The editing rules above: no UI test exercises the time-entry editor.
- The description and rejection reason being carried into a copy: the copy test does not check them.
- Who is told when an entry is created, when a change moves it to another user, and when a change leaves its status alone: the tests check an entry of a deleted user, when it is deleted or its status changes, and that the approval operations tell the user of a live entry whose status they change; `DemoDataLoaderTest` checks that each entry the demo data creates tells someone once, but not whom.
- The rejected count refreshing in the open sessions that are told: it needs a live session (`docs/guidelines/testing.md`).
- The tag deletion defect: no test in the suite.
