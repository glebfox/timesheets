# Absence

Source: `entity/absence/Absence.java`, `entity/absence/AbsenceDays.java`, `entity/absence/AbsenceDaysValidator.java`, `app/AbsenceSupport.java`, `app/WorkPlanSupport.java`, `liquibase/changelog/2026/11/04-121000-08247a92.xml`

A range of whole days on which a [user](user.md) is away, such as a vacation or a sick leave, recorded as one of the [absence types](absence-type.md). An absence's days plan nothing, whatever the user's [work schedule](work-schedule.md) and its [holiday calendar](holiday-calendar.md) say ([a user's plan for a day](work-schedule.md#a-users-plan-for-a-day)): a schedule and a calendar apply to everyone who works to them, and an absence is one person's. Time logged on an absent day counts as spent, as on a weekend, and nothing stops it from being logged; time logged before the absence was recorded stays as it is.

Absences are recorded for users by the holders of [Absence management](../roles/absence-management.md), in the [absence screens](../views/absences.md); there are no requests or approvals, and a user without that role cannot record their own. The [calendar](../views/calendar.md#absences) shows each user their own absences as bars across their days.

## Attributes

- **User** — required.
- **Type** — required.
- **First day** and **last day** — required; both belong to the absence, so a one-day absence has the same first and last day. There are no parts of a day, and no limit on the length: a parental leave runs for months.
- **Comment** — optional plain text, for the people who manage absences; the calendar does not show it.
- Shown as `<first day> – <last day> <type>`, such as "02/03/2026 – 13/03/2026 Vacation", or `<day> <type>` for one day.

The last day is not before the first, on every save: a save through `DataManager` is refused with "The last day must not be before the first day" («Последний день не может быть раньше первого»). The [editor](../views/absences.md#absence-editor) refuses such a range on its one dates field, with the same message, before that.

One user's absences do not overlap: the editor refuses a range that shares a day with another non-deleted absence of the same user, whatever its type, a deleted type included, before the save. Nothing else checks it, so a save through `DataManager` can make two absences overlap. The plan stays right then: each day they share plans nothing, and the calendar shows both bars.

## Deletion

A deleted absence stops counting: its days plan as the schedule and its holiday calendar say, and the calendar drops its bar. Deleting a user deletes their absences with them, and restoring the user in Data Tools restores them. Deleting the type leaves its absences in place: they still plan nothing and keep the type's name.

## Not covered

- Two overlapping absences, which only a save through `DataManager` makes: the plan of the days they share and their two bars have no test and were not checked live.
- Restoring a user in Data Tools restoring their absences: no test, and not checked live.
