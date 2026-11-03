# 0013 The framework's icons are Gravity UI counterparts of Jmix's

Status: accepted

## Context

The screens' own icons come from Gravity UI, while the framework's standard actions and components drew Jmix's Vaadin icons, so a toolbar or an editor mixed two styles. Issue #71 asked for Gravity UI icons there too. Jmix names each such icon by its purpose, such as `CREATE_ACTION` or `DETAIL_CLOSE_ACTION`, and its theme maps each name to a Vaadin glyph; several names share one, such as `ban` for Cancel, Close, and No.

Alternatives considered and rejected:

- **Set the icons view by view, in descriptors and controllers.** Every standard action of every view would name its icon, and the framework's own components, such as the filters and the pickers, cannot be reached that way.
- **Choose each icon anew by its meaning.** It reopens every icon with no rule to settle it. Cancel as `xmark` was the closest call: `xmark` is the plainer Cancel, but in this application it already means Reject on the approval screen, beside the reject dialog's Cancel, and Close task in the project editor's task toolbar, on the same screen as the editor's Cancel.

## Decision

`TsIcons` replaces Jmix's `Icons` bean, and each name it lists takes the Gravity UI counterpart of the glyph Jmix's theme gives it, or the nearest icon where Gravity UI has none. Save is the one exception: a floppy disk, where Jmix draws an archive box. Cancel keeps `ban`. Decided by the user (issue #71).

## Consequences

- One class decides the framework's icons. An add-on brought in later keeps Jmix's icons until its names are added there.
- Icons the framework creates without the bean keep Jmix's: the buttons of the standard option and input dialogs, the pagination, the combo button's arrow, the twin column's buttons, and the close button on a tab. Replacing Jmix's `Dialogs` bean would reach the dialogs' buttons; it was left for later.
