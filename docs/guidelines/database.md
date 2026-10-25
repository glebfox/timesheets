# Database

Project-specific mechanics of the schema and its changelogs. How to write a changelog that matches an entity change is the `jmix-create-liquibase-changelog` skill.

## Changelog order

The root `changelog.xml` includes the add-ons' changelogs, then `includeAll` of `liquibase/changelog/`, which runs files in alphabetical path order:

1. `010-init-user.xml` — the user table and the `admin` user with full access, in every context.
2. `YYYY/MM/*.xml` — the rest of the schema, one changelog per model change. New changelogs go here, in the folder of the month they are written. A schema changelog may also insert a row that every context needs, in a changeset with the `context` and `dbms` variants of the `admin` insert, as the default work schedule's changelog does; demo data may then update that row.
3. `init-data/*.xml` — demo data, in the `dev` context only.

So on a fresh `dev` database the demo data runs after every schema changelog, including ones added later, while an existing database applied it long ago. A schema change that adds a required column must keep the demo inserts valid, with a default value or an updated insert; otherwise a fresh `dev` start fails while existing databases migrate fine.

Demo data may be edited in place. That changes the changeset's checksum, so a `dev` database that already applied it fails to start until it is recreated.

## Soft delete and uniqueness

Every entity is soft-deleted, so a unique index covers non-deleted rows only: it is created with `where DELETED_DATE is null` appended, and only for PostgreSQL (`dbms="postgresql"`). A deleted record's name can therefore be reused. Other databases, including the HSQLDB of the tests, get no unique index at all. A new unique constraint follows the same pattern.
