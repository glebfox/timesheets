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

import com.glebfox.timesheets.data.renderer.BadgeRender;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDayDetailView;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionChangeType;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionPropertyContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "holiday-calendars/:id", layout = MainView.class)
@ViewController("ts_HolidayCalendar.detail")
@ViewDescriptor("holiday-calendar-detail-view.xml")
@EditedEntityContainer("holidayCalendarDc")
public class HolidayCalendarDetailView extends StandardDetailView<HolidayCalendar> {

    @ViewComponent
    private DataGrid<HolidayCalendarDay> daysDataGrid;
    @ViewComponent
    private CollectionPropertyContainer<HolidayCalendarDay> daysDc;

    @Autowired
    private MetadataTools metadataTools;

    @Subscribe(id = "daysDc", target = Target.DATA_CONTAINER)
    public void onDaysDcCollectionChange(final CollectionContainer.CollectionChangeEvent<HolidayCalendarDay> event) {
        // The browser sizes the auto-width columns to the rows it shows first, while an added day or an edited one
        // can have a wider date or the longer type
        if (event.getChangeType() == CollectionChangeType.ADD_ITEMS
                || event.getChangeType() == CollectionChangeType.SET_ITEM) {
            daysDataGrid.recalculateColumnWidths();
        }
    }

    @Install(to = "daysDataGrid.edit", subject = "viewConfigurer")
    private void daysDataGridEditViewConfigurer(final HolidayCalendarDayDetailView view) {
        view.setCalendarDays(daysDc.getItems());
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
