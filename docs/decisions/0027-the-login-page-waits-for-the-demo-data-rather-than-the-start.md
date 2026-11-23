# 0027 The login page waits for the demo data, rather than the start

Status: accepted

## Context

On a `dev` start against a database without clients, the demo data loads once the application is ready ([decision 0007](0007-demo-data-is-loaded-by-the-application-not-by-liquibase.md)), after the web server has started. A sign-in in the seconds before the load ended put the user as they were before the demo data, without a name or an avatar, into the session, and the user menu kept showing them so until the next sign-in (issue #123). A live check measured the load at about 18 s from the web server's start: about 8 s until the data was saved, then about 10 s of the listeners that run after its commit.

Alternatives considered and rejected:

- **Load before the web server starts.** No request could arrive during the load, and no screen would wait, but for those seconds the browser cannot connect: it shows its own error or keeps loading, and nothing says why.
- **A notice above a disabled form.** The language switch stays usable, but the form moves when the notice goes, and the form has to be disabled and enabled by hand.
- **The page asks for the load's state every second.** Simpler, but a page lags up to a second behind the end and keeps asking for nothing.
- **Jmix's `UiEventPublisher` ends the wait.** It reaches only the sessions whose security context holds a signed-in user, and a login page's session has none.

## Decision

Whether a start loads the demo data is decided before the web server starts. While the load runs, the login page shows a strictly modal dialog, and the server pushes its close to every page that shows it once the load is over, the listeners that run after its commit included. Decided with the user (issue #123).

## Consequences

- From the first request the web server answers, no sign-in through the login page reaches the user as they were before the demo data.
- A remembered sign-in skips the login page, so it is not held back.
- The wait lasts until the listeners that run after the commit are done, about 10 s longer than the data needs: a session signed in at the commit would get an event for each of its user's new time entries.
- Outside `dev` no page waits.
