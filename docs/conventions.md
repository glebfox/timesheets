# Conventions

Rules that tooling does not enforce and the code does not consistently show.

## Language

Code, comments, documentation, commit messages, and every other repository artifact are written in English, whatever language the conversation is in. User-visible text is the exception: see below.

## User-visible text

Every user-visible text is a message key, defined in both `messages_en.properties` and `messages_ru_RU.properties`, the bundles of the locales in `jmix.core.available-locales`. The two bundles stay key-for-key in sync.

A text with a count, such as "5 workdays", has one key per plural form, `<key>.one`, `<key>.few`, and `<key>.many`, in both bundles, and takes the form its count needs from `PluralForms` in `app/util/`. Russian uses all three forms; English repeats its `many` text under `few`.

A required field that is not bound to an entity attribute names itself in its required message, as a bound field does; without a message it says only "The field is required". A field declared in a descriptor gets the message there, `requiredMessage="msg://<field>.requiredMessage"`, not from the controller, and both bundles word it as the framework words a bound field's, with the field's label: `%s required` in English, such as "Name required", and `Заполните поле "%s"` in Russian. The password fields of the user editor, and the tag fields the time entry editor builds in code, predate the rule.

## The current user's data

A screen that shows the current user's own data, like My time entries or My tasks, narrows it with a query on the current user, not with a row-level role.

## Soft deletion

Soft deletion is chosen per entity where it fits; it is not a rule for every entity. An entity is soft-deleted when a deleted record must keep working for the records that refer to it, as a deleted work schedule keeps the plans of its assignments; when it must come back with a soft-deleted record whose deletion cascades to it, as work schedule assignments come back with their user; or when restoring a deleted record is a real need. Otherwise it may be deleted for good. Every entity so far is soft-deleted, so the code alone suggests a project-wide rule that does not exist.

## Security roles

Security roles are granular: each grants one piece of access and is named by what it grants, such as "User: read only" or "Work schedule management". Full access, which grants everything, is the one exception. A larger role is composed of them rather than repeating their policies: a role in code extends the interfaces of the roles it includes, as Work schedule management extends User: read only.

## Custom list actions

An action of a list component that the framework does not provide, such as Make default, is a class of its own in `action/` with its own `@ActionType`, declared in the descriptor as standard actions are. It is never a click listener on a button, or a declarative action with an `actionPerformed` handler. It checks what the standard action it stands for checks: a create-like action, which needs no selection, the entity-level create permission; an action on selected items, the entity-level permission of its operation and then the row-level constraints. It finds its container, loader, view, and data context, and makes the entity-level check, through `ListActionTargets`, the package's shared helper; `TimeEntryCopyAction` predates the helper and makes the check itself. It implements `AdjustWhenViewReadOnly`, so a read-only view disables it, unless it must keep working in an editor that opens read-only for lack of permission on the editor's own entity, as the assignment actions do. Why the base classes and the read-only mode call for this: `docs/guidelines/views.md`.

## Shared UI pieces

A UI piece that several views show the same way, such as an avatar, a user cell, or a status badge, is built by `ComponentSupport` in `app/util/`, not in each view. Older views still build some of these pieces themselves.

## CSS classes set from Java

Java code gives a component a class that names its part, such as `title` in a user cell, and the part's stylesheet in the theme gives its look. It sets no utility classes, ones that each carry a single look such as a font size or a color: neither Lumo's nor the project's own in `app/util.css`. Aura's classes that `docs/guidelines/design-system.md` prescribes, such as `aura-accent-neutral` on a badge, are not utility classes. Why: [decision 0006](decisions/0006-java-code-sets-no-utility-css-classes.md). The task-with-project cell in `ComponentSupport` predates the rule.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `<type>: <short description>` — imperative, one subject line, no scope and no prose body. Types in use: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.

## Branches and pull requests

A branch is named `<type>/<slug>`, with type `feature`, `fix`, `refactor`, `docs`, or `chore`, such as `fix/calendar-entry-stale-after-edit`. A pull request is squash-merged, so its title is a Conventional Commits subject; when it resolves an issue, its body starts with `Closes #<issue>.`

## Markdown

Paragraphs, list items, table rows, and blockquote lines are not hard-wrapped: each stays on one line, and the renderer wraps it. Code blocks keep their formatting verbatim.
