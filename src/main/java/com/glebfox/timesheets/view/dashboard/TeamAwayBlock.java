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

import com.glebfox.timesheets.app.TeamDashboardSupport;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PersonAbsence;
import com.glebfox.timesheets.component.approval.ApprovalRosterCard;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;

/**
 * The Team away block of the dashboard: the absences of the current user's team that share a day with this week or
 * the next, nearest first, each with the person's avatar and name, the absence's type, and its dates. No one away
 * shows an empty state.
 */
@FragmentDescriptor("team-away-block.xml")
public class TeamAwayBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * How many weeks the block covers, the current one included.
     */
    static final int WEEKS = 2;

    @ViewComponent
    private Div rows;
    @ViewComponent
    private GridEmptyState emptyState;

    @Autowired
    private TeamDashboardSupport teamDashboardSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private DashboardDates dashboardDates;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Override
    public void refresh() {
        LocalDate weekStart = ApprovalPeriod.weekStartOf(DashboardBlock.today(timeSource, currentAuthentication),
                componentSupport.getDaysOfWeek().getFirst());
        List<PersonAbsence> absences = teamDashboardSupport.loadTeamAbsences(weekStart,
                weekStart.plusWeeks(WEEKS).minusDays(1));

        rows.removeAll();
        absences.forEach(absence -> rows.add(createRow(absence)));

        rows.setVisible(!absences.isEmpty());
        emptyState.setVisible(absences.isEmpty());
    }

    private Component createRow(PersonAbsence absence) {
        String name = ApprovalRosterCard.nameOf(absence.user());

        Div text = uiComponents.create(Div.class);
        text.addClassName("name");
        text.add(DashboardRows.part(uiComponents, "title", name),
                DashboardRows.part(uiComponents, "kind", absence.absence().typeName()));

        return DashboardRows.row(uiComponents,
                componentSupport.createAvatar(name, absence.user().getAvatar(), "avatar"),
                text,
                DashboardRows.part(uiComponents, "value",
                        dashboardDates.formatDates(absence.absence().firstDay(), absence.absence().lastDay())));
    }
}
