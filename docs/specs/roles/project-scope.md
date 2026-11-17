# Project scope

Source: `security/ProjectScopeRole.java`, `app/ProjectDataAccessSupport.java`, `app/ProjectAccessSupport.java`

Row-level role `project-scope`, named "Project scope". It includes [Time entry rules](time-entry-rules.md).

## Grants

It grants nothing on its own; it narrows what resource roles grant to the projects where the person takes part, by the type of their [project role](../data-model/project-role.md) on each ([project participant](../data-model/project-participant.md)). Under user substitution it reads and judges as the substituted user.

### Reading

- [Projects](../data-model/project.md), their [tasks](../data-model/task.md), participants, [tag groups and tags](../data-model/tag-group.md): only those of projects where the person takes part, in any project role.
- [Time entries](../data-model/time-entry.md): the person's own, and every entry, in every status, of projects where they are a Manager, an Approver, or an Observer. On a project where they are a Member, they read only their own. A person's own entries stay readable when their task is deleted, and so do the entries of a deleted person on a project its Manager, Approver, or Observer reads.

Reading is narrowed for every load that starts from one of these entities, a count or another value query included, whatever screen or `DataManager` call makes it. An entity reached as a reference of one the person reads shows regardless, whether it comes with the load or is loaded later, when first read, as the narrowing applies to what a load starts from: an entry's task and its project, or a participant's project, show to whoever reads the entry or the participant. This is an accepted limit: a person who has left a project still sees its name and its task on their own entries there.

### Writing

- A project is changed only by its Manager. Creating and removing projects is not narrowed: whoever a resource role lets create or remove a project does so.
- Tasks, participants, tag groups, and tags are created, changed, and removed only on a project where the person is the Manager; a tag belongs to the project of its tag group. Moving one to another project, or a tag to a tag group of another project, needs the person to be the Manager of both.
- A save is judged by the participants as stored before it, so no one makes themselves a project's Manager, neither by adding a participant row nor by changing the role of their own.
- Time entries are written by the rules of [Time entry rules](time-entry-rules.md).

The rules hold on every save, whatever screen or `DataManager` call makes it; a save they refuse fails with an access-denied exception and saves nothing. A screen that asks about project data it shows, such as the project editor whether it opens read-only, or its dialogs of tasks, participants, tag groups, and tags, gets yes only on a project where the person is the Manager.

## Holders

None in code or seed data. [Client scope](client-scope.md) includes it.

## Not covered

- Reading and writing through a constrained `DataManager`, the acting user holding Full access and this role, and the screens' questions about project data loaded as the project editor and the tag group editor load it: `ProjectScopeRoleTest`. No test opens the project list, the project editor, its dialogs, the Time entries list, or the approval screen as a holder.
- A task or a project loaded later, when first read, showing as one that comes with the load does: read from the framework, which applies no JPQL policy to such a load; `ClientScopeRoleTest` checks it for a user.
- A deleted participant opens nothing, and a participant whose project role is deleted reads the project's data but, of its time entries, only their own, and changes nothing: read from the queries, no test.
