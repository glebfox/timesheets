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
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.TimeSource;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
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
 * The Recent entries block of the dashboard: the current user's {@link #LIMIT} latest entries, each with its date,
 * task and project, time spent, and status. A click on a row opens the entry in the time entry editor, in a dialog,
 * if the user may open the editor; Copy to today opens the editor on a new entry copied from it and dated today, if
 * the user may create time entries. After a save in either, the dashboard reloads. No entries shows an empty state.
 */
@FragmentDescriptor("recent-entries-block.xml")
public class RecentEntriesBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * How many entries the block shows.
     */
    static final int LIMIT = 5;

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
    private DatatypeFormatter datatypeFormatter;
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

        rows.setVisible(!entries.isEmpty());
        emptyState.setVisible(entries.isEmpty());
    }

    private Component createRow(TimeEntry entry, boolean openPermitted, boolean copyPermitted) {
        WorkTime spent = entry.getSpentTime() != null ? entry.getSpentTime() : WorkTime.ZERO;
        Component[] parts = {
                DashboardRows.part(uiComponents, "date", datatypeFormatter.formatLocalDate(entry.getDate())),
                DashboardRows.part(uiComponents, "name", messages.formatMessage(getClass(),
                        "recentEntriesBlock.task", metadataTools.format(entry.getTask().getProject()),
                        entry.getTask().getName())),
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
        if (copyPermitted) {
            JmixButton copyButton = DashboardRows.actionButton(uiComponents, event -> copyToToday(entry));
            copyButton.addThemeName("small");
            copyButton.setIcon(GravityUiIcon.COPY.create());
            copyButton.setText(messages.getMessage(getClass(), "recentEntriesBlock.copyToToday"));
            copyButton.addClassName("row-copy");
            row.add(copyButton);
        }
        return row;
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
