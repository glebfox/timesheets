/*
 * Copyright 2026 Gleb Gorelov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.glebfox.timesheets.view.timeentry.approval;

import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ApprovalPerson;
import com.glebfox.timesheets.app.util.ApprovalTotals;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.ScrollIntoViewOption;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableFunction;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import org.jspecify.annotations.Nullable;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The selected person's part of the approval screen: a header with the person, four figures of the period (Logged,
 * Waiting, Approved, and Rejected), and the period itself. A month is a {@link ApprovalMonthGrid} with the entries of
 * one week below it, grouped by day; a week is a {@link ApprovalWeekBoard}. Only the period's own days count, in the
 * figures, the grid, and the list alike.
 * <p>
 * The panel only shows what it is given. A click on a day or a week of the month grid, on an entry's ✓ or ✕, and on
 * a button that approves a person, a week, or a day, is passed on as an event of the panel, whose listener decides
 * what follows, such as {@link #showWeek}. Those buttons are in the {@code actions} of the person, the week card, and
 * the day, one for each that has waiting entries; which entries a button approves is asked of the function the panel
 * is created with, so that the count on a button and what the click approves come from one place. The ids of the
 * buttons are {@code approvePersonBtn}, {@code approveWeekBtn-<first day>}, and {@code approveDayBtn-<day>}.
 * <p>
 * The panel has the class {@code approval-person-panel}. Its parts are the {@code header}, with the {@code avatar},
 * the {@code name}, the {@code details} (the roles and the projects), and the {@code actions} of the person; the
 * {@code figures}, each a {@code figure} with the id {@code figure-logged}, {@code figure-waiting},
 * {@code figure-approved}, or {@code figure-rejected} and the parts {@code label}, {@code value}, and {@code details};
 * and, for a month, the {@code entries} of a week, with an {@code entries-header} and an {@code entries-day} for each
 * day with entries, whose {@code day-header} has the {@code date}, a {@code separator}, the {@code hours}, and the
 * {@code actions} of the day.
 * <p>
 * The panel is a container so that a lookup of components by id, as Jmix does it, reaches its parts.
 */
final class ApprovalPersonPanel extends Composite<Div> implements HasComponents {

    static final String CLASS_NAME = "approval-person-panel";

    private static final int DAYS_IN_WEEK = 7;

    private final ComponentSupport componentSupport;
    private final Messages messages;
    private final MetadataTools metadataTools;
    private final Locale locale;
    private final ApprovalPeriodTitle titles;

    @Nullable
    private ApprovalPeriod period;
    private Map<LocalDate, List<TimeEntry>> entriesByDay = Map.of();
    @Nullable
    private ApprovalMonthGrid monthGrid;
    @Nullable
    private Div entryList;
    private final Map<LocalDate, Div> dayGroups = new HashMap<>();
    // The buttons of what is drawn with the person, and those of the week listed, which change with the week
    private final ApprovalBulkApproveButtons drawnButtons;
    private final ApprovalBulkApproveButtons listedButtons;

    /**
     * Creates an empty panel.
     *
     * @param componentSupport builds the avatar and the badges, and formats days and times
     * @param messages         gives the texts
     * @param metadataTools    formats the entries' attributes
     * @param locale           the locale of the texts
     * @param titles           formats the dates of a week
     * @param entriesToApprove gives the entries that a bulk approve of a stretch of the shown period takes
     */
    ApprovalPersonPanel(ComponentSupport componentSupport, Messages messages, MetadataTools metadataTools,
                        Locale locale, ApprovalPeriodTitle titles,
                        SerializableFunction<ApprovalScope, List<TimeEntry>> entriesToApprove) {
        this.componentSupport = componentSupport;
        this.messages = messages;
        this.metadataTools = metadataTools;
        this.locale = locale;
        this.titles = titles;
        this.drawnButtons = new ApprovalBulkApproveButtons(entriesToApprove, this::fireApproveScopeClick);
        this.listedButtons = new ApprovalBulkApproveButtons(entriesToApprove, this::fireApproveScopeClick);

        getContent().addClassName(CLASS_NAME);
    }

    /**
     * Chooses the week to list when a person's month is opened: the first week with waiting entries, otherwise the
     * last week with entries.
     *
     * @param person         the person
     * @param period         the month
     * @param firstDayOfWeek the day a week starts on
     * @return the first day of the week, a row of the month's grid
     */
    static LocalDate openingWeekOf(ApprovalPerson person, ApprovalPeriod period, DayOfWeek firstDayOfWeek) {
        List<TimeEntry> entries = entriesIn(person, period);
        LocalDate day = entries.stream()
                .filter(entry -> entry.getStatus() == TimeEntryStatus.NEW)
                .map(TimeEntry::getDate)
                .findFirst()
                .orElse(entries.isEmpty() ? period.end() : entries.getLast().getDate());
        return ApprovalPeriod.weekStartOf(day, firstDayOfWeek);
    }

    /**
     * Shows a person's period.
     *
     * @param person       the person with their entries
     * @param roleNames    the names of the person's roles on the projects of their entries
     * @param period       the period, a month or a week
     * @param selectedWeek for a month, the first day of the week to list, a row of the month's grid; {@code null}
     *                     for a week
     * @throws IllegalArgumentException if a month comes without a week to list, or a week with one
     */
    void show(ApprovalPerson person, List<String> roleNames, ApprovalPeriod period, @Nullable LocalDate selectedWeek) {
        Preconditions.checkArgument((period.mode() == ApprovalMode.MONTH) == (selectedWeek != null),
                "A month needs the week to list, and a week takes none");
        clear();
        this.period = period;

        List<TimeEntry> entries = entriesIn(person, period);
        entriesByDay = entries.stream().collect(Collectors.groupingBy(TimeEntry::getDate));
        List<DayOfWeek> daysOfWeek = componentSupport.getDaysOfWeek();

        Div panel = getContent();
        panel.add(createHeader(person, roleNames, entries, period), createFigures(ApprovalTotals.of(entries)));

        if (selectedWeek != null) {
            monthGrid = new ApprovalMonthGrid(period, daysOfWeek, entriesByDay, componentSupport, messages, locale,
                    titles,
                    (day, fromClient) -> fireEvent(new DayClickEvent(this, fromClient, day)),
                    (weekStart, fromClient) -> fireEvent(new WeekClickEvent(this, fromClient, weekStart)));
            entryList = new Div();
            entryList.addClassName("entries");
            panel.add(monthGrid, entryList);
            for (List<LocalDate> row : period.weekRows(daysOfWeek.getFirst())) {
                addWeekButton(monthGrid.getWeekActions(row.getFirst()), row, period);
            }
            showWeek(selectedWeek);
        } else {
            ApprovalWeekBoard board = new ApprovalWeekBoard(period, daysOfWeek, entriesByDay,
                    entry -> createEntryCard(entry, true), componentSupport, messages, locale);
            panel.add(board);
            for (LocalDate day : period.days()) {
                addDayButton(drawnButtons, board.getDayActions(day), day);
            }
        }
        drawnButtons.refresh();
    }

    /**
     * Shows nothing, as when nobody is selected.
     */
    void clear() {
        getContent().removeAll();
        period = null;
        entriesByDay = Map.of();
        monthGrid = null;
        entryList = null;
        dayGroups.clear();
        drawnButtons.clear();
        listedButtons.clear();
    }

    /**
     * Lists the entries of a week of the month shown, grouped by day, only the month's days, and marks the week in
     * the grid.
     *
     * @param weekStart the first day of a row of the month's grid
     * @throws IllegalStateException    if the panel does not show a month
     * @throws IllegalArgumentException if no row of the grid starts on the day
     */
    void showWeek(LocalDate weekStart) {
        Preconditions.checkState(monthGrid != null && entryList != null && period != null,
                "Only a month lists the entries of a week");
        monthGrid.setSelectedWeek(weekStart);

        entryList.removeAll();
        dayGroups.clear();
        listedButtons.clear();

        ApprovalPeriod month = period;
        List<LocalDate> days = weekStart.datesUntil(weekStart.plusDays(DAYS_IN_WEEK)).filter(month::contains).toList();
        List<TimeEntry> weekEntries = days.stream()
                .flatMap(day -> entriesByDay.getOrDefault(day, List.of()).stream())
                .toList();

        H4 title = new H4(titles.formatDates(days.getFirst(), days.getLast()));
        Div header = new Div(title);
        header.addClassName("entries-header");
        ApprovalStatusBadges.of(ApprovalTotals.of(weekEntries), componentSupport, messages, locale)
                .forEach(header::add);
        entryList.add(header);

        if (weekEntries.isEmpty()) {
            Span empty = new Span(messages.getMessage(ApprovalPersonPanel.class, "entries.empty", locale));
            empty.addClassName("empty");
            entryList.add(empty);
            return;
        }

        for (LocalDate day : days) {
            List<TimeEntry> dayEntries = entriesByDay.getOrDefault(day, List.of());
            if (!dayEntries.isEmpty()) {
                Div group = createDayGroup(day, dayEntries);
                dayGroups.put(day, group);
                entryList.add(group);
            }
        }
        listedButtons.refresh();
    }

    /**
     * Scrolls the list of a week's entries to a day: to the day's entries, or to the list itself for a day without
     * any. Does nothing unless the panel shows a month.
     *
     * @param day a day of the week listed
     */
    void scrollToDay(LocalDate day) {
        Component target = dayGroups.containsKey(day) ? dayGroups.get(day) : entryList;
        if (target != null) {
            target.scrollIntoView(ScrollIntoViewOption.Behavior.SMOOTH, ScrollIntoViewOption.Block.START);
        }
    }

    /**
     * Adds a listener of clicks on the days of the month grid; a day of a neighbouring month is not clicked.
     *
     * @param listener the listener
     * @return a registration that removes the listener
     */
    Registration addDayClickListener(ComponentEventListener<DayClickEvent> listener) {
        return addListener(DayClickEvent.class, listener);
    }

    /**
     * Adds a listener of clicks on the week cards of the month grid.
     *
     * @param listener the listener
     * @return a registration that removes the listener
     */
    Registration addWeekClickListener(ComponentEventListener<WeekClickEvent> listener) {
        return addListener(WeekClickEvent.class, listener);
    }

    /**
     * Adds a listener of clicks on the buttons that approve the waiting entries of a person's period, a week, or a day.
     *
     * @param listener the listener
     * @return a registration that removes the listener
     */
    Registration addApproveScopeClickListener(ComponentEventListener<ApproveScopeClickEvent> listener) {
        return addListener(ApproveScopeClickEvent.class, listener);
    }

    /**
     * Adds a listener of clicks on the ✓ of an entry's card.
     *
     * @param listener the listener
     * @return a registration that removes the listener
     */
    Registration addApproveEntryClickListener(ComponentEventListener<ApproveEntryClickEvent> listener) {
        return addListener(ApproveEntryClickEvent.class, listener);
    }

    /**
     * Adds a listener of clicks on the ✕ of an entry's card.
     *
     * @param listener the listener
     * @return a registration that removes the listener
     */
    Registration addRejectEntryClickListener(ComponentEventListener<RejectEntryClickEvent> listener) {
        return addListener(RejectEntryClickEvent.class, listener);
    }

    private static List<TimeEntry> entriesIn(ApprovalPerson person, ApprovalPeriod period) {
        return person.entries().stream().filter(entry -> period.contains(entry.getDate())).toList();
    }

    private Div createHeader(ApprovalPerson person, List<String> roleNames, List<TimeEntry> entries,
                             ApprovalPeriod period) {
        String name = ApprovalRosterCard.nameOf(person.user());
        Avatar avatar = componentSupport.createAvatar(name, person.user().getAvatar(), "avatar");

        H3 nameText = new H3(name);
        nameText.addClassName("name");

        Div details = new Div();
        details.addClassName("details");
        roleNames.forEach(roleName -> details.add(componentSupport.createBadge(roleName, BadgeStyle.NEUTRAL)));
        Span projects = new Span(entries.stream()
                .map(entry -> metadataTools.getInstanceName(entry.getTask().getProject()))
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.joining(", ")));
        projects.addClassName("projects");
        details.add(projects);

        // Where the actions of the person go
        Div actions = new Div();
        actions.addClassName("actions");
        addPersonButton(actions, period);

        Div header = new Div(avatar, nameText, details, actions);
        header.addClassName("header");
        return header;
    }

    private Div createFigures(ApprovalTotals totals) {
        int count = totals.count();
        Div logged = createFigure("logged", null, componentSupport.formatWorkTimeTotal(totals.time()),
                count > 0 ? entryCountText(count) : null);
        ApprovalStatusBar bar = new ApprovalStatusBar();
        bar.setTotals(totals);
        logged.add(bar);

        Div figures = new Div(logged,
                createStatusFigure("waiting", TimeEntryStatus.NEW, totals),
                createStatusFigure("approved", TimeEntryStatus.APPROVED, totals),
                createStatusFigure("rejected", TimeEntryStatus.REJECTED, totals));
        figures.addClassName("figures");
        return figures;
    }

    private Div createStatusFigure(String name, TimeEntryStatus status, ApprovalTotals totals) {
        int count = totals.count(status);
        String details = count > 0
                ? messages.formatMessage(ApprovalPersonPanel.class, PluralForms.key("figure.details", count, locale),
                locale, componentSupport.formatWorkTimeTotal(totals.time(status)))
                : null;

        Div figure = createFigure(name, status, String.valueOf(count), details);
        figure.setClassName("empty", count == 0);
        return figure;
    }

    private Div createFigure(String name, @Nullable TimeEntryStatus status, String value, @Nullable String details) {
        Div label = new Div();
        label.addClassName("label");
        if (status != null) {
            Span dot = new Span();
            dot.addClassNames("dot", ApprovalStatusClassNames.of(status));
            label.add(dot);
        }
        label.add(new Text(messages.getMessage(ApprovalPersonPanel.class, "figure." + name + ".label", locale)));

        Div valueText = new Div(value);
        valueText.addClassName("value");

        Div figure = new Div(label, valueText);
        figure.setId("figure-" + name);
        figure.addClassName("figure");
        if (details != null) {
            Div detailsText = new Div(details);
            detailsText.addClassName("details");
            figure.add(detailsText);
        }
        return figure;
    }

    private Div createDayGroup(LocalDate day, List<TimeEntry> entries) {
        String date = day.format(DateTimeFormatter.ofPattern(
                messages.getMessage(ApprovalPersonPanel.class, "entriesDay.date", locale), locale));
        Span dateText = new Span(messages.formatMessage(ApprovalPersonPanel.class, "entriesDay.title", locale,
                componentSupport.formatDayOfWeekShort(day.getDayOfWeek()), date));
        dateText.addClassName("date");

        Span separator = new Span(messages.getMessage(ApprovalPersonPanel.class, "entriesDay.separator", locale));
        separator.addClassName("separator");

        Span hours = new Span(componentSupport.formatWorkTimeTotal(ApprovalTotals.of(entries).time()));
        hours.addClassName("hours");

        // Where the actions of the day go
        Div actions = new Div();
        actions.addClassName("actions");
        addDayButton(listedButtons, actions, day);

        // The spaces take no room in the header's layout, but keep the heading, such as "Mon, Oct 19 · 6h", in words
        // for assistive technology
        Div header = new Div(dateText, new Text(" "), separator, new Text(" "), hours, actions);
        header.addClassName("day-header");

        Div group = new Div(header);
        group.addClassName("entries-day");
        entries.forEach(entry -> group.add(createEntryCard(entry, false)));
        return group;
    }

    private ApprovalEntryCard createEntryCard(TimeEntry entry, boolean compact) {
        ApprovalEntryCard card = new ApprovalEntryCard(entry, compact, componentSupport, messages, metadataTools,
                locale);
        card.addApproveClickListener(event ->
                fireEvent(new ApproveEntryClickEvent(this, event.isFromClient(), entry)));
        card.addRejectClickListener(event ->
                fireEvent(new RejectEntryClickEvent(this, event.isFromClient(), entry)));
        return card;
    }

    private void addPersonButton(Div actions, ApprovalPeriod period) {
        String key = period.mode() == ApprovalMode.MONTH ? "approvePersonBtn.month" : "approvePersonBtn.week";
        Button button = createApproveButton("approvePersonBtn", ButtonVariant.PRIMARY, ButtonVariant.SUCCESS);
        drawnButtons.add(actions, button, new ApprovalScope(period.start(), period.end()),
                count -> countText(key, count));
    }

    /**
     * Adds the button of a week of the month grid, which approves the week's days of the month.
     */
    private void addWeekButton(Div actions, List<LocalDate> row, ApprovalPeriod month) {
        List<LocalDate> days = row.stream().filter(month::contains).toList();
        ApprovalScope scope = new ApprovalScope(days.getFirst(), days.getLast());

        Button button = createApproveButton("approveWeekBtn-" + row.getFirst(), ButtonVariant.PRIMARY,
                ButtonVariant.SUCCESS, ButtonVariant.SMALL);
        button.setAriaLabel(messages.formatMessage(ApprovalPersonPanel.class, "approveWeekBtn.ariaLabel", locale,
                titles.formatDates(scope.from(), scope.to())));
        String text = messages.getMessage(ApprovalPersonPanel.class, "approveWeekBtn.text", locale);
        drawnButtons.add(actions, button, scope, count -> text);
    }

    private void addDayButton(ApprovalBulkApproveButtons buttons, Div actions, LocalDate day) {
        Button button = createApproveButton("approveDayBtn-" + day, ButtonVariant.TERTIARY, ButtonVariant.SUCCESS,
                ButtonVariant.SMALL);
        button.setAriaLabel(messages.formatMessage(ApprovalPersonPanel.class, "approveDayBtn.ariaLabel", locale,
                titles.formatDates(day, day)));
        String text = messages.getMessage(ApprovalPersonPanel.class, "approveDayBtn.text", locale);
        buttons.add(actions, button, ApprovalScope.ofDay(day), count -> text);
    }

    private static Button createApproveButton(String id, ButtonVariant... variants) {
        Button button = new Button(GravityUiIcon.CHECK.create());
        button.setId(id);
        button.addThemeVariants(variants);
        return button;
    }

    private void fireApproveScopeClick(ApprovalScope scope, boolean fromClient) {
        fireEvent(new ApproveScopeClickEvent(this, fromClient, scope));
    }

    private String countText(String key, int count) {
        return messages.formatMessage(ApprovalPersonPanel.class, PluralForms.key(key, count, locale), locale, count);
    }

    private String entryCountText(int count) {
        return messages.formatMessage(ApprovalPersonPanel.class, PluralForms.key("entryCount", count, locale), locale,
                count);
    }

    /**
     * A click on a day of the month grid.
     */
    static final class DayClickEvent extends ComponentEvent<ApprovalPersonPanel> {

        private final LocalDate day;

        DayClickEvent(ApprovalPersonPanel source, boolean fromClient, LocalDate day) {
            super(source, fromClient);
            this.day = day;
        }

        /**
         * Returns the day clicked.
         *
         * @return a day of the month shown
         */
        LocalDate getDay() {
            return day;
        }
    }

    /**
     * A click on a week card of the month grid.
     */
    static final class WeekClickEvent extends ComponentEvent<ApprovalPersonPanel> {

        private final LocalDate weekStart;

        WeekClickEvent(ApprovalPersonPanel source, boolean fromClient, LocalDate weekStart) {
            super(source, fromClient);
            this.weekStart = weekStart;
        }

        /**
         * Returns the week clicked.
         *
         * @return the first day of a row of the month's grid
         */
        LocalDate getWeekStart() {
            return weekStart;
        }
    }

    /**
     * A click on a button that approves the waiting entries of a stretch of the period: the person's, a week's, or a
     * day's.
     */
    static final class ApproveScopeClickEvent extends ComponentEvent<ApprovalPersonPanel> {

        private final ApprovalScope scope;

        ApproveScopeClickEvent(ApprovalPersonPanel source, boolean fromClient, ApprovalScope scope) {
            super(source, fromClient);
            this.scope = scope;
        }

        /**
         * Returns the days the clicked button approves.
         *
         * @return the scope of the button
         */
        ApprovalScope getScope() {
            return scope;
        }
    }

    /**
     * A click on a button of an entry's card.
     */
    abstract static class EntryClickEvent extends ComponentEvent<ApprovalPersonPanel> {

        private final TimeEntry entry;

        EntryClickEvent(ApprovalPersonPanel source, boolean fromClient, TimeEntry entry) {
            super(source, fromClient);
            this.entry = entry;
        }

        /**
         * Returns the entry whose button was clicked.
         *
         * @return the entry, as the panel was given it
         */
        TimeEntry getEntry() {
            return entry;
        }
    }

    /**
     * A click on the ✓ of an entry's card.
     */
    static final class ApproveEntryClickEvent extends EntryClickEvent {

        ApproveEntryClickEvent(ApprovalPersonPanel source, boolean fromClient, TimeEntry entry) {
            super(source, fromClient, entry);
        }
    }

    /**
     * A click on the ✕ of an entry's card.
     */
    static final class RejectEntryClickEvent extends EntryClickEvent {

        RejectEntryClickEvent(ApprovalPersonPanel source, boolean fromClient, TimeEntry entry) {
            super(source, fromClient, entry);
        }
    }
}
