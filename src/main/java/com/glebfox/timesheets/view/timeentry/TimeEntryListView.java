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
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.view.*;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Route(value = "time-entries", layout = MainView.class)
@ViewController("ts_TimeEntry.list")
@ViewDescriptor("time-entry-list-view.xml")
@LookupComponent("timeEntriesDataGrid")
@DialogMode(width = "1024px", closeOnEsc = true)
public class TimeEntryListView extends StandardListView<TimeEntry> {

    @ViewComponent
    private DataGrid<TimeEntry> timeEntriesDataGrid;

    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ComponentSupport componentSupport;

    @Subscribe
    public void onInit(final InitEvent event) {
        // The rejection reason is Markdown; the descriptor has no attribute for this
        timeEntriesDataGrid.setTooltipMarkdownEnabled(true);
    }

    @Supply(to = "timeEntriesDataGrid.status", subject = "renderer")
    private Renderer<TimeEntry> timeEntriesDataGridStatusRenderer() {
        return componentSupport.createTimeEntryStatusRender(true);
    }

    @Install(to = "timeEntriesDataGrid", subject = "partNameGenerator")
    private @Nullable String timeEntriesDataGridPartNameGenerator(final TimeEntry timeEntry) {
        return timeEntry.getStatus() == TimeEntryStatus.REJECTED ? "rejected-entry" : null;
    }

    @Install(to = "timeEntriesDataGrid.status", subject = "tooltipGenerator")
    private @Nullable String timeEntriesDataGridStatusTooltipGenerator(final TimeEntry timeEntry) {
        return timeEntry.getStatus() == TimeEntryStatus.REJECTED && StringUtils.isNotBlank(timeEntry.getRejectionReason())
                ? timeEntry.getRejectionReason()
                : null;
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

    @Supply(to = "timeEntriesDataGrid.user", subject = "renderer")
    private Renderer<TimeEntry> timeEntriesDataGridUserRenderer() {
        return componentSupport.createUserRenderer(TimeEntry::getUser);
    }

    @Supply(to = "timeEntriesDataGrid.task", subject = "renderer")
    private Renderer<TimeEntry> timeEntriesDataGridTaskRenderer() {
        return componentSupport.createTaskWithProjectRenderer(TimeEntry::getTask);
    }
}