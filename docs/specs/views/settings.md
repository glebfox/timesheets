# Settings screens

Source: `view/tasktype/`, `view/projectrole/`

The catalogs under the Settings menu: task types and project roles.

## Task types

`task-types`. Lists task types with the name and description, a name filter, and pagination, both kept in the URL; Create, Edit, and Remove. The description is cut short, with the full text in a popover that renders the Markdown. The editor (`task-types/:id`, a 640px dialog when opened as one) has the name and the description, in a Markdown editor with a preview. What a task type is: [task type](../data-model/task-type.md).

## Project roles

`project-roles`. Lists project roles with the name, the type as a badge (Manager primary, Approver green, Observer neutral, Member plain), and the description, which is cut short with the full text in a popover that renders the Markdown. It offers a name filter, a type filter, and pagination, all kept in the URL; Create, Edit, and Remove. The editor (`project-roles/:id`, a 640px dialog when opened as one) has the name, the type, and the description in a Markdown editor with a preview. What a project role is: [project role](../data-model/project-role.md).

## Not covered

- All of these screens: no UI test exists for them.
