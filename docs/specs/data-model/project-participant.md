# Project participant

Source: `entity/project/ProjectParticipant.java`

A user's membership in a project, with a role.

## Attributes

- **User**, **project**, **role** — required. The role comes from the [project role](project-role.md) catalog.
- A user participates in a project at most once: the pair is unique among non-deleted participants (`docs/guidelines/database.md`).
- Shown as the user's name.

## Meaning

Participation is what makes a project's tasks [bookable](task.md#bookable-tasks) for the user and lists them in the user's My tasks. The role is informational: it grants no permissions and changes nothing a participant can do ([security roles](../roles/README.md)).

## Deletion

A participant is deleted with its project. Deleting the user or the role leaves the participant in place.

## Not covered

- The one-participation-per-user rule: HSQLDB has no unique index, so no test can check it.
