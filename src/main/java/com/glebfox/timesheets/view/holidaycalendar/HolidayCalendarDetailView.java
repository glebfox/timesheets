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

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.data.renderer.BadgeRender;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDayDetailView;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.core.Sort;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionChangeType;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.model.Sorter;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

@Route(value = "holiday-calendars/:id", layout = MainView.class)
@ViewController("ts_HolidayCalendar.detail")
@ViewDescriptor("holiday-calendar-detail-view.xml")
@EditedEntityContainer("holidayCalendarDc")
public class HolidayCalendarDetailView extends StandardDetailView<HolidayCalendar> {

    @ViewComponent
    private DataGrid<HolidayCalendarDay> daysDataGrid;
    @ViewComponent
    private CollectionPropertyContainer<HolidayCalendarDay> daysDc;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ComponentSupport componentSupport;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        daysDataGrid.sort(GridSortOrder.desc(daysDataGrid.getColumnByKey("date")).build());
    }

    @Subscribe(id = "daysDc", target = Target.DATA_CONTAINER)
    public void onDaysDcCollectionChange(final CollectionContainer.CollectionChangeEvent<HolidayCalendarDay> event) {
        // A created day is added at the end and an edited day stays in place, whatever its date,
        // while the grid sorts the days only when its sort order changes
        if (event.getChangeType() == CollectionChangeType.ADD_ITEMS
                || event.getChangeType() == CollectionChangeType.SET_ITEM) {
            sortDaysAsShown();
        }
    }

    private void sortDaysAsShown() {
        Sorter sorter = daysDc.getSorter();
        List<Sort.Order> orders = daysDataGrid.getSortOrder().stream()
                .map(sortOrder -> sortOrder.getDirection() == SortDirection.ASCENDING
                        ? Sort.Order.asc(sortOrder.getSorted().getKey())
                        : Sort.Order.desc(sortOrder.getSorted().getKey()))
                .toList();
        if (sorter != null && !orders.isEmpty()) {
            sorter.sort(Sort.by(orders));
        }
    }

    @Install(to = "daysDataGrid.create", subject = "viewConfigurer")
    private void daysDataGridCreateViewConfigurer(final HolidayCalendarDayDetailView view) {
        view.setCalendarDays(daysDc::getItems);
    }

    @Install(to = "daysDataGrid.edit", subject = "viewConfigurer")
    private void daysDataGridEditViewConfigurer(final HolidayCalendarDayDetailView view) {
        view.setCalendarDays(daysDc::getItems);
    }

    @Supply(to = "daysDataGrid.date", subject = "renderer")
    private Renderer<HolidayCalendarDay> daysDataGridDateRenderer() {
        // The weekday catches a wrong date: a workday on a Friday or a day off on a Sunday reads as wrong at once
        return new TextRenderer<>(day -> formatDate(day.getDate()));
    }

    private String formatDate(LocalDate date) {
        return messageBundle.formatMessage("dateColumn.format",
                componentSupport.formatDayOfWeekShort(date.getDayOfWeek()),
                datatypeFormatter.formatLocalDate(date));
    }

    @Supply(to = "daysDataGrid.type", subject = "renderer")
    private Renderer<HolidayCalendarDay> daysDataGridTypeRenderer() {
        return new BadgeRender<>(
                day -> metadataTools.format(day.getType()),
                day -> switch (day.getType()) {
                    case DAY_OFF -> BadgeStyle.NEUTRAL;
                    case WORKDAY -> BadgeStyle.DEFAULT;
                }
        );
    }
}
