# Conventions

Rules that tooling does not enforce and the code does not consistently show.

## Language

Code, comments, documentation, commit messages, and every other repository artifact are written in English, whatever language the conversation is in. User-visible text is the exception: see below.

## User-visible text

Every user-visible text is a message key, defined in both `messages_en.properties` and `messages_ru_RU.properties`, the bundles of the locales in `jmix.core.available-locales`. The two bundles stay key-for-key in sync.

## The current user's data

A screen that shows the current user's own data, like My time entries or My tasks, narrows it with a query on the current user, not with a row-level role.

## Shared UI pieces

A UI piece that several views show the same way, such as an avatar, a user cell, or a status badge, is built by `ComponentSupport` in `app/util/`, not in each view. Older views still build some of these pieces themselves.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `<type>: <short description>` — imperative, one subject line, no scope and no prose body. Types in use: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.

## Markdown

Paragraphs, list items, table rows, and blockquote lines are not hard-wrapped: each stays on one line, and the renderer wraps it. Code blocks keep their formatting verbatim.
