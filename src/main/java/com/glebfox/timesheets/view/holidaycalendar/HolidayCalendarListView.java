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

package com.glebfox.timesheets.view.holidaycalendar;

import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.LoadContext;
import io.jmix.core.Sort;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionChangeType;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Route(value = "holiday-calendars", layout = MainView.class)
@ViewController("ts_HolidayCalendar.list")
@ViewDescriptor("holiday-calendar-list-view.xml")
@LookupComponent("holidayCalendarsDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class HolidayCalendarListView extends StandardListView<HolidayCalendar> {

    private static final String FILLED_UNTIL = "filledUntil";

    @ViewComponent
    private DataGrid<HolidayCalendar> holidayCalendarsDataGrid;

    @Autowired
    private DataManager dataManager;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    /**
     * The latest date among the non-deleted days of each listed calendar that has any, by calendar id.
     */
    private final Map<UUID, LocalDate> filledUntil = new HashMap<>();

    /**
     * Orders calendars by Filled until, a calendar with no days as the earliest.
     */
    private final Comparator<HolidayCalendar> byFilledUntil = Comparator.comparing(
            calendar -> filledUntil.get(calendar.getId()), Comparator.nullsFirst(Comparator.naturalOrder()));

    @Subscribe
    public void onInit(final InitEvent event) {
        // Filled until is not an attribute, so the database cannot sort by it. The grid sorts the page in memory
        // with this comparator while the page holds every calendar; otherwise it reloads the page, and the load
        // delegate sorts it
        Objects.requireNonNull(holidayCalendarsDataGrid.getColumnByKey(FILLED_UNTIL)).setComparator(byFilledUntil);
    }

    @Install(to = "holidayCalendarsDl", target = Target.DATA_LOADER)
    private List<HolidayCalendar> holidayCalendarsDlLoadDelegate(final LoadContext<HolidayCalendar> loadContext) {
        // The database rejects a sort by Filled until, which the grid leaves on the loader after an in-memory sort
        LoadContext.Query query = Objects.requireNonNull(loadContext.getQuery());
        if (query.getSort() != null) {
            query.setSort(Sort.by(query.getSort().getOrders().stream()
                    .filter(order -> !FILLED_UNTIL.equals(order.getProperty()))
                    .toList()));
        }

        List<HolidayCalendar> calendars = new ArrayList<>(dataManager.loadList(loadContext));
        filledUntil.clear();
        loadFilledUntil(calendars);

        // Only the header keeps a sort by Filled until: the grid drops it when it reloads a full page to sort it
        holidayCalendarsDataGrid.getSortOrder().stream()
                .filter(order -> FILLED_UNTIL.equals(order.getSorted().getKey()))
                .findFirst()
                .ifPresent(order -> calendars.sort(order.getDirection() == SortDirection.ASCENDING
                        ? byFilledUntil
                        : byFilledUntil.reversed()));
        return calendars;
    }

    @Subscribe(id = "holidayCalendarsDc", target = Target.DATA_CONTAINER)
    public void onHolidayCalendarsDcCollectionChange(
            final CollectionContainer.CollectionChangeEvent<HolidayCalendar> event) {
        // The editor opens by navigation, so the list learns of a save only as the one row added or replaced
        if (event.getChangeType() == CollectionChangeType.ADD_ITEMS
                || event.getChangeType() == CollectionChangeType.SET_ITEM) {
            loadFilledUntil(event.getChanges());
        }
    }

    // One query for all the given calendars, apart from the list's own query, so a calendar with no days stays listed
    private void loadFilledUntil(Collection<? extends HolidayCalendar> calendars) {
        List<UUID> calendarIds = calendars.stream()
                .map(HolidayCalendar::getId)
                .toList();
        if (calendarIds.isEmpty()) {
            return;
        }

        calendarIds.forEach(filledUntil::remove);
        dataManager.loadValues("select d.calendar.id, max(d.date) from ts_HolidayCalendarDay d"
                        + " where d.calendar.id in :calendarIds"
                        + " group by d.calendar.id")
                .properties("calendarId", "lastDate")
                .parameter("calendarIds", calendarIds)
                .list()
                .forEach(row -> filledUntil.put(row.getValue("calendarId"), row.getValue("lastDate")));
    }

    @Supply(to = "holidayCalendarsDataGrid.filledUntil", subject = "renderer")
    private Renderer<HolidayCalendar> holidayCalendarsDataGridFilledUntilRenderer() {
        return new TextRenderer<>(calendar -> {
            LocalDate lastDate = filledUntil.get(calendar.getId());
            return lastDate == null ? "" : datatypeFormatter.formatLocalDate(lastDate);
        });
    }
}
