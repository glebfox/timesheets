# FullCalendar

How the Jmix FullCalendar add-on behaves where the calendar screen depends on it. The screen itself: `docs/specs/views/calendar.md`.

## Dropping an event

- The container data provider writes the new date into the bound item before `EventDropEvent` reaches the view, so an entity bound directly would already be modified when the view decides the move is not allowed.
- `EventDropEvent` cannot revert the drop ([jmix#4696](https://github.com/jmix-framework/jmix/issues/4696)); the only way to move the event back is to replace the item in the container.
- That is why the calendar binds `TimeEntryCalendarWrapper` rather than `TimeEntry`: the wrapper's date setter only buffers the date, and the view applies or discards it after checking whether the user may update the entry.

## Day cell bottom text

- The add-on asks the server for the text and class names of every visible day after each refetch of events: after navigation and whenever the bound container changes. The requests are asynchronous, so the generators must read state that is already up to date when the container change event is handled.
- A `null` text leaves the previous text in place, so the generator always returns a text, `00:00` for a day without entries.
- Class names returned by the class-names generator are added to the element and never removed, so a day whose state changes would keep the old class as well. The view resets the class names on each container change, before the add-on regenerates them.

## Locale

- The first day of the week comes from the add-on's own message bundle (`i18n.firstDayOfWeek`: Sunday in English, Monday in Russian), not from the Java locale. Code that needs the calendar's weeks takes them from the dates the calendar displays.
- Reloading the page keeps the displayed dates, so the view must not assume it starts at the current month.
