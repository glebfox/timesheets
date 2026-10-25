# Work schedule assignment

Source: `entity/schedule/WorkScheduleAssignment.java`, `app/WorkPlanSupport.java`

A [user](user.md) working to a [work schedule](work-schedule.md) from a date on. A user's assignments form a history: each day takes the schedule of the latest assignment that starts on or before it, so an assignment has no end date and the next one ends it. Days before the first assignment, and every day of a user with no assignments, take the default schedule. An assignment from a date changes only the days from that date on; reassigning a user leaves their past months as they were.

Assignments are set up in Data Tools: no screen of the application manages them.

## Attributes

- **User**, **work schedule**, **start date** — required. A start date in the future is allowed: a planned change.
- A user has at most one assignment per start date among non-deleted assignments (`docs/guidelines/database.md`).
- Shown as `<start date> <schedule>`.

## Deletion

A deleted assignment stops counting: its days fall to the user's previous assignment, or to the default schedule. Deleting a user deletes their assignments with them, and restoring the user in Data Tools restores them. Deleting the schedule leaves its assignments in place, and the plan they give stays as it was.

## Not covered

- One assignment per user and start date: the tests run on HSQLDB, which has no unique indexes (`docs/guidelines/testing.md`).
