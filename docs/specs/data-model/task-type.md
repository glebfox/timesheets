# Task type

Source: `entity/task/TaskType.java`

A catalog entry classifying tasks, such as "Development" or "Meeting".

## Attributes

- **Name** — required; unique among non-deleted task types (`docs/guidelines/database.md`).
- **Description** — optional, in Markdown.

## Deletion

Deleting a task type leaves the tasks of that type in place, still referring to it.
