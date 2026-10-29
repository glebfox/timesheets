# 0005 Requests that open no view need only sign-in

Status: accepted

## Context

A signed-in user who typed an unknown address, such as `/no-such-page`, into the browser got Spring Boot's Whitelabel Error Page with status 403 instead of the "Nothing was found" page, while following the same address within the application showed that page (issue #10). Jmix 3.1 builds the security of the Flow UI on Vaadin 25's `VaadinSecurityConfigurer`, whose rule for a request that matches no other rule is `denyAll`: only an address that opens a view asks for a signed-in user, so Spring Security refuses an unknown address before Vaadin can show its error page. Vaadin 24, under Jmix 2, asked only for a signed-in user there. Navigation within the application is a framework-internal request, which Vaadin always lets through, so it never met the rule. Jmix 3.1.x and the 3.2 snapshots configure it the same way, and neither Jmix nor Vaadin Flow had an issue for it on 2026-10-29.

Alternatives considered and rejected:

- **Keep Vaadin's default.** Every typed unknown address keeps ending on the Whitelabel 403 page.
- **`permitAll()` for requests that match no other rule.** A signed-out user at an unknown address would see the "Nothing was found" page instead of the login page, and every address the application serves outside its views, now or later, would be open without signing in.

## Decision

`TsSecurityConfiguration` extends Jmix's `FlowuiVaadinWebSecurity`, which makes the default Flow UI security of the security-flowui module step aside, and sets the rule for requests that match no other rule to `authenticated()`. The rest is Jmix's default configuration.

## Consequences

- A signed-in user at an unknown address gets the "Nothing was found" page; a signed-out user is sent to the login page ([main view, login, and error pages](../specs/views/main.md#error-pages)).
- A request that opens no view and matches no other rule passes the security filters for any signed-in user, whatever their roles; roles are checked only when a view opens. An HTTP endpoint added outside the views needs its own security filter chain, or every signed-in user can reach it.
- The override replaces `configureVaadinSpecifics` of `FlowuiVaadinWebSecurity`, so a change a later Jmix makes there does not reach the application: a Jmix upgrade compares the override with the new default.
- Once Jmix's default asks only for a signed-in user there, the subclass can go.
