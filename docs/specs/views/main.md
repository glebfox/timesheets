# Main view, login, and error pages

Source: `view/main/`, `view/login/`, `router/error/`, `menu.xml`, `security/UiMinimalRole.java`

The application frame around every screen, the login page, and the error pages.

## Main view

The frame of the signed-in application. Screens open as tabs in its work area; tabs can be dragged, and each offers closing itself, the others, or all.

- **Header** — the menu toggle, the "Timesheets" title, and the user menu. The user menu button shows the user's avatar and full name (the username when both names are empty), and marks a substituted user. Its header shows the name with the username beneath it. Its items are Substitute user, the theme switch, and Log out.
- **Menu** — Timesheets (Calendar, My time entries, My tasks), Project Management (Clients, Projects, Time entries), Settings (Task types, Project roles, Work schedules, Holiday calendars), and Administration (Users) start expanded; Security, Data Tools, and Pessimistic locking hold the framework's own screens and start collapsed. A user sees only the items their roles allow.
- **Rejected badge** — the My time entries item carries a red badge with the number of the current user's Rejected [time entries](../data-model/time-entry.md#status), hidden when there are none. It refreshes while the user is signed in, whenever one of their entries changes status or is deleted, by anyone and from any session.

## Login

`login`. A form with username, password, "remember me", and a language choice of English or Russian that starts at the browser's language; there is no "forgot password". The `dev` profile pre-fills `admin`/`admin`. A failed sign-in shows the form's error without saying why: wrong credentials, an inactive or locked user, and a user without UI access look alike.

## Error pages

An unknown address followed within the application shows a "Nothing was found" page with a link back to the application. An unknown address typed into the browser never reaches the application: a signed-in user gets the server's plain error page (Spring Boot's Whitelabel Error Page) with status 403, and a signed-out user is sent to the login page. Why such addresses stay forbidden: [decision 0005](../../decisions/0005-addresses-that-open-no-view-stay-forbidden.md).

A screen the user may not open does not open, whether its address is followed or typed into the browser: an "Access denied" notification is shown instead, over the main view when the address was typed. The application also has an "Access denied" page, which it never shows: tabbed mode checks the user's access to a screen before the page could appear.

## Not covered

- The live refresh of the rejected badge: it needs two live sessions (`docs/guidelines/testing.md`).
- What an address typed into the browser gets before any screen opens, the 403 and the redirect to the login page: no test sends a request through the security filters.
- The rest of these screens: no UI test exists for them.
