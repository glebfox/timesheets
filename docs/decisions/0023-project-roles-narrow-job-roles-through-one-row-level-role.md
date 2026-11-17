# 0023 Project roles narrow job roles through one row-level role

Status: accepted

## Context

Until issue #23 every active demo user but one held Full access, and project roles (Manager, Approver, Member, Observer) granted nothing: they decided only whose time entries a user approves and whom the dashboard's My team shows. Each person must do the work of their job, on the projects where they take part and nowhere else, whatever reaches the data: a screen, a `DataManager` call, or the AI assistance of #88, which reads with the user's entity, attribute, and row-level permissions and nothing more. A person's part differs from project to project: the Manager of one project may be a Member of another.

In Jmix 3.1 resource roles add up and row-level roles narrow: the row-level policies of all of a person's row-level roles combine with AND, and a role composes only with roles of its own kind.

Alternatives considered and rejected:

- **The screens alone narrow**, with queries on the user's projects and editors opened read-only, and no row-level role. The AI assistant, a REST endpoint, or a new screen would read and change everything (issue #23, comment c).
- **A row-level role per project role type**, such as one for Managers and one for Members. Two row-level roles narrow each other instead of adding up, so the Manager of one project who is a Member of another would get only what both allow.

## Decision

Two axes. A person's job role, a resource role composed of granular blocks, says what kind of work they do and which screens they have; their project role, set per project on its Participants tab, says where. One row-level role, Project scope, reads the person's project role on each project, in its JPQL read policies and its create, update, and delete predicates, and narrows whatever the job roles grant to what the project role allows there. Client scope, for a project's client, includes it and also narrows users and clients; Time entry rules, which Project scope includes, holds the write rules of time entries. A person does something on a project only when both their job role and their project role allow it. Decided by the user (issue #23).

## Consequences

- A project role above the person's job role is not checked: the person reads what Project scope opens but has no screen to act on it. A warning on the Participants tab would mean reading other people's security roles from the application, which is not wanted.
- A JPQL read policy narrows only what a load starts from, so a reference of an entity the person reads shows regardless, as an entry's task and project do on the person's own entries of a project they have left; accepted.
- The read policies are JPQL `where` clauses only, never a `join` and never a predicate read policy, as the AI datasets of #88 refuse both.
- A save of project data or of a time entry by a holder costs a query or two for the person's part in the project.
- Only Project office and Full access work across every project: Project office gets Time entry rules alone, and Full access no row-level role. Which row-level role a person gets depends on their job roles, and it is a second assignment: [decision 0026](0026-a-persons-row-level-role-is-a-second-assignment-and-a-missing-one-fails-open.md).
