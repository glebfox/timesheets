# 0001 One default work schedule, enforced by an entity listener

Status: accepted

## Context

A user's plan falls back to the default work schedule on every day no assignment covers, so a moment with no default, or with two, leaves the plan without an answer or with an arbitrary one. The default flag can change through any save path, not only through the schedule editor: Data Tools' entity inspector edits, deletes, and restores schedules, and any `DataManager` call can do the same. Every other cross-row rule of the data model is applied only by the editors, and uniqueness elsewhere is a PostgreSQL partial unique index over non-deleted rows (`docs/guidelines/database.md`).

Alternatives considered and rejected:

- **The schedule editor alone keeps the rule.** Data Tools and `DataManager` bypass an editor, and the rule must hold on every save path.
- **The entity class keeps the rule**, in its own lifecycle callbacks or methods. Rejected by the user: the entity carries no business logic.
- **A partial unique index on `IS_DEFAULT`.** The listener unmarks the previous default after the new default is flushed, since `EntityChangedEvent` is delivered after the flush, so such an index would reject every change of default. The rule therefore rests on no database constraint.

## Decision

An `EntityChangedEvent` listener on `WorkSchedule`, running before commit, keeps exactly one non-deleted default on every save. Marking a schedule as the default unmarks the others in the same transaction. Unmarking or deleting the default while no other schedule is the default is refused, which rolls the save back. The listener works through `UnconstrainedDataManager`, so the rule holds whatever the saving user may read. The schema changelog seeds the default in every context, so the rule holds from the first start.

## Consequences

- The one default is the only rule of the data model, beyond required attributes, value ranges, and uniqueness, that holds on every save.
- The default is changed by marking another schedule, never by unmarking the current one.
- A schedule deleted as the default, in the same save that marked another, keeps its flag while deleted; the listener unmarks it when it is restored while another schedule is the default.
- Two saves that mark different schedules at the same time both unmark the same previous default, so optimistic locking fails one of them.
- Writes that bypass the application, such as SQL or Liquibase changesets, bypass the rule too: the seed and the demo data are written to satisfy it.
