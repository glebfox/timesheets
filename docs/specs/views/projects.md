# Project screens

Source: `view/project/`, `view/projectparticipant/`

The project list, the project editor, and the participant editor. What a project is: [project](../data-model/project.md), [project participant](../data-model/project-participant.md).

## Projects

`projects`, menu Project Management. Lists projects with the name, client, status as a badge (Open green, Closed red), and description, which is cut short with the full text in a popover that renders the Markdown. It offers a filter on any attribute and pagination, both kept in the URL, and Create, Edit, and Remove. It is also the project picker of the other editors.

## Project editor

`projects/:id`, a 1024px dialog when opened as one.

- **Name**, **description** — the description in a Markdown editor with a preview.
- **Client** — a drop-down showing each client's logo and name, or the client picker.
- **Status** — Open or Closed.

Three tabs below hold what the project owns. Their changes, including those made in the dialogs they open, reach the database only when the project is saved. The one exception is a project role created from the participant editor: it is saved at once.

- **Tasks** — name, type, status as a badge (Active green, Inactive red), and description, cut short with the full text in a popover that renders the Markdown. Create and Edit open the [task editor](tasks.md#task-editor) in a dialog; Remove; **Close** turns the selected Active task Inactive and does nothing to an Inactive one.
- **Participants** — the user (avatar, name, and an Active or Inactive badge) and the role as a badge colored by role type: Manager primary, Approver green, Observer neutral, Member plain. Create and Edit open the participant editor in a dialog; Remove; **Add participants** lists every project role, and choosing one opens the user picker in multi-select mode and adds each chosen user with that role. Users who already participate are not filtered out: adding one again breaks the one-participation rule, and the save fails on PostgreSQL.
- **Tag groups** — the name. Create and Edit open the [tag group editor](tags.md#tag-group-editor) in a dialog; Remove.

## Participant editor

`project-participants/:id`, a 480px dialog when opened as one.

- **User** — a drop-down searched by username, first name, or last name, case-insensitively, showing each user's avatar, name, and Active or Inactive badge; or the user picker.
- **Project** — shown only when the participant has no project yet, which happens only when the editor is opened by URL.
- **Role** — a drop-down of project roles by name, showing each role's type beneath it; or the role picker. Its Create action opens the [project role editor](settings.md#project-roles) in a dialog and selects the new role.

## Not covered

- All of these screens: no UI test exists for them.
- The failed save when a participating user is added again: PostgreSQL only, so no test can check it.
