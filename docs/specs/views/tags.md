# Tag screens

Source: `view/taggroup/`, `view/tag/`

The tag group and tag editors and pickers. What tag groups and tags are: [tag group and tag](../data-model/tag-group.md).

## Tag group editor

`tagGroups/:id`, a 480px dialog when opened as one; it is opened from the Tag groups tab of the [project editor](projects.md#project-editor).

- **Name**.
- **Project** — shown only when the group has no project yet, which happens only when the editor is opened by URL.
- **Tags** — the group's tags by name, with Create and Edit opening the tag editor in a dialog, and Remove. They are saved with the group.

## Tag editor

`tags/:id`, a 480px dialog when opened as one: the **name**, and the **tag group**, shown only when the tag has no group yet, which happens only when the editor is opened by URL.

## Pickers

- **Select tag groups** — tag group names, ordered by name, with a name filter and pagination. Opened from the task editor, it is limited to the task's project and allows several groups to be selected; opened from the tag editor or from a filter condition, it lists the groups of every project.
- **Select tags** — tag names with a name filter and pagination. No editor opens it; it serves filter conditions on tags, such as in the time-entry lists.

## Not covered

- All of these screens: no UI test exists for them.
