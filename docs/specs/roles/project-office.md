# Project office

Source: `security/ProjectOfficeRole.java`

Resource role `project-office`, named "Project office", a job role: a person who sets up clients and projects across the company and logs their own time. It includes [Worker](worker.md), [Client management](client-management.md), [Projects: create and remove](project-creation.md), [Projects: management](project-management.md), and [Task types and project roles](project-catalogs.md), and grants nothing besides.

## Grants

- Everything [Worker](worker.md) grants.
- The [client list](../views/clients.md#clients) and the client editor, with the menu item.
- The [project list](../views/projects.md#projects) and its menu item, and the project editor with its dialogs: creating, changing, and removing projects and their tasks, participants, tag groups, and tags.
- The [task type](../views/settings.md#task-types) and [project role](../views/settings.md#project-roles) lists and editors, with their Settings menu items.
- The user list, as the picker of the participant editor's user field and of Add participants, with no menu item. Tabbed mode also opens it by its URL, so a holder reaches it that way, read only: the user editor is not granted, and the list offers no Edit.

No Time entries list and no approval screen.

## Row-level role

[Time entry rules](time-entry-rules.md), assigned beside the job role whatever other job roles the person holds, Full access aside, with which they get none ([job roles and their row-level role](README.md#job-roles-and-their-row-level-role)). It narrows no reading and nothing of project data, so a holder reads and changes every project, also when they approve or manage, and writes only their own time entries, by the time entry rules: they create entries only for themselves and only on projects where they take part in a role other than Observer, and change or remove nobody else's. With [Approver](approver.md) or [Manager](manager.md) besides, they still approve and reject only the entries the approval rule names, on the projects where they are an Approver or a Manager.

As the rules narrow no reading, a holder reads every time entry too. No screen lists other people's entries, but the [time entry editor](../views/time-entries.md#editor), opened by its URL, `time-entries/:id`, shows any entry, read only, as it shows every holder of [Time logging](time-logging.md#grants) the entries their row-level role lets them read; for Project office that is every entry.

Without a row-level role the holder changes and removes anyone's entries in any status, Closed ones included. Nothing assigns it by itself, and no screen shows that it is missing.

## Holders

None in code or seed data.

## Not covered

- The menu items a holder opens and those they do not, signing in with the job role alone, and creating time entries, for a holder with Time entry rules: `JobRolesTest`, which also checks that a holder saves and changes their own entry but neither saves one for another person nor changes or removes another person's, and that a holder of this role and Approver reads every project and approves only by the approval rule. The user list as a view a holder may open is checked for Projects: management (`ProjectBlocksRoleTest`), not for this role. What each included role grants is checked by that role's own tests.
- No test opens a screen as a holder. The time entry editor showing another person's entry was seen once in a UI test written to look and not kept.
- A holder without a row-level role, or with another one: not checked.
