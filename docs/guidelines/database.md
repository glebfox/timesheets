# Database

Project-specific mechanics of the schema, its changelogs, and soft deletion. How to write a changelog that matches an entity change is the `jmix-create-liquibase-changelog` skill.

## Changelog order

The root `changelog.xml` includes the add-ons' changelogs, then `includeAll` of `liquibase/changelog/`, which runs files in alphabetical path order:

1. `010-init-user.xml` — the user table and the `admin` user with full access, in every context.
2. `YYYY/MM/*.xml` — the rest of the schema, one changelog per model change. New changelogs go here, in the folder of the month they are written. A schema changelog may also insert a row that every context needs, in a changeset with the `context` and `dbms` variants of the `admin` insert, as the default work schedule's changelog does; demo data may then update that row.
3. `init-data/*.xml` — demo data, in the `dev` context only.

So on a fresh `dev` database the demo data runs after every schema changelog, including ones added later, while an existing database applied it long ago. A schema change that adds a required column must keep the demo inserts valid, with a default value or an updated insert; otherwise a fresh `dev` start fails while existing databases migrate fine.

Demo data may be edited in place. That changes the changeset's checksum, so a `dev` database that already applied it fails to start until it is recreated.

## Soft delete and uniqueness

On a soft-deleted entity, which every entity is so far (`docs/conventions.md`), a unique index covers non-deleted rows only: it is created with `where DELETED_DATE is null` appended, and only for PostgreSQL (`dbms="postgresql"`). A deleted record's name can therefore be reused. Other databases, including the HSQLDB of the tests, get no unique index at all. A new unique constraint on a soft-deleted entity follows the same pattern; a hard-deleted entity has no deleted rows to leave out, so its unique index is an ordinary one on every database.

Removing a record and adding another with the same unique key in one save, such as a holiday calendar day removed and its date added again in one editor session, works on PostgreSQL: EclipseLink writes the soft delete, an update, before the insert, so the index never covers both rows at once. It was checked live; no test can show it (`docs/guidelines/testing.md`).

## Soft delete and references

A record that refers to a soft-deleted one, such as a work schedule whose holiday calendar is deleted, or an assignment whose schedule is deleted, saves as it is and keeps the reference: the save needs no `PersistenceHints.SOFT_DELETION` hint, so an editor's save keeps soft deletion on. See `WorkScheduleEditorUiTest` and `UserAssignmentsUiTest`.

A query condition that navigates such a reference, such as `e.workSchedule.name`, joins the referenced table with the soft deletion filter and drops the rows that refer to a deleted record. A load that must keep those rows, as the loads of assignments do, leaves the reference out of its conditions.

A record that refers to a soft-deleted one still reaches it through its fetch plan, as a time entry loaded with its user does when the user is deleted. A load of the soft-deleted record by its own id, such as by the old reference id of a delete event, finds nothing unless soft deletion is off for that load. See `TimeEntryEventListener`.
