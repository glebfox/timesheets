# Tag group and tag

Source: `entity/tag/`, `app/TagsSupport.java`

A tag group is a named set of tags that belongs to one project, such as "Activity" with the tags "Coding" and "Review". Time entries on the project's tasks are tagged from its groups, and a task can make some groups required ([time entry](time-entry.md#rules-applied-when-editing)).

## Tag group

- **Name** — required; not unique, even within a project.
- **Project** — required. A tag group is part of its [project](project.md): it is edited inside the project and deleted with it.
- **Tags** — owned by the group: edited inside it and deleted with it.

## Tag

- **Name** — required; not unique, even within a group.
- **Tag group** — required.
- Shown as `<name> [<group>]`.

## Deletion

A deleted tag group takes its tags with it. Deleting a time entry currently deletes its tags as well: a known defect described in [time entry](time-entry.md#deletion) and tracked in [#8](https://github.com/glebfox/timesheets/issues/8).
