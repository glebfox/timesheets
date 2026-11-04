# 0014 Commercial Jmix add-ons are allowed

Status: accepted

## Context

The repository is public under the Apache License 2.0, and some Jmix add-ons are commercial: the Jmix premium repository serves them only with credentials. The application already depends on one, the Tabbed Application Mode add-on, so building it already needs those credentials (`README.md`), and the repository's CI has them as secrets.

Alternatives considered and rejected:

- **Open-source add-ons only, so that anyone can build the public repository without credentials.** The project is past that point with Tabbed Mode, and a capability the project needs would then be built in the project or done without.

## Decision

A commercial Jmix add-on is weighed on what it does, like any other: its license and the credentials the build needs are no argument against it. Decided with the user on 2026-10-31, when the Kanban add-on was weighed for a week board of time entries (#50).

## Consequences

- Building the application needs credentials for the Jmix premium repository, as it already did.
- The Kanban add-on was rejected for its fixed tile content and its column headers, which take no content or actions of the project's own, not for its license (#50).
