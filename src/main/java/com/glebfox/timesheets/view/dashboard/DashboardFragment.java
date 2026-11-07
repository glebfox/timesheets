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
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.shared.Registration;
import io.jmix.core.AccessManager;
import io.jmix.core.Messages;
import io.jmix.core.Metadata;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.Subscribe;
import io.jmix.flowui.view.Target;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewComponent;
import io.jmix.tabbedmode.app.main.HasWorkArea;
import io.jmix.tabbedmode.component.workarea.WorkArea;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;
import java.util.Optional;

/**
 * The dashboard: a header with a greeting, today's date, Log time, and Refresh, then the My time section and the My
 * team section, each a grid of {@link DashboardBlock}s. The My team section shows only to a user who may open the
 * approval screen and approves on at least one project; for anyone else its blocks load nothing. It loads everything
 * it shows itself, through its blocks, so it works in any host.
 * <p>
 * It refreshes when its host is ready and, in a host with a {@link WorkArea} whose initial layout holds it, each time
 * the initial layout is shown again, after the last tab closes; it loads nothing while a tab hides it.
 */
@FragmentDescriptor("dashboard-fragment.xml")
public class DashboardFragment extends Fragment<Div> {

    @ViewComponent
    private H2 greeting;
    @ViewComponent
    private Span today;
    @ViewComponent
    private JmixButton logTimeButton;
    @ViewComponent
    private H3 myTimeHeading;
    @ViewComponent
    private Div myTimeBlocks;
    @ViewComponent
    private H3 myTeamHeading;
    @ViewComponent
    private Div myTeamBlocks;

    @Autowired
    private Messages messages;
    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private CurrentUserSubstitution currentUserSubstitution;
    @Autowired
    private TimeSource timeSource;
    @Autowired
    private AccessManager accessManager;
    @Autowired
    private Metadata metadata;
    @Autowired
    private DialogWindows dialogWindows;
    @Autowired
    private ApprovalOpener approvalOpener;
    @Autowired
    private TeamDashboardSupport teamDashboardSupport;

    private @Nullable Registration workAreaStateRegistration;

    @Subscribe(target = Target.HOST_CONTROLLER)
    public void onHostReady(final View.ReadyEvent event) {
        Optional<WorkArea> workArea = findWorkArea();
        // The host's ready event fires again on each navigation to the host while it is open, so the listener is
        // added once, or each show would refresh once per navigation
        if (workAreaStateRegistration == null) {
            workAreaStateRegistration = workArea
                    .map(area -> area.addStateChangeListener(stateChangeEvent -> {
                        if (stateChangeEvent.getState() == WorkArea.State.INITIAL_LAYOUT) {
                            refresh();
                        }
                    }))
                    .orElse(null);
        }

        // A main view opened on a tab, such as by a link to a screen, shows the dashboard only once the tab closes
        if (workArea.map(area -> area.getState() == WorkArea.State.INITIAL_LAYOUT).orElse(true)) {
            refresh();
        }
    }

    /**
     * Reloads the dashboard: the header, every block, and which sections and headings show.
     */
    public void refresh() {
        updateHeader();

        refreshBlocks(myTimeBlocks);
        // The team section is for a user who approves: it loads nothing for anyone else
        if (isTeamPermitted()) {
            refreshBlocks(myTeamBlocks);
        } else {
            myTeamBlocks.setVisible(false);
        }

        // The headings tell the sections apart, so they show only when both sections do
        boolean bothSections = myTimeBlocks.isVisible() && myTeamBlocks.isVisible();
        myTimeHeading.setVisible(bothSections);
        myTeamHeading.setVisible(bothSections);
    }

    @Subscribe(id = "refreshButton", subject = "clickListener")
    public void onRefreshButtonClick(final ClickEvent<JmixButton> event) {
        refresh();
    }

    @Subscribe(id = "logTimeButton", subject = "clickListener")
    public void onLogTimeButtonClick(final ClickEvent<JmixButton> event) {
        dialogWindows.detail(FragmentUtils.getHostView(getParentController()), TimeEntry.class)
                .withViewClass(TimeEntryDetailView.class)
                .withViewConfigurer(editor -> editor.setOwnTimeEntry(true))
                .newEntity()
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        refresh();
                    }
                })
                .open();
    }

    private Optional<WorkArea> findWorkArea() {
        View<?> host = FragmentUtils.getHostView(getParentController());
        return host instanceof HasWorkArea hasWorkArea
                ? hasWorkArea.getWorkAreaOptional()
                : Optional.empty();
    }

    private void updateHeader() {
        Locale locale = currentAuthentication.getLocale();
        ZonedDateTime now = timeSource.now().withZoneSameInstant(currentAuthentication.getTimeZone().toZoneId());

        Greeting timeOfDay = Greeting.of(now.toLocalTime());
        greeting.setText(messages.formatMessage(DashboardFragment.class, timeOfDay.getMessageKey(), locale,
                userName()));

        LocalDate date = now.toLocalDate();
        String dateText = StringUtils.capitalize(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL)
                .withLocale(locale)
                .format(date));
        int week = date.get(WeekFields.of(locale).weekOfWeekBasedYear());
        today.setText(messages.formatMessage(DashboardFragment.class, "dashboard.today", locale, dateText, week));

        logTimeButton.setVisible(isTimeEntryCreatePermitted());
    }

    private String userName() {
        UserDetails user = currentUserSubstitution.getEffectiveUser();
        if (user instanceof User appUser && StringUtils.isNotBlank(appUser.getFirstName())) {
            return appUser.getFirstName();
        }
        return user.getUsername();
    }

    /**
     * Whether the current user sees the My team section: they may open the approval screen and approve on at least
     * one project, so a user who approves nowhere sees none, Full access included.
     */
    private boolean isTeamPermitted() {
        return approvalOpener.isPermitted() && teamDashboardSupport.approvesAnywhere();
    }

    private boolean isTimeEntryCreatePermitted() {
        UiEntityContext context = new UiEntityContext(metadata.getClass(TimeEntry.class));
        accessManager.applyRegisteredConstraints(context);
        return context.isCreatePermitted();
    }

    private static void refreshBlocks(Div section) {
        boolean anyShown = false;
        for (Component child : section.getChildren().toList()) {
            if (child instanceof DashboardBlock block) {
                block.refresh();
                anyShown |= child.isVisible();
            }
        }
        section.setVisible(anyShown);
    }
}
