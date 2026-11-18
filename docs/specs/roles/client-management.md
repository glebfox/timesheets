# Client management

Source: `security/ClientManagementRole.java`

Resource role `client-management`, named "Client management".

## Grants

- [Clients](../data-model/client.md): creating, reading, changing, and removing them, every attribute modifiable.
- The [client list](../views/clients.md#clients) and its Project Management menu item, and the [client editor](../views/clients.md#client-editor).

It grants no project screens and no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## Holders

None directly. [Project office](project-office.md) includes it; no user of the `dev` demo data holds it.

## Not covered

- The screens as a holder sees them: `ProjectBlocksRoleTest` checks the grants with the UI's entity, attribute, view, and menu contexts. `ProjectManagementRoleUiTest` opens the client list only as the project editor's client picker, for a holder who also has Projects: management, where Edit opens the client editor; no UI test opens the list from the menu, or creates, changes, or removes a client, as a holder.
- A holder who lacks UI: minimal access: not checked.
