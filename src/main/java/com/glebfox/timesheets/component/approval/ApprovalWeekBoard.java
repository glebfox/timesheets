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

package com.glebfox.timesheets.component.approval;

import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ApprovalTotals;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.Messages;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * A person's week on the approval screen: seven columns, one per day in the order of the user's week, each headed by
 * the day and its time, with the day's entries as compact cards below. Saturday and Sunday are shaded.
 * <p>
 * The board has the class {@code approval-week-board}. Its {@code column}s have the id {@code column-<date>} and the
 * class {@code weekend}; each holds a {@code header}, with the parts {@code day}, {@code hours}, {@code line} in the
 * class of the day's status when it has entries, and {@code actions}, where the actions of a day go, and the
 * {@code entries} of the day.
 */
final class ApprovalWeekBoard extends Composite<Div> implements HasComponents {

    static final String CLASS_NAME = "approval-week-board";

    // The part of each column's header where the actions of the day go, by the day
    private final Map<LocalDate, Div> dayActions = new LinkedHashMap<>();

    /**
     * Creates the board of a week.
     *
     * @param week             the week
     * @param daysOfWeek       the days of the week in the order of the columns
     * @param entriesByDay     the entries of each day
     * @param cardFactory      creates the card of an entry
     * @param componentSupport formats days and times
     * @param messages         gives the texts
     * @param locale           the locale of the texts
     */
    ApprovalWeekBoard(ApprovalPeriod week, List<DayOfWeek> daysOfWeek, Map<LocalDate, List<TimeEntry>> entriesByDay,
                      Function<TimeEntry, ApprovalEntryCard> cardFactory, ComponentSupport componentSupport,
                      Messages messages, Locale locale) {
        Preconditions.checkArgument(week.mode() == ApprovalMode.WEEK, "Not a week: %s", week);

        Div board = getContent();
        board.addClassName(CLASS_NAME);

        for (DayOfWeek dayOfWeek : daysOfWeek) {
            LocalDate day = week.days().stream()
                    .filter(date -> date.getDayOfWeek() == dayOfWeek)
                    .findFirst()
                    .orElseThrow();
            List<TimeEntry> entries = entriesByDay.getOrDefault(day, List.of());
            board.add(createColumn(day, entries, cardFactory, componentSupport, messages, locale));
        }
    }

    /**
     * Creates the column of a day: its header over the cards of its entries.
     */
    private Div createColumn(LocalDate day, List<TimeEntry> entries, Function<TimeEntry, ApprovalEntryCard> cardFactory,
                             ComponentSupport componentSupport, Messages messages, Locale locale) {
        Div cards = new Div();
        cards.addClassName("entries");
        entries.forEach(entry -> cards.add(cardFactory.apply(entry)));

        Div column = new Div(createHeader(day, entries, componentSupport, messages, locale), cards);
        column.setId("column-" + day);
        column.addClassName("column");
        DayOfWeek dayOfWeek = day.getDayOfWeek();
        column.setClassName("weekend", dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY);
        return column;
    }

    /**
     * Creates the header of a day's column: the day, its time and the line of its status when it has entries, and the
     * part where its actions go.
     */
    private Div createHeader(LocalDate day, List<TimeEntry> entries, ComponentSupport componentSupport,
                             Messages messages, Locale locale) {
        Span dayName = new Span(messages.formatMessage(ApprovalWeekBoard.class, "weekBoard.day", locale,
                componentSupport.formatDayOfWeekShort(day.getDayOfWeek()), day.getDayOfMonth()));
        dayName.addClassName("day");

        Div header = new Div(dayName);
        header.addClassName("header");
        // A day with entries has a status
        ApprovalTotals.dayStatus(entries).ifPresent(status -> {
            Span hours = new Span(componentSupport.formatWorkTimeTotal(ApprovalTotals.of(entries).time()));
            hours.addClassName("hours");

            Div line = new Div();
            line.addClassNames("line", ApprovalStatusClassNames.of(status));
            header.add(hours, line);
        });
        // Where the actions of the day go
        Div actions = new Div();
        actions.addClassName("actions");
        header.add(actions);
        dayActions.put(day, actions);
        return header;
    }

    /**
     * Returns the part of a column's header where the actions of the day go.
     *
     * @param day a day of the week shown
     * @return the {@code actions} part, empty until something is put in it
     * @throws IllegalArgumentException if the day is not in the week shown
     */
    Div getDayActions(LocalDate day) {
        Preconditions.checkArgument(dayActions.containsKey(day), "%s is not a day of the week shown", day);
        return dayActions.get(day);
    }
}
