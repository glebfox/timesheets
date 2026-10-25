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
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.DataManager;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.model.CollectionChangeType;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Route(value = "holiday-calendars", layout = MainView.class)
@ViewController("ts_HolidayCalendar.list")
@ViewDescriptor("holiday-calendar-list-view.xml")
@LookupComponent("holidayCalendarsDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class HolidayCalendarListView extends StandardListView<HolidayCalendar> {

    @Autowired
    private DataManager dataManager;
    @Autowired
    private DatatypeFormatter datatypeFormatter;

    /**
     * The latest date among the non-deleted days of each listed calendar that has any, by calendar id.
     */
    private final Map<UUID, LocalDate> filledUntil = new HashMap<>();

    @Subscribe(id = "holidayCalendarsDl", target = Target.DATA_LOADER)
    public void onHolidayCalendarsDlPostLoad(final CollectionLoader.PostLoadEvent<HolidayCalendar> event) {
        filledUntil.clear();
        loadFilledUntil(event.getLoadedEntities());
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
