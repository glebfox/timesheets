# Absence type

Source: `entity/absence/AbsenceType.java`, `liquibase/changelog/2026/11/04-121000-08247a92.xml`, `demo/DemoDataLoader.java`

A catalog entry classifying [absences](absence.md), such as "Vacation", "Sick leave", or "Day off". Types are managed in the [absence type screens](../views/settings.md#absence-types).

## Attributes

- **Name** — required; unique among non-deleted absence types (`docs/guidelines/database.md`). A duplicate is refused with "An absence type with the same name already exists".
- **Description** — optional.

A type carries no other rule: there are no balances, limits, or paid and unpaid types, and an absence of any type plans nothing on its days. The schema creates no types, so a new installation has none until a holder of [Absence management](../roles/absence-management.md) adds them; the `dev` demo data creates Vacation, Sick leave, and Day off.

## Deletion

Deleting a type removes it from the absence type list, the absence list's type filter, and the absence editor's drop-down, but the absences that refer to it keep it: they still plan nothing, the calendar still labels their bars with its name, and the absence list shows it with a Deleted badge.

## Not covered

- Name uniqueness: the tests run on HSQLDB, which has no unique indexes (`docs/guidelines/testing.md`).
