# Conventions

Rules for writing and committing code that formatters and linters do not enforce and the code does not consistently show, and the rules that tests check, unless they belong to a guideline.

## Language

Code, comments, documentation, commit messages, and every other repository artifact are written in English, whatever language the conversation is in. User-visible text is the exception: see below.

## User-visible text

Every user-visible text is a message key, defined in both `messages_en.properties` and `messages_ru_RU.properties`, the bundles of the locales in `jmix.core.available-locales`. The two bundles stay key-for-key in sync: a key that one of them lacks shows that locale's users the key itself instead of a text. The labels of standard actions, such as Create, Edit, Remove, and Close, come from Jmix's bundles and its Russian translations, on the project's own actions too, and are not repeated in the project's bundles.

A text with a count, such as "5 workdays", has one key per plural form, `<key>.one`, `<key>.few`, and `<key>.many`, in both bundles, and takes the form its count needs from `PluralForms` in `app/util/`. Russian uses all three forms; English repeats its `many` text under `few`.

A required field that is not bound to an entity attribute names itself in its required message, as a bound field does; without a message it says only "The field is required". A field declared in a descriptor gets the message there, `requiredMessage="msg://<field>.requiredMessage"`, not from the controller, and both bundles word it as the framework words a bound field's, with the field's label: `%s required` in English, such as "Name required", and `Заполните поле "%s"` in Russian. The password fields of the user editor, and the tag fields the time entry editor builds in code, predate the rule.

## The current user's data

A screen that shows the current user's own data, like My time entries or My tasks, narrows it with a query on the current user, not with a row-level role.

## Soft deletion

Soft deletion is chosen per entity where it fits; it is not a rule for every entity. An entity is soft-deleted when a deleted record must keep working for the records that refer to it, as a deleted work schedule keeps the plans of its assignments; when it must come back with a soft-deleted record whose deletion cascades to it, as work schedule assignments come back with their user; or when restoring a deleted record is a real need. Otherwise it may be deleted for good. Every entity so far is soft-deleted, so the code alone suggests a project-wide rule that does not exist.

## Security roles

Security roles are granular: each grants one piece of access and is named by what it grants, such as "User: read only" or "Work schedule management". Full access, which grants everything, and the job roles are the exceptions. A larger role is composed of them rather than repeating their policies: a role in code extends the interfaces of the roles it includes, as Work schedule management extends User: read only.

A job role, such as Worker or Manager, is a composite: a resource role that only extends the roles it is made of, declares no policy of its own, and is named by the kind of work its holder does. A person is given job roles, not blocks, and beside them, as a second assignment, the one row-level role that the combination table of `docs/specs/roles/README.md` gives their job roles; Full access is the Administrator's, with no row-level role. The row-level role narrows the job roles to the projects where the person takes part, by their project role on each ([decision 0023](decisions/0023-project-roles-narrow-job-roles-through-one-row-level-role.md)); nothing assigns or checks it, so a missing one leaves the person unrestricted ([decision 0026](decisions/0026-a-persons-row-level-role-is-a-second-assignment-and-a-missing-one-fails-open.md)).

Every item of the main menu is decided for every job role: it has a row in the who-can-do-what table of `docs/specs/roles/README.md`, of its own or shared, where each job role either opens it or does not, unless it is one of the framework's administration screens, which only Full access opens. A screen added to the menu is placed in the table, and its view and menu item are granted to the job roles the table gives it, in the same change. Why: a job role is everything its holder opens, so a screen left undecided would reach only the Administrator, or reach a job role by accident through a block it includes, and the table the Administrator assigns roles by would no longer be whole.

## Custom list actions

An action of a list component that the framework does not provide, such as Make default, is a class of its own in `action/` with its own `@ActionType`, declared in the descriptor as standard actions are. It is never a click listener on a button, or a declarative action with an `actionPerformed` handler. It checks what the standard action it stands for checks: a create-like action, which needs no selection, the entity-level create permission; an action on selected items, the entity-level permission of its operation and then the row-level constraints. An action that makes a new entity from the selected one, as Copy of time entries does, is create-like: it checks only the entity-level create permission and asks the row-level constraints nothing about the selected item, since the row-level rules judge the new entity when it is saved, and a question about the selected one would refuse a copy the rules allow, such as a New copy of an Approved entry. It finds its container, loader, view, and data context, and makes the entity-level check, through `ListActionTargets`, the package's shared helper; `TimeEntryCopyAction` predates the helper and makes the check itself. It implements `AdjustWhenViewReadOnly`, so a read-only view disables it, unless it must keep working in an editor that opens read-only for lack of permission on the editor's own entity, as the assignment actions do. Why the base classes and the read-only mode call for this: `docs/guidelines/views.md`.

An action that does what another action does, a standard one or one of the project's, and differs from it only in being enabled under a narrower condition, keeps that action's type when it is used in one place, and its view's controller gives it an enabled rule (`@Install(to = "<grid>.<action>", subject = "enabledRule")`), as Time entries enables its standard Create and its Copy only for a user who may change an entry's user. Used in more than one place, or differing in how a read-only view treats it, which an enabled rule cannot express, it extends that action's class with its own `@ActionType` and overrides only the difference, as `ChildEditAction`, in the project and tag group editors, is an Edit that a read-only editor leaves enabled, so a child opens read-only from it. Either way it keeps the action's permission checks, its Read label, and the editor it opens. Why: one place does not justify a type of its own, and there the rule sits beside the screen that needs it; a type is for a difference that several screens share. An action the framework has no counterpart for, such as Close on a project's tasks (`TaskCloseAction`), is a class of its own, as above.

## Refresh in grids

A grid that loads its own data outside an editor, a list or a picker, declares the standard `list_refresh` action and no Refresh button, and its `gridEmptyState` names that action, `action="<gridId>.refresh"`: the empty state shows Refresh, and a list's right-click menu lists it. A table in an editor declares none. Why: [decision 0017](decisions/0017-refresh-lives-in-the-empty-state-not-in-the-toolbar.md).

## Shared UI pieces

A UI piece that several views show the same way, such as an avatar, the avatar cell of a user or a client, or a status badge, is built by `ComponentSupport` in `app/util/`, not in each view. Older views still build some of these pieces themselves.

When a UI piece that looks the same in several places becomes a component or a renderer of its own, every place that looks the same moves to it, including one that differs in its settings or its data: their differences become its API, not a reason to leave a place out or to share only a helper class. The avatar cell is one: the user cell and the client cell differ only in the optional line under the title and the badge.

## Entity drop-downs

An entity drop-down (`entityComboBox`) has a picker that opens the entity's list, as another way to choose its value, and a clear button, to empty the field in one click instead of selecting and deleting its text: the `entity_lookup` and `entity_clear` actions, in an editor and in a list's filter alike, and on a required field too, where the save then asks for a value. Three older fields lack one or both: the assignment editor's schedule field, the task lookup's project filter, and the field that picks the new default schedule when the default one is deleted.

## Required fields

The project's own input dialogs, a view opened as a dialog or a dialog built in code, leave the check of an empty required field to the framework, as detail views do: a field is made required the plain way, `required="true"` in the descriptor or `setRequired(true)` in code, and is never hidden, disabled, or made read-only while it becomes required to keep its mark back, which the framework's check would then pass over. So Jmix's `jmix.ui.component.immediate-required-validation-enabled`, which the application leaves at its default, on, decides for every form and dialog at once whether an empty required field is marked when it opens or only once it changes or the confirming button is pressed, and a user never meets two rules for the same thing.

## Prefix icons

Every field of a form that can take a prefix shows an icon there, whether the descriptor declares it or the controller builds it, so that the fields of a form look alike and line up, and the icon shows at a glance what kind of value a field takes, with the same icon for the same kind of value on every screen. Fields with no prefix area, such as switches, segmented bars, Markdown editors, and avatar fields, are not concerned. The Status fields of the project, task, and time entry editors show an icon chosen by their value, an icon for each status. Two kinds of field go without one. The multi-select drop-downs have no framework API for a prefix, and an icon slotted into them by hand is ruled out. The date range field's two pickers stay without one: an icon on each, about 40 px, would move the widths at which its dates stack.

## Components in a view

A component that a view shows, such as a panel, a bar, or a card of a list, is declared in the view's descriptor, with the generic `component` element when it has no element of its own, or created in the controller through `UiComponents`. The view handles its events with `@Subscribe` and gives its handlers with `@Install` wherever the component allows it, not with listeners and setters in code. Inside a component, the parts it builds are created with `new`, which is cheaper, except a Jmix component that takes beans from the application context, such as the reject dialog's Markdown editor, which comes from `UiComponents`. Older views still create some of their components with `new`. How the generic element and the annotations reach a component of the project's own: `docs/guidelines/views.md`.

## CSS classes set from Java

A component or renderer with its own Java class and its own stylesheet in the theme, such as the avatar cell, gives its parts classes that name them, such as `title`, and its stylesheet gives their look; it sets no utility classes, ones that each carry a single look such as a font size or a color. A piece built in place, with no class or stylesheet of its own, such as a grid renderer in a view, may set the project's utility classes from `app/util.css`. Lumo's utility classes are used nowhere. Aura's classes that `docs/guidelines/design-system.md` prescribes, such as `aura-accent-neutral` on a badge, are not utility classes. Why: [decision 0006](decisions/0006-a-component-with-its-own-stylesheet-sets-no-utility-classes.md).

## Breakpoints

A media or a container query on width switches at a common screen width, written in pixels, such as 480, 600, 768, 1024, 1280, or 1440 pixels, rather than at a width worked out from the content. A container query on a part that is a small share of the screen, such as a day's column on the approval screen's week board, may switch at the width its own content needs, in `rem`. A container query switches at least a scrollbar's width away from every width its container has at a common window, with the navigation drawer open or collapsed, so that a classic or an overlay scrollbar never decides the layout (`docs/guidelines/design-system.md`). Older stylesheets, such as the calendar's, predate the rule.

## SpotBugs findings

A SpotBugs finding is fixed in the code, test code included. It is excluded instead only when it is false for this application, such as a pattern the framework requires or a threat the application does not face: in `config/spotbugs/exclude.xml`, matched by the class or method it concerns unless it is false everywhere, with a comment that gives the reason. Findings are not suppressed with `@SuppressFBWarnings`: the filter is the one place exclusions live.

## Commits

[Conventional Commits](https://www.conventionalcommits.org/): `<type>: <short description>` — imperative, one subject line, no scope and no prose body. Types in use: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`.

## Branches and pull requests

A branch is named `<type>/<slug>`, with type `feature`, `fix`, `refactor`, `docs`, or `chore`, such as `fix/calendar-entry-stale-after-edit`. A pull request is squash-merged, so its title is a Conventional Commits subject; when it resolves an issue, its body starts with `Closes #<issue>.`

## Markdown

Paragraphs, list items, table rows, and blockquote lines are not hard-wrapped: each stays on one line, and the renderer wraps it. Code blocks keep their formatting verbatim.
