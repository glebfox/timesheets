# 0005 Addresses that open no view stay forbidden

Status: accepted

## Context

A signed-in user who types an unknown address, such as `/no-such-page`, into the browser gets Spring Boot's Whitelabel Error Page with status 403 instead of the "Nothing was found" page, while following the same address within the application shows that page (issue #10). Jmix 3.1 builds the security of the Flow UI on Vaadin 25's `VaadinSecurityConfigurer`, whose rule for a request that matches no other rule is `denyAll`: only an address that opens a view asks for a signed-in user, so Spring Security refuses an unknown address before Vaadin can show its error page. Vaadin 24, under Jmix 2, asked only for a signed-in user there. Navigation within the application is a framework-internal request, which Vaadin always lets through, so it never meets the rule. A plain Vaadin 25 application behaves the same, except that it shows the browser's own error page, because it does not open Spring Boot's error path to everyone as Jmix does.

Alternatives considered and rejected:

- **`authenticated()` for requests that match no other rule.** It was built and checked (PR #40, closed unmerged): a typed unknown address showed the "Nothing was found" page. But every address the application serves outside its views would pass the security filters for any signed-in user, whatever their roles: a REST endpoint added later, any other HTTP endpoint, and every static file the application ships outside Vaadin's public folders, such as the error page templates and icons. Each would need its own security filter chain to be closed again, and one forgotten chain leaves it open.
- **`permitAll()` for requests that match no other rule.** The same, and open to signed-out users too.

## Decision

The application keeps the default of Jmix and Vaadin: an address that opens no view and matches no other rule is refused for everyone. Not opening more than the views need is worth more than the "Nothing was found" page for a typed address. Decided by the user.

The error pages stay in the application.

## Consequences

- A typed unknown address gets the server's plain 403 page when signed in and the login page when signed out ([main view, login, and error pages](../specs/views/main.md#error-pages)); the "Nothing was found" page appears only for an unknown address followed within the application.
- An HTTP endpoint added outside the views, such as a REST endpoint, is refused for everyone until it gets its own security filter chain, which must state who may call it.
- Showing the "Nothing was found" page for a typed address takes a way that opens nothing beyond the views.
