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
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.glebfox.timesheets.component.icon.GravityUiIcon;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Element;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * The Recent entries block of the dashboard: the current user's {@link #LIMIT} latest entries, each on one line with
 * its date, task and then project, time spent, and status. A click on a row opens the entry in the time entry
 * editor, in a dialog, if the user may open the editor; Copy to today, an icon button with its name as the tooltip,
 * opens the editor on a new entry copied from it and dated today, if the user may create time entries. After a save
 * in either, the dashboard reloads. No entries shows an empty state.
 */
@FragmentDescriptor("recent-entries-block.xml")
public class RecentEntriesBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * How many entries the block shows.
     */
    static final int LIMIT = 5;

    /**
     * The class of the rows' container when a date of the rows shows its year, from another year than the current
     * one, so the date column of every row is as wide as such a date and the rows stay aligned.
     */
    static final String WITH_YEARS = "with-years";

    @ViewComponent
    private Div rows;
    @ViewComponent
    private GridEmptyState emptyState;

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

    @Override
    public void refresh() {
        List<TimeEntry> entries = dashboardSupport.loadRecentEntries(LIMIT);
        boolean openPermitted = timeEntryEditorOpener.isOpenPermitted();
        boolean copyPermitted = timeEntryEditorOpener.isCreatePermitted();

        rows.removeAll();
        for (TimeEntry entry : entries) {
            rows.add(createRow(entry, openPermitted, copyPermitted));
        }
        int currentYear = DashboardBlock.today(timeSource, currentAuthentication).getYear();
        rows.setClassName(WITH_YEARS, entries.stream().anyMatch(entry -> entry.getDate().getYear() != currentYear));

        rows.setVisible(!entries.isEmpty());
        emptyState.setVisible(entries.isEmpty());
    }

    private Component createRow(TimeEntry entry, boolean openPermitted, boolean copyPermitted) {
        WorkTime spent = entry.getSpentTime() != null ? entry.getSpentTime() : WorkTime.ZERO;
        Component[] parts = {
                DashboardRows.part(uiComponents, "date", dashboardDates.formatDate(entry.getDate())),
                createName(entry),
                DashboardRows.part(uiComponents, "value", componentSupport.formatWorkTimeTotal(spent)),
                componentSupport.createTimeEntryStatusRender().createComponent(entry)
        };

        Component main;
        if (openPermitted) {
            JmixButton open = DashboardRows.actionButton(uiComponents, event -> openEditor(entry), parts);
            open.addClassName("row-open");
            main = open;
        } else {
            Div content = uiComponents.create(Div.class);
            content.addClassName("row-open");
            content.add(parts);
            main = content;
        }

        Div row = DashboardRows.row(uiComponents, main);
        row.addClassName("recent-entry");
        if (copyPermitted) {
            // An icon, as the action repeats on every row; its name is the tooltip and what a screen reader says
            String copyText = messages.getMessage(getClass(), "recentEntriesBlock.copyToToday");
            JmixButton copyButton = DashboardRows.actionButton(uiComponents, event -> copyToToday(entry));
            copyButton.setIcon(GravityUiIcon.COPY.create());
            copyButton.setAriaLabel(copyText);
            copyButton.setTooltipText(copyText);
            copyButton.addClassName("row-copy");
            row.add(copyButton);
        }
        return row;
    }

    /**
     * Creates the entry's name: its task first, so a narrow row that cuts the name keeps the task, then its project.
     */
    private Span createName(TimeEntry entry) {
        Span name = uiComponents.create(Span.class);
        name.addClassName("name");
        name.add(DashboardRows.part(uiComponents, "task", entry.getTask().getName()));
        name.getElement().appendChild(Element.createText(" "));
        name.add(DashboardRows.part(uiComponents, "project", metadataTools.format(entry.getTask().getProject())));
        return name;
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
