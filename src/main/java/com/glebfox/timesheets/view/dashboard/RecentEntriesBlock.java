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

package com.glebfox.timesheets.view.dashboard;

import com.glebfox.timesheets.app.DashboardSupport;
import com.glebfox.timesheets.app.TimeEntrySupport;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.data.renderer.TextRenderer;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.Install;
import io.jmix.flowui.view.Supply;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Objects;

/**
 * The Recent entries block of the dashboard: the current user's {@link #LIMIT} latest entries, in a grid without a
 * header, a row each, with its date, its task and then its project, time spent, status, and Copy to today. A click on
 * the task opens the entry in the time entry editor, in a dialog, if the user may open the editor; otherwise the task
 * is plain text. Copy to today, an icon button with its name as the tooltip, opens the editor on a new entry copied
 * from it and dated today, if the user may create time entries; otherwise its column is hidden. After a save in
 * either, the dashboard reloads. No entries shows the grid's empty state.
 */
@FragmentDescriptor("recent-entries-block.xml")
public class RecentEntriesBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * How many entries the block shows.
     */
    static final int LIMIT = 5;

    /**
     * The part name of the task column's cells, by which the stylesheet caps the column's width.
     */
    static final String TASK_CELL_PART = "task-cell";

    /**
     * The part name of the date and the time columns' cells, by which the stylesheet sets their text apart.
     */
    static final String NUMBER_CELL_PART = "number-cell";

    @ViewComponent
    private DataGrid<TimeEntry> entriesGrid;
    @ViewComponent
    private CollectionContainer<TimeEntry> entriesDc;

    @Autowired
    private DashboardSupport dashboardSupport;
    @Autowired
    private TimeEntrySupport timeEntrySupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private TimeEntryEditorOpener timeEntryEditorOpener;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Messages messages;
    @Autowired
    private DashboardDates dashboardDates;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private boolean openPermitted;

    @Override
    public void refresh() {
        List<TimeEntry> entries = dashboardSupport.loadRecentEntries(LIMIT);
        openPermitted = timeEntryEditorOpener.isOpenPermitted();
        Objects.requireNonNull(entriesGrid.getColumnByKey("copy"))
                .setVisible(timeEntryEditorOpener.isCreatePermitted());

        entriesDc.setItems(entries);
        // The browser sizes the auto-width columns to the rows it shows first, while a date of another year, which
        // shows its year, or a wider status may come only with a later refresh
        entriesGrid.recalculateColumnWidths();
    }

    @Supply(to = "entriesGrid.date", subject = "renderer")
    private Renderer<TimeEntry> entriesGridDateRenderer() {
        return new TextRenderer<>(entry -> dashboardDates.formatDate(entry.getDate()));
    }

    @Install(to = "entriesGrid.date", subject = "partNameGenerator")
    private String entriesGridDatePartNameGenerator(final TimeEntry entry) {
        return NUMBER_CELL_PART;
    }

    @Supply(to = "entriesGrid.task", subject = "renderer")
    private Renderer<TimeEntry> entriesGridTaskRenderer() {
        return new ComponentRenderer<>(this::createTaskCell);
    }

    @Install(to = "entriesGrid.task", subject = "partNameGenerator")
    private String entriesGridTaskPartNameGenerator(final TimeEntry entry) {
        return TASK_CELL_PART;
    }

    @Supply(to = "entriesGrid.spent", subject = "renderer")
    private Renderer<TimeEntry> entriesGridSpentRenderer() {
        return new TextRenderer<>(entry -> componentSupport.formatWorkTimeTotal(
                entry.getSpentTime() != null ? entry.getSpentTime() : WorkTime.ZERO));
    }

    @Install(to = "entriesGrid.spent", subject = "partNameGenerator")
    private String entriesGridSpentPartNameGenerator(final TimeEntry entry) {
        return NUMBER_CELL_PART;
    }

    @Supply(to = "entriesGrid.status", subject = "renderer")
    private Renderer<TimeEntry> entriesGridStatusRenderer() {
        return componentSupport.createTimeEntryStatusRender();
    }

    @Supply(to = "entriesGrid.copy", subject = "renderer")
    private Renderer<TimeEntry> entriesGridCopyRenderer() {
        return new ComponentRenderer<>(this::createCopyButton);
    }

    /**
     * Creates the task cell: the task, which opens the entry if the user may open the editor, then its project,
     * quieter. On a wide card both stand on one line, the project after a separator; on a narrow one the project
     * goes on a line of its own under the task. A name too long is cut from its end, the project first, so the task
     * stays.
     */
    private Component createTaskCell(TimeEntry entry) {
        Div cell = uiComponents.create(Div.class);
        cell.addClassName("recent-task");

        String taskName = entry.getTask().getName();
        if (openPermitted) {
            JmixButton task = uiComponents.create(JmixButton.class);
            task.addClassNames("link-button", "task");
            task.setText(taskName);
            task.addClickListener(event -> openEditor(entry));
            cell.add(task);
        } else {
            cell.add(DashboardRows.part(uiComponents, "task", taskName));
        }

        Span separator = DashboardRows.part(uiComponents, "separator", "·");
        separator.getElement().setAttribute("aria-hidden", "true");
        cell.add(separator, DashboardRows.part(uiComponents, "project",
                metadataTools.format(entry.getTask().getProject())));
        return cell;
    }

    /**
     * Creates Copy to today: an icon, as the action repeats on every row; its name is the tooltip and what a screen
     * reader says.
     */
    private Component createCopyButton(TimeEntry entry) {
        String copyText = messages.getMessage(getClass(), "recentEntriesBlock.copyToToday");
        JmixButton copyButton = uiComponents.create(JmixButton.class);
        copyButton.addThemeVariants(ButtonVariant.TERTIARY);
        copyButton.setIcon(GravityUiIcon.COPY.create());
        copyButton.setAriaLabel(copyText);
        copyButton.setTooltipText(copyText);
        copyButton.addClassName("row-copy");
        copyButton.addClickListener(event -> copyToToday(entry));
        return copyButton;
    }

    private void openEditor(TimeEntry entry) {
        timeEntryEditorOpener.open(this, builder -> builder.editEntity(entry), this::refreshDashboard);
    }

    private void copyToToday(TimeEntry entry) {
        TimeEntry copy = timeEntrySupport.copy(entry);
        copy.setDate(DashboardBlock.today(timeSource, currentAuthentication));
        timeEntryEditorOpener.open(this, builder -> builder.newEntity(copy), this::refreshDashboard);
    }
}
