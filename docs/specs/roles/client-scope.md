# Client scope

Source: `security/ClientScopeRole.java`, `demo/DemoDataLoader.java`

Row-level role `client-scope`, named "Client scope". It includes [Project scope](project-scope.md), and through it [Time entry rules](time-entry-rules.md).

## Grants

It grants nothing on its own; it is the row-level role of a project's client, who reads the projects where they take part and the time logged there. Besides everything Project scope narrows, it narrows reading two entities that Project scope leaves open, because other holders of Project scope need all of them, such as whoever manages work schedules or absences:

- [Users](../data-model/user.md): the person themselves, and the people who take part, in any [project role](../data-model/project-role.md), in the projects where the person takes part ([project participant](../data-model/project-participant.md)).
- [Clients](../data-model/client.md): the clients of the projects where the person takes part.

Under user substitution it reads as the substituted user.

Reading is narrowed for every load that starts from a user or a client, a count or another value query included, whatever screen or `DataManager` call makes it. Only the participants as they stand count: a person who has left the projects where the holder takes part, and takes part in none of them now, is not read, and neither are the people and the client of a deleted project, unless they belong to another of the holder's projects.

A user or a client reached as a reference of an entity the person reads shows regardless, whether it comes with the load or is loaded later, when first read, as the narrowing applies to what a load starts from: an entry's user shows to whoever reads the entry, a person who has left the project included, and a project's client to whoever reads the project. This is an accepted limit, as it is for Project scope.

Signing in and switching to a substituted user are not narrowed: they read the user apart from any role.

Writing users and clients is not narrowed; what the role narrows for writing is Project scope's.

## Holders

- The demo user `gman`, in the `dev` demo data only, beside [Observer](observer.md).

## Not covered

- Reading users, clients, and projects through a constrained `DataManager`, the acting user holding Full access and this role, and an entry's user that a holder reads only as a reference, with the load and loaded later: `ClientScopeRoleTest`. The rest of what Project scope narrows is checked for holders of Project scope (`ProjectScopeRoleTest`), not of this role.
- No test opens a screen as a holder, such as the user list, the client list, or a picker of users or clients.
- The people and the client of a deleted project not being read, a client loaded later when first read, reading as a substituted user, counts and other value queries of users and clients being narrowed, and signing in, and switching to a substituted user who takes part in none of the holder's projects, not being narrowed: read from the policies and the framework, no test for this role.
