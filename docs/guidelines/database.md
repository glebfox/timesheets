# Database

Project-specific mechanics of the schema, its changelogs, and soft deletion. How to write a changelog that matches an entity change is the `jmix-create-liquibase-changelog` skill.

## Changelog order

The root `changelog.xml` includes the add-ons' changelogs, then `includeAll` of `liquibase/changelog/`, which runs files in alphabetical path order:

1. `010-init-user.xml` — the user table and the `admin` user with full access, in every environment.
2. `YYYY/MM/*.xml` — the rest of the schema, one changelog per model change. New changelogs go here, in the folder of the month they are written. A schema changelog may also insert a row that every environment needs, with one insert per `dbms` group as the `admin` insert has, as the default work schedule's changelog does; the demo data may then change that row.

The changelogs hold no demo data: a `dev` start loads it from code, after Liquibase has run (`ARCHITECTURE.md`); why: [decision 0007](../decisions/0007-demo-data-is-loaded-by-the-application-not-by-liquibase.md). A schema change therefore needs no change to the demo data unless the loader breaks on it, such as on a new required attribute, and `DemoDataLoaderTest` catches that.

## Soft delete and uniqueness

On a soft-deleted entity, which every entity is so far (`docs/conventions.md`), a unique index covers non-deleted rows only: it is created with `where DELETED_DATE is null` appended, and only for PostgreSQL (`dbms="postgresql"`). A deleted record's name can therefore be reused. Other databases, including the HSQLDB of the tests, get no unique index at all. A new unique constraint on a soft-deleted entity follows the same pattern; a hard-deleted entity has no deleted rows to leave out, so its unique index is an ordinary one on every database.

Removing a record and adding another with the same unique key in one save, such as a holiday calendar day removed and its date added again in one editor session, works on PostgreSQL: EclipseLink writes the soft delete, an update, before the insert, so the index never covers both rows at once. It was checked live; no test can show it (`docs/guidelines/testing.md`).

## Soft delete and references

A record that refers to a soft-deleted one, such as a work schedule whose holiday calendar is deleted, or an assignment whose schedule is deleted, saves as it is and keeps the reference: the save needs no `PersistenceHints.SOFT_DELETION` hint, so an editor's save keeps soft deletion on. See `WorkScheduleEditorUiTest` and `UserAssignmentsUiTest`.

A query condition that navigates such a reference, such as `e.workSchedule.name`, joins the referenced table with the soft deletion filter and drops the rows that refer to a deleted record. A load that must keep those rows, as the loads of assignments do, leaves the reference out of its conditions, or compares the reference by its foreign key: `e.user.id <> :id` in the query's own where clause and `e.user not in (select m.user ...)` do not join the user table, while `e.user.id` or `m.user = e.user` inside a subquery does. See `TimeEntryApprovalSupport` and its test `entriesOfDeletedUsersAreListed`. Such a comparison by the foreign key still drops the rows when the load also fetches that reference, unless it fetches it in batch: see the last paragraph. A sort is not such a condition: a list that the database sorts by a reference column, such as the absence list sorted by type, sorts through a left join and keeps the rows whose reference is deleted. See `listShowsAnAbsenceOfADeletedType` in `AbsenceViewsUiTest`.

A record loaded by its id still reaches a soft-deleted record it refers to through its fetch plan, as a time entry loaded by id with its user does when the user is deleted. A load of the soft-deleted record by its own id, such as by the old reference id of a delete event, finds nothing unless soft deletion is off for that load. See `TimeEntryEventListener`.

A collection in the fetch plan, such as a time entry's tags, is different: it leaves out its soft-deleted members, in a load by id too, so a tag deleted after it was put on an entry is not among the entry's tags as any screen loads them. A comparison with the stored record, such as the time entry listener's check for changed tags, loads it with soft deletion on as well, so both sides leave the deleted tag out. See `TimeEntryEventListener` and `softDeletedTagIsNoChange` in `TimeEntryStatusResetTest`.

A fetch plan of a query-loaded record reaches a deleted to-one reference: the reference is fetched with a left outer join that has no soft deletion filter, so the record keeps it and stays in the result, as an absence of a deleted type does. A fetch plan holds a reference also through the instance name: the `_base` fetch plan of an absence holds its type. The exception is a query whose conditions name the same reference by its id, such as `e.user.id <> :approverId`: such a comparison joins nothing by itself, but EclipseLink reuses its path for the fetch, which turns into an inner join with the soft deletion filter, and a row whose reference is deleted drops out. A condition in the query's own where clause on the reference's own path, such as `e.type = :type` or `e.type not in (select ...)`, does not do this: Jmix then switches the fetch to batch by itself and reads the reference in a query of its own. `e.user.id` is not that path. A condition that navigates further, such as `e.type.name`, drops the row whatever the fetch plan, as above. A load with such a condition that must keep those rows, such as the approval screen's load of the entries of deleted users, adds the reference with `FetchMode.BATCH`, which reads it in a query of its own with no filter. See `TimeEntryApprovalSupport`: without the batch fetch of the user, its test `entriesOfDeletedUsersAreListed` fails.
