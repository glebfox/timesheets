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
import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.app.util.WaitingApproval;
import com.glebfox.timesheets.component.approval.ApprovalRosterCard;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.glebfox.timesheets.view.approval.ApprovalOpening;
import com.glebfox.timesheets.view.approval.ApprovalRosterScope;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.Messages;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.component.card.JmixCard;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.ViewComponent;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;

/**
 * The Waiting for my approval block of the dashboard: the number of New entries the current user approves, their
 * time, and the date of the oldest, then the {@link #MAX_ROWS} people with the most of them, and "and N more" for the
 * rest. A click on a person opens the approval screen on the month of their oldest waiting entry, with them selected;
 * the link below opens it as the menu does. Nothing waiting shows an empty state.
 */
@FragmentDescriptor("waiting-approval-block.xml")
public class WaitingApprovalBlock extends Fragment<JmixCard> implements DashboardBlock {

    /**
     * How many people the block lists; the rest are counted in "and N more".
     */
    static final int MAX_ROWS = 5;

    @ViewComponent
    private Div figure;
    @ViewComponent
    private Span count;
    @ViewComponent
    private Span note;
    @ViewComponent
    private Div rows;
    @ViewComponent
    private GridEmptyState emptyState;

    @Autowired
    private TeamDashboardSupport teamDashboardSupport;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private ApprovalOpener approvalOpener;
    @Autowired
    private UiComponents uiComponents;
    @Autowired
    private Messages messages;
    @Autowired
    private DashboardDates dashboardDates;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    @Override
    public void refresh() {
        WaitingApproval waiting = teamDashboardSupport.loadWaiting();
        LocalDate oldest = waiting.oldest();
        boolean empty = waiting.count() == 0 || oldest == null;

        rows.removeAll();
        figure.setVisible(!empty);
        rows.setVisible(!empty);
        emptyState.setVisible(empty);
        if (empty) {
            return;
        }

        Locale locale = currentAuthentication.getLocale();
        count.setText(String.valueOf(waiting.count()));
        note.setText(messages.formatMessage(getClass(),
                PluralForms.key("waitingApprovalBlock.note", waiting.count(), locale), locale,
                componentSupport.formatWorkTimeTotal(waiting.total()), dashboardDates.formatDate(oldest)));

        List<WaitingApproval.Person> people = waiting.people();
        people.stream().limit(MAX_ROWS).forEach(person -> rows.add(createPersonRow(person)));
        if (people.size() > MAX_ROWS) {
            rows.add(DashboardRows.more(uiComponents, messages, people.size() - MAX_ROWS, locale));
        }
    }

    /**
     * Opens the approval screen on the month of the person's oldest waiting entry, showing the people who need
     * review, with the person selected, if the current user may open the approval screen.
     *
     * @param person the person with waiting entries
     */
    public void openPerson(WaitingApproval.Person person) {
        approvalOpener.open(this, new ApprovalOpening(ApprovalMode.MONTH,
                ApprovalPeriod.monthOf(YearMonth.from(person.oldest())), ApprovalRosterScope.NEEDS_REVIEW,
                person.user()));
    }

    @Subscribe(id = "approvalLink", subject = "clickListener")
    public void onApprovalLinkClick(final ClickEvent<JmixButton> event) {
        approvalOpener.open(this, null);
    }

    private Component createPersonRow(WaitingApproval.Person person) {
        String name = ApprovalRosterCard.nameOf(person.user());
        return DashboardRows.buttonRow(uiComponents, event -> openPerson(person),
                componentSupport.createAvatar(name, person.user().getAvatar(), "avatar"),
                DashboardRows.part(uiComponents, "name", name),
                DashboardRows.part(uiComponents, "value", String.valueOf(person.count())));
    }
}
