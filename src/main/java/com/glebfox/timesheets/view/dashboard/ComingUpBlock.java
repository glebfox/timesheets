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
import com.glebfox.timesheets.app.util.ComingUp;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import io.jmix.core.Messages;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The Coming up block of the dashboard: the current user's absences that have not ended, then the next days off of
 * their holiday calendar, each nearest first, at most {@link #MAX_ROWS} of each; consecutive days off of one name are
 * one row, with their range. Nothing coming shows an empty state.
 */
@FragmentDescriptor("coming-up-block.xml")
public class ComingUpBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * How many absences, and how many holidays, the block shows.
     */
    static final int MAX_ROWS = 3;

    @ViewComponent
    private Div rows;
    @ViewComponent
    private GridEmptyState emptyState;

    @Autowired
    private DashboardSupport dashboardSupport;
    @Autowired
    private DashboardDates dashboardDates;
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
        ComingUp comingUp = dashboardSupport.loadComingUp(DashboardBlock.today(timeSource, currentAuthentication));

        rows.removeAll();
        comingUp.absences().stream().limit(MAX_ROWS).forEach(absence -> rows.add(createAbsenceRow(absence)));
        comingUp.holidays().stream().limit(MAX_ROWS).forEach(holiday -> rows.add(createHolidayRow(holiday)));

        boolean empty = rows.getChildren().findAny().isEmpty();
        rows.setVisible(!empty);
        emptyState.setVisible(empty);
    }

    private Component createAbsenceRow(WorkPlan.AbsencePeriod absence) {
        return createRow(absence.typeName(), messages.getMessage(getClass(), "comingUpBlock.absence"),
                dashboardDates.formatDates(absence.firstDay(), absence.lastDay()));
    }

    private Component createHolidayRow(ComingUp.Holiday holiday) {
        return createRow(holiday.name(), messages.getMessage(getClass(), "comingUpBlock.holiday"),
                dashboardDates.formatDates(holiday.firstDay(), holiday.lastDay()));
    }

    private Component createRow(String name, String kind, String dates) {
        Div text = uiComponents.create(Div.class);
        text.addClassName("name");
        text.add(DashboardRows.part(uiComponents, "title", name), DashboardRows.part(uiComponents, "kind", kind));
        return DashboardRows.row(uiComponents, text, DashboardRows.part(uiComponents, "value", dates));
    }
}
