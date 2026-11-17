# Manager

Source: `security/ManagerRole.java`

Resource role `manager`, named "Manager", a job role: a person who runs projects. It includes [Approver](approver.md), [Projects: management](project-management.md), and [Time entries: read only](time-entry-read.md), and through them every role those include, and grants nothing besides.

## Grants

- Everything [Approver](approver.md) grants: logging one's own time, the approval screen, and the user list and editor, read only.
- The [project list](../views/projects.md#projects) and its menu item, and the project editor with its dialogs: changing projects, and creating, changing, and removing their tasks, participants, tag groups, and tags. Not creating or removing projects.
- The Time entries list ([time entry screens](../views/time-entries.md#lists)) and its menu item, read only: it offers no Create or Copy, as a holder may not set an entry's user, and the editor opens another person's entry read only.
- The client, task type, and project role lists, as the pickers of the project editor's fields, with no menu items. Tabbed mode also opens each by its URL, so a holder reaches them that way, read only: their editors are not granted, and the lists offer no Edit.

On the holder's own New or Rejected entry, which the [time entry editor](../views/time-entries.md#editor) opens for changes, the editor's Status field is editable: a holder sets it to Approved or Rejected by hand and saves, as the approval rule lets a Manager review their own entries. Any other status, or a status changed together with anything else, is refused with an access-denied error.

## Row-level role

[Project scope](project-scope.md), assigned beside the job role ([job roles and their row-level role](README.md#job-roles-and-their-row-level-role)); [Time entry rules](time-entry-rules.md) instead when the person also holds Project office, and none with Full access. With Project scope a holder changes project data only on the projects where they are the Manager, and the project editor opens read-only on the other projects where they take part; they read every entry of the projects where they are a Manager, an Approver, or an Observer, which the Time entries list shows, opening other people's entries read only; otherwise they work as an [Approver](approver.md#row-level-role) does.

Without a row-level role the holder is unrestricted: they read and change every project and every time entry, Closed ones included. Nothing assigns it by itself, and no screen shows that it is missing.

## Holders

None in code or seed data.

## Not covered

- The menu items a holder opens and those they do not, signing in with the job role alone, and creating time entries, for a holder with Project scope: `JobRolesTest`. The client, task type, and project role lists as views a holder may open are checked for Projects: management (`ProjectBlocksRoleTest`), not for this role. What each included role grants is checked by that role's own tests.
- The approval screen opened by a holder with Project scope, listing a member of the managed project with their entry, and the Time entries list, which offers no Create or Copy and opens another person's entry read only: `JobRolesUiTest`. The project screens are checked for Projects: management with Project scope (`ProjectManagementRoleUiTest`), not for this role.
- The Status field on the holder's own entry, editable and saved with Approved: no test; seen once in a UI test written to look and not kept.
- A holder without a row-level role, or with another one: not checked.
