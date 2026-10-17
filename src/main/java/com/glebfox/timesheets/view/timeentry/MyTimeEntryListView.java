/*
 * Copyright 2023 Gleb Gorelov
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

package com.glebfox.timesheets.view.timeentry;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.builder.DetailViewBuilder;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "my-time-entries", layout = MainView.class)
@ViewController("ts_TimeEntry.my")
@ViewDescriptor("my-time-entry-list-view.xml")
@DialogMode(width = "1024px", closeOnEsc = true)
public class MyTimeEntryListView extends StandardView {

    @ViewComponent
    private DataGrid<TimeEntry> timeEntriesDataGrid;

    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private ViewBuilders viewBuilders;

    @Supply(to = "timeEntriesDataGrid.status", subject = "renderer")
    private Renderer<TimeEntry> timeEntriesDataGridStatusRenderer() {
        return componentSupport.createTimeEntryStatusRender();
    }

    @Supply(to = "timeEntriesDataGrid.tags", subject = "renderer")
    private Renderer<TimeEntry> timeEntriesDataGridTagsRenderer() {
        return new ComponentRenderer<>(timeEntry -> {
            List<Tag> tags = timeEntry.getTags();
            if (tags.isEmpty()) {
                return null;
            }

            FlexLayout wrapper = new FlexLayout();
            wrapper.setWidthFull();
            wrapper.setFlexWrap(FlexLayout.FlexWrap.WRAP);
            wrapper.addClassNames("gap-s");

            for (Tag tag : tags) {
                Badge tagBadge = new Badge(metadataTools.format(tag));
                tagBadge.addThemeVariants(BadgeVariant.SMALL);
                wrapper.add(tagBadge);
            }

            return wrapper;
        });
    }

    @Supply(to = "timeEntriesDataGrid.task", subject = "renderer")
    private Renderer<TimeEntry> timeEntriesDataGridTaskRenderer() {
        return componentSupport.createTaskWithProjectRenderer(TimeEntry::getTask);
    }

    @Subscribe("timeEntriesDataGrid.create")
    public void onTimeEntriesDataGridCreate(final ActionPerformedEvent event) {
        timeEntryDetailBuilder()
                .newEntity()
                .open();
    }

    @Subscribe("timeEntriesDataGrid.edit")
    public void onTimeEntriesDataGridEdit(final ActionPerformedEvent event) {
        timeEntryDetailBuilder()
                .open();
    }

    private DetailViewBuilder<TimeEntry, TimeEntryDetailView> timeEntryDetailBuilder() {
        return viewBuilders.detail(timeEntriesDataGrid, TimeEntryDetailView.class)
                .withViewConfigurer(timeEntryDetailView ->
                        timeEntryDetailView.setOwnTimeEntry(true));
    }
}
