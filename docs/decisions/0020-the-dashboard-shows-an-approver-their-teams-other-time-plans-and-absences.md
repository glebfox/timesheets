# 0020 The dashboard shows an approver their team's other time, plans, and absences

Status: accepted

## Context

Team this week tells an approver whether each person they approve logged their week, and Team away who is away. An approver's roles may grant neither the entries on other projects, nor work schedules, nor absences.

Alternatives considered and rejected:

- **Only the time on the projects the approver approves.** A person who works on several projects would look as if they had not logged their week.
- **Team away only for holders of absence permissions.** Team this week already marks the absent days through the plan.

## Decision

For the people an approver approves by participation, the dashboard reads without the approver's permissions: each person's total on other projects, without project or status; their plan for the week; and their absences with the type, such as Sick leave, but not the comment. Decided by the user (issue #69).

## Consequences

- An approver learns the hours outside their projects, the schedules, and the absence types of the people they approve, and of no one else.
- The people are read with their names, avatars, and active flag only; no credentials leave the database.
