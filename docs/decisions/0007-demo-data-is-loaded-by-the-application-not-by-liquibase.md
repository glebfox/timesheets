# 0007 Demo data is loaded by the application, not by Liquibase

Status: accepted

## Context

The `dev` profile needs demo data that shows the application at work: a team with avatars, clients with logos, projects and tasks in several states, and time logged in the weeks up to the day the database is created, so that the calendar, which opens on the current week, opens on data (issue #44). The demo data was a set of Liquibase changelogs in the `dev` context, run after the schema changelogs, with every date fixed when the changelog was written.

Alternatives considered and rejected:

- **Liquibase changelogs in the `dev` context, the Jmix default.** Dates relative to the current date need database-specific SQL in every insert, and time entries that follow each user's plan, through schedule changes and holidays, cannot be computed in a changelog at all. The inserts bypass the entity listeners, so they have to be written to satisfy the listeners' rules by hand, and editing an applied changeset breaks every `dev` database that applied it until the database is recreated.

## Decision

Beans of the `dev` profile, in `demo/`, load the demo data through `DataManager` once the application is ready: in one transaction, with dates computed from the day of the start, and only into a database that has no clients, deleted ones included. The changelogs hold only the schema and the rows every environment needs. Decided by the user.

## Consequences

- The demo data goes through the entity listeners as any save does, and its time entries tell their users as created entries do.
- A changed loader never reaches a database that already has clients: to get the new demo data, a developer recreates the database.
- A failed load leaves nothing behind and runs again on the next start.
- A schema change needs a change to the demo data only when the loader breaks on it, which `DemoDataLoaderTest` catches.
- A production start never loads demo data: the loader exists only in the `dev` profile.
