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
import com.glebfox.timesheets.app.util.Attention;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.ShortDay;
import com.glebfox.timesheets.app.util.WeekSummary;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import io.jmix.core.AccessManager;
import io.jmix.core.Messages;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.tabbedmode.ViewBuilders;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * The Needs attention block of the dashboard: the current user's rejected entries, then the days of this and the
 * previous week that are short, then the recent weeks that are short, at most {@link #MAX_ROWS} rows in all, and
 * "and N more" for the rest. A short day or week opens the calendar on it, if the user may open the calendar. A link in
 * the card's header opens My time entries, if the user may open it. Nothing to show reads "All caught up".
 */
@FragmentDescriptor("needs-attention-block.xml")
public class NeedsAttentionBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * How many rows the block shows; the rest of what needs attention is counted in "and N more".
     */
    static final int MAX_ROWS = 5;

    @ViewComponent
    private Div rows;
    @ViewComponent
    private GridEmptyState emptyState;
    @ViewComponent
    private JmixButton myEntriesLink;

    @Autowired
    private DashboardSupport dashboardSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private DashboardDates dashboardDates;
    @Autowired
    private CalendarOpener calendarOpener;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private ViewBuilders viewBuilders;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Messages messages;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Override
    public void refresh() {
        LocalDate today = DashboardBlock.today(timeSource, currentAuthentication);
        Attention attention = dashboardSupport.loadAttention(today, componentSupport.getDaysOfWeek().getFirst());
        boolean calendarPermitted = calendarOpener.isPermitted();

        // The rows are made only for what the block shows
        List<Supplier<Component>> all = new ArrayList<>();
        if (attention.rejectedCount() > 0) {
            all.add(() -> createRejectedRow(attention.rejectedCount(), isMyTimeEntriesPermitted()));
        }
        for (ShortDay day : attention.shortDays()) {
            all.add(() -> createDayRow(day, calendarPermitted));
        }
        for (WeekSummary week : attention.shortWeeks()) {
            all.add(() -> createWeekRow(week, calendarPermitted));
        }

        rows.removeAll();
        all.stream().limit(MAX_ROWS).forEach(row -> rows.add(row.get()));
        if (all.size() > MAX_ROWS) {
            rows.add(DashboardRows.more(uiComponents, messages, all.size() - MAX_ROWS,
                    currentAuthentication.getLocale()));
        }

        rows.setVisible(!all.isEmpty());
        emptyState.setVisible(all.isEmpty());
        // Left out of the header, not hidden in it: a hidden link would still keep its place beside the title
        getContent().setHeaderSuffix(isMyTimeEntriesPermitted() ? myEntriesLink : null);
    }

    @Subscribe(id = "myEntriesLink", subject = "clickListener")
    public void onMyEntriesLinkClick(final ClickEvent<JmixButton> event) {
        openMyTimeEntries();
    }

    /**
     * Opens the current user's time entries, if the user may open them; otherwise does nothing. The list opens
     * with its filter empty: it has no way to be opened filtered by status, and in tabbed mode its URL query
     * parameters are not read.
     */
    public void openMyTimeEntries() {
        if (isMyTimeEntriesPermitted()) {
            viewBuilders.view(FragmentUtils.getHostView(getParentController()), MyTimeEntryListView.class).open();
        }
    }

    private boolean isMyTimeEntriesPermitted() {
        return DashboardAccess.isViewPermitted(accessManager, MyTimeEntryListView.ID);
    }

    /**
     * Creates the row of the rejected entries: their name, and their number in the badge the main menu's My time
     * entries item shows, which opens the user's entries where the user may open them.
     */
    private Component createRejectedRow(long count, boolean clickable) {
        Component[] parts = {
                DashboardRows.part(uiComponents, "name", messages.getMessage(getClass(),
                        "needsAttentionBlock.rejected")),
                componentSupport.createRejectedCountBadge(count)
        };
        return clickable
                ? DashboardRows.buttonRow(uiComponents, event -> openMyTimeEntries(), parts)
                : DashboardRows.row(uiComponents, parts);
    }

    private Component createDayRow(ShortDay day, boolean clickable) {
        String name = messages.formatMessage(getClass(), "needsAttentionBlock.day",
                componentSupport.formatDayOfWeekShort(day.date().getDayOfWeek()),
                dashboardDates.formatDate(day.date()));
        return createShortRow(name, day.missing(), clickable, day.date());
    }

    private Component createWeekRow(WeekSummary week, boolean clickable) {
        return createShortRow(dashboardDates.formatDates(week.firstDay(), week.lastDay()),
                week.total().difference(), clickable, week.firstDay());
    }

    private Component createShortRow(String name, WorkTime missing, boolean clickable, LocalDate date) {
        Component[] parts = {
                DashboardRows.part(uiComponents, "name", name),
                DashboardRows.part(uiComponents, "value", messages.formatMessage(getClass(),
                        "needsAttentionBlock.missing", componentSupport.formatWorkTimeTotal(missing)))
        };
        return clickable
                ? DashboardRows.buttonRow(uiComponents, event -> calendarOpener.open(this, date), parts)
                : DashboardRows.row(uiComponents, parts);
    }
}
