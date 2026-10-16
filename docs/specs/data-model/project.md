# Project

Source: `entity/project/Project.java`, `entity/project/ProjectStatus.java`, `app/ProjectSupport.java`

Work done for a client, and the container for its tasks, participants, and tag groups.

## Attributes

- **Name** — required; unique among non-deleted projects (`docs/guidelines/database.md`).
- **Client** — required ([client](client.md)).
- **Description** — optional.
- **Status** — Open or Closed; every new project starts as Open. Closing a project makes its tasks no longer [bookable](task.md#bookable-tasks); it does not lock the project or the time entries already logged on it.
- **Tasks**, **participants**, **tag groups** — owned by the project ([task](task.md), [project participant](project-participant.md), [tag group](tag-group.md)): they are edited inside the project and saved with it.

## Deletion

Deleting a project also deletes its tasks, participants, and tag groups, and the tag groups take their tags with them. Time entries on the project's tasks stay in place, still referring to the deleted tasks.

## Not covered

- The deletion cascade: no test.
- The effect of closing a project on bookable tasks: no test.
