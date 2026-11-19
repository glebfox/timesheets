# 0026 A person's row-level role is a second assignment, and a missing one fails open

Status: accepted

## Context

Job roles are resource roles and narrow nothing: a row-level role narrows them to the projects where the person takes part ([decision 0023](0023-project-roles-narrow-job-roles-through-one-row-level-role.md)). In Jmix 3.1 a resource role composes only with resource roles, so a job role cannot carry its row-level role, and a person's authorities are built from their role assignment rows only: a person who holds a job role without a row-level role is unrestricted. Row-level roles narrow one another rather than add up, so each combination of job roles needs one row-level role, which a table gives (`docs/specs/roles/README.md`).

Alternatives considered and rejected:

- **Derive the row-level role from the person's job roles when their authorities are built at sign-in** (`security/DatabaseUserRepository.java`), with one assignment per person and nothing to forget. Declined by the user: the row-level role a person works under would not show on the Role assignments screen.

## Decision

The Administrator assigns each person their job roles and, as a second assignment, the row-level role the combination table gives their job roles. Nothing assigns it by itself and nothing checks it: the setup fails open, and that is accepted in writing. Decided by the user (issue #23).

## Consequences

- A person given a job role without the row-level role, or with the wrong one, is unrestricted, or narrowed wrongly, and no screen shows it: a Manager without Project scope reads and changes every project and every time entry, Closed ones included.
- Every role a person works under shows on the Role assignments screen.
- The roles' README states the combination table and the risk where an Administrator reads how to assign roles, and each job role's spec says what its holder can do without a row-level role.
- The `dev` demo data gives each demo user the row-level role of the table as a row-level assignment.
