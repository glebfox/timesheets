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

package com.glebfox.timesheets.view.workschedule;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.flowui.model.CollectionChangeType;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.util.Set;

@Route(value = "work-schedules", layout = MainView.class)
@ViewController("ts_WorkSchedule.list")
@ViewDescriptor("work-schedule-list-view.xml")
@LookupComponent("workSchedulesDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class WorkScheduleListView extends StandardListView<WorkSchedule> {

    @ViewComponent
    private CollectionContainer<WorkSchedule> workSchedulesDc;
    @ViewComponent
    private CollectionLoader<WorkSchedule> workSchedulesDl;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private ComponentSupport componentSupport;

    @Subscribe(id = "workSchedulesDc", target = Target.DATA_CONTAINER)
    public void onWorkSchedulesDcCollectionChange(final CollectionContainer.CollectionChangeEvent<WorkSchedule> event) {
        // The editor opens by navigation, so the list learns of a save only as the one row added or replaced,
        // while saving a schedule as the default also unmarked the previous default in the database
        if (event.getChangeType() != CollectionChangeType.REFRESH && countDefaults() > 1) {
            workSchedulesDl.load();
        }
    }

    private long countDefaults() {
        return workSchedulesDc.getItems().stream()
                .filter(schedule -> Boolean.TRUE.equals(schedule.getIsDefault()))
                .count();
    }

    @Supply(to = "workSchedulesDataGrid.name", subject = "renderer")
    private Renderer<WorkSchedule> workSchedulesDataGridNameRenderer() {
        return new ComponentRenderer<>(schedule -> {
            Span cell = new Span(schedule.getName());
            cell.addClassName("text-with-badges");

            if (Boolean.TRUE.equals(schedule.getIsDefault())) {
                cell.add(componentSupport.createBadge(
                        messageBundle.getMessage("defaultBadge.text"), BadgeStyle.DEFAULT));
            }
            return cell;
        });
    }

    @Supply(to = "workSchedulesDataGrid.workdays", subject = "renderer")
    private Renderer<WorkSchedule> workSchedulesDataGridWorkdaysRenderer() {
        return new ComponentRenderer<>(schedule -> {
            Set<DayOfWeek> workdays = WorkScheduleDetailView.getWorkdays(schedule);

            Span cell = new Span();
            cell.addClassName("workday-chips");
            for (DayOfWeek day : componentSupport.getDaysOfWeek()) {
                String label = componentSupport.formatDayOfWeekShort(day);
                if (workdays.contains(day)) {
                    cell.add(componentSupport.createBadge(label, BadgeStyle.DEFAULT));
                } else {
                    Badge dayOff = componentSupport.createBadge(label, BadgeStyle.NEUTRAL);
                    dayOff.addClassName("day-off");
                    cell.add(dayOff);
                }
            }
            return cell;
        });
    }
}
