# Client management

Source: `security/ClientManagementRole.java`

Resource role `client-management`, named "Client management".

## Grants

- [Clients](../data-model/client.md): creating, reading, changing, and removing them, every attribute modifiable.
- The [client list](../views/clients.md#clients) and its Project Management menu item, and the [client editor](../views/clients.md#client-editor).

It grants no project screens and no signing in: a holder also needs a role that does, such as [UI: minimal access](ui-minimal.md).

## Holders

None in code or seed data.

## Not covered

- The screens as a holder sees them: `ProjectBlocksRoleTest` checks the grants with the UI's entity, attribute, view, and menu contexts, and no UI test opens the client list or editor as a holder.
- A holder who lacks UI: minimal access: not checked.
