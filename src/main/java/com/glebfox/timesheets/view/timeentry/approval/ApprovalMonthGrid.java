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

import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ApprovalTotals;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.function.SerializableBiConsumer;
import io.jmix.core.Messages;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * A person's month on the approval screen, in the Calendar's layout: one row per week, and beside each row a card of
 * the week.
 * <p>
 * A day shows its number and, with entries, their time with a dot and a line in the color of the day's status;
 * Saturday and Sunday are shaded. The days of the neighbouring months are dimmed and show nothing, and the week
 * cards count only the month's days: hours, entries, and the status badges.
 * <p>
 * A click on a day of the month or on a week card is passed on with the day or the first day of the week, and
 * whether it came from the browser; the grid does not change the selected week by itself, see
 * {@link #setSelectedWeek}.
 * <p>
 * The grid has the class {@code approval-month-grid}. Its parts are {@code heading} cells, {@code day} cells with the
 * id {@code day-<date>} and the classes {@code weekend}, {@code outside} for a neighbouring day, and
 * {@code selected}, with the parts {@code number}, {@code hours} (holding a {@code dot}), and {@code line}; and
 * {@code week} cards with the id {@code week-<first day>} and the class {@code selected}, with the parts
 * {@code hours}, {@code count}, {@code badges}, and {@code actions}, where the actions of a week go.
 */
final class ApprovalMonthGrid extends Composite<Div> implements HasComponents {

    static final String CLASS_NAME = "approval-month-grid";
    static final String SELECTED_CLASS_NAME = "selected";

    private final ApprovalPeriod period;
    private final ComponentSupport componentSupport;
    private final Messages messages;
    private final Locale locale;
    private final ApprovalPeriodTitle titles;
    // The cells of each row and its week card, by the first day of the row
    private final Map<LocalDate, List<Div>> rowCells = new LinkedHashMap<>();
    private final Map<LocalDate, Div> weekCards = new LinkedHashMap<>();

    /**
     * Creates the grid of a month.
     *
     * @param period           the month
     * @param daysOfWeek       the days of the week in the order of the columns, from the first day of a row
     * @param entriesByDay     the entries of each day of the month; the days of other months are ignored
     * @param componentSupport builds the badges, and formats days and times
     * @param messages         gives the texts
     * @param locale           the locale of the texts
     * @param titles           formats the dates of a week
     * @param onDayClick       called with a clicked day of the month, and whether the click came from the browser
     * @param onWeekClick      called with the first day of a clicked week, and whether the click came from the browser
     */
    ApprovalMonthGrid(ApprovalPeriod period, List<DayOfWeek> daysOfWeek, Map<LocalDate, List<TimeEntry>> entriesByDay,
                      ComponentSupport componentSupport, Messages messages, Locale locale,
                      ApprovalPeriodTitle titles,
                      SerializableBiConsumer<LocalDate, Boolean> onDayClick,
                      SerializableBiConsumer<LocalDate, Boolean> onWeekClick) {
        this.period = period;
        this.componentSupport = componentSupport;
        this.messages = messages;
        this.locale = locale;
        this.titles = titles;

        Div grid = getContent();
        grid.addClassName(CLASS_NAME);

        for (DayOfWeek day : daysOfWeek) {
            grid.add(createHeading(componentSupport.formatDayOfWeekShort(day)));
        }
        grid.add(createHeading(messages.getMessage(ApprovalMonthGrid.class, "monthGrid.week", locale)));

        for (List<LocalDate> row : period.weekRows(daysOfWeek.getFirst())) {
            List<Div> cells = new ArrayList<>();
            List<TimeEntry> weekEntries = new ArrayList<>();
            for (LocalDate day : row) {
                List<TimeEntry> dayEntries = period.contains(day)
                        ? entriesByDay.getOrDefault(day, List.of())
                        : List.of();
                weekEntries.addAll(dayEntries);

                Div cell = createDay(day, dayEntries);
                if (period.contains(day)) {
                    cell.addClickListener(event -> onDayClick.accept(day, event.isFromClient()));
                }
                cells.add(cell);
            }

            LocalDate weekStart = row.getFirst();
            Div weekCard = createWeekCard(row, ApprovalTotals.of(weekEntries));
            weekCard.addClickListener(event -> onWeekClick.accept(weekStart, event.isFromClient()));

            rowCells.put(weekStart, cells);
            weekCards.put(weekStart, weekCard);
            cells.forEach(grid::add);
            grid.add(weekCard);
        }
    }

    /**
     * Marks the week whose entries are listed: its days and its card.
     *
     * @param weekStart the first day of a row of the grid
     * @throws IllegalArgumentException if no row of the grid starts on the day
     */
    void setSelectedWeek(LocalDate weekStart) {
        Preconditions.checkArgument(weekCards.containsKey(weekStart), "No week of %s starts on %s", period, weekStart);
        weekCards.forEach((start, card) -> {
            boolean selected = start.equals(weekStart);
            card.setClassName(SELECTED_CLASS_NAME, selected);
            card.getElement().setAttribute("aria-pressed", String.valueOf(selected));
            rowCells.get(start).forEach(cell -> cell.setClassName(SELECTED_CLASS_NAME, selected));
        });
    }

    private static Div createHeading(String text) {
        Div heading = new Div(text);
        heading.addClassName("heading");
        return heading;
    }

    private Div createDay(LocalDate day, List<TimeEntry> entries) {
        Span number = new Span(String.valueOf(day.getDayOfMonth()));
        number.addClassName("number");

        Div cell = new Div(number);
        cell.setId("day-" + day);
        cell.addClassName("day");
        cell.setClassName("weekend", day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY);
        cell.setClassName("outside", !period.contains(day));

        Optional<TimeEntryStatus> status = ApprovalTotals.dayStatus(entries);
        if (status.isPresent()) {
            String statusClassName = ApprovalStatusClassNames.of(status.get());

            Span dot = new Span();
            dot.addClassNames("dot", statusClassName);
            Div hours = new Div(dot,
                    new Span(componentSupport.formatWorkTimeTotal(ApprovalTotals.of(entries).time())));
            hours.addClassName("hours");

            Div line = new Div();
            line.addClassNames("line", statusClassName);

            cell.add(hours, line);
        }

        return cell;
    }

    private Div createWeekCard(List<LocalDate> row, ApprovalTotals totals) {
        Div hours = new Div(componentSupport.formatWorkTimeTotal(totals.time()));
        hours.addClassName("hours");

        Span count = new Span(entryCountText(totals.count()));
        count.addClassName("count");

        Div badges = new Div();
        badges.addClassName("badges");
        List<Badge> statusBadges = ApprovalStatusBadges.of(totals, componentSupport, messages, locale);
        statusBadges.forEach(badges::add);

        // Where the actions of the week go
        Div actions = new Div();
        actions.addClassName("actions");

        Div card = new Div(hours, count, badges, actions);
        card.setId("week-" + row.getFirst());
        card.addClassName("week");

        // Chosen as a button is: by a click, or by Enter or Space while it has the focus, which click it here.
        // Without preventDefault, Space would also scroll the page
        card.getElement().setAttribute("role", "button");
        card.getElement().setAttribute("tabindex", "0");
        card.getElement()
                .addEventListener("keydown", event -> ComponentUtil.fireEvent(card, new ClickEvent<>(card)))
                .setFilter("event.key === 'Enter' || event.key === ' '")
                .preventDefault();

        // The card does not say which week it is, so its name tells it with what it shows
        List<LocalDate> monthDays = row.stream().filter(period::contains).toList();
        List<String> summary = new ArrayList<>(List.of(hours.getText(), count.getText()));
        statusBadges.stream().map(ApprovalStatusBadges::spokenTextOf).forEach(summary::add);
        card.getElement().setAttribute("aria-label", messages.formatMessage(ApprovalMonthGrid.class,
                "weekCard.ariaLabel", locale,
                titles.formatDates(monthDays.getFirst(), monthDays.getLast()), String.join(", ", summary)));

        return card;
    }

    private String entryCountText(int count) {
        return messages.formatMessage(ApprovalMonthGrid.class, PluralForms.key("entryCount", count, locale), locale,
                count);
    }
}
