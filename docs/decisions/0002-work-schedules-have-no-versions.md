# 0002 Work schedules have no versions; assignments from a date carry the history

Status: accepted

## Context

A user's plan for a day comes from the work schedule assigned to them on that day: its workday hours, its days of the week, and its holiday calendar. Both sides change over time. A user moves to another schedule, for example from full time to 0.5 rate, and a schedule itself may change, for example from 8 to 7 hours a day. Months already past should keep the plan they had, or a user's past shortfalls and overtime change after the fact.

Alternatives considered and rejected:

- **A schedule attribute on the user, with no history.** The cheapest model, but moving a user to another schedule rewrites every past month of theirs.
- **Versions of a schedule**, each edit kept with the date it takes effect, and plans choosing the version in force on each day. It keeps past plans through any edit, but every edit becomes a new record and every plan load has to pick a version per day, in the data model, in plan resolution, and in the editor. The user judged that cost too high for the need.

## Decision

A user's schedule history is a list of assignments, each a schedule from a start date; a day takes the schedule of the latest assignment that starts on or before it. Schedules themselves have no versions: editing one changes the plan of every day it applies to, past days included. When conditions change from some date, a new schedule is created and assigned from that date. Decided by the user.

## Consequences

- Moving a user to another schedule leaves their earlier months as they were.
- Editing a schedule's workday hours, days of the week, or holiday calendar changes past plans of everyone on it. A change meant only from a date is a new schedule, so schedules accumulate, and their names have to tell them apart.
- Changing the default schedule changes the past plans of users who have no assignment on those days. Assigning a schedule explicitly from their first day freezes them.
- Holiday calendar days are dated, so a calendar keeps its history without versions; editing a past day changes past plans too.
- Deleting a schedule rewrites nothing: a soft-deleted schedule keeps applying to the assignments that refer to it.
- Adding versions later means a new table, a migration that turns every existing schedule into its first version, and plan resolution choosing a version per day.
