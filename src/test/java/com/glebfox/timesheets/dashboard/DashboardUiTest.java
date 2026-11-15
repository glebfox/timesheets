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

package com.glebfox.timesheets.dashboard;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.UserTimeZoneSupport;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.security.UiMinimalRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.dashboard.DashboardDates;
import com.glebfox.timesheets.view.dashboard.DashboardFragment;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.tasktype.TaskTypeListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HasText;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.Fragments;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.temporal.WeekFields;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// The main view is named: by default, the extension looks it up once per class in a view registry that the previous
// class's teardown has emptied, and then opens its own blank view instead
@UiTest(initialView = MainView.class)
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class DashboardUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    CurrentAuthentication currentAuthentication;
    @Autowired
    UserTimeZoneSupport userTimeZoneSupport;
    @Autowired
    Fragments fragments;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    DashboardDates dashboardDates;

    ApprovalTestData data;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "dashboard-ui");
    }

    @AfterEach
    void tearDown() {
        // The entries saved through the editor refer to the test's users and tasks, so they go first
        List<TimeEntry> entries = dataManager.load(TimeEntry.class)
                .query("e.user.username like :prefix")
                .parameter("prefix", "dashboard-ui-%")
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list();
        if (!entries.isEmpty()) {
            dataManager.save(new SaveContext()
                    .removing(entries.toArray())
                    .setHint(PersistenceHints.SOFT_DELETION, false));
        }
        data.removeAll();
    }

    @Test
    void initialLayoutHoldsDashboardTest() {
        MainView mainView = mainView();

        VerticalLayout initialLayout = mainView.getWorkArea().getInitialLayout();
        List<Component> children = initialLayout.getChildren().toList();
        assertThat(children).first().isInstanceOf(DrawerToggle.class);
        assertThat(children).hasAtLeastOneElementOfType(DashboardFragment.class);
        assertThat(children).contains(dashboard());
    }

    @Test
    void greetingUsesFirstNameElseUsernameTest() {
        User named = data.saveUser("named", "Chell", null);
        User unnamed = data.saveUser("unnamed", null, null);
        DashboardFragment dashboard = dashboard();
        HasText greeting = component(dashboard, "greeting");

        systemAuthenticator.runWithUser(named.getUsername(), dashboard::refresh);
        assertThat(greeting.getText()).contains("Chell").doesNotContain(named.getUsername());

        systemAuthenticator.runWithUser(unnamed.getUsername(), dashboard::refresh);
        assertThat(greeting.getText()).contains(unnamed.getUsername());
    }

    @Test
    void dateLineShowsTheWeekNumberTest() {
        DashboardFragment dashboard = dashboard();
        dashboard.refresh();

        HasText today = component(dashboard, "today");
        // The user's today, which the header counts from as every block does
        LocalDate date = userTimeZoneSupport.today();
        int week = date.get(WeekFields.of(currentAuthentication.getLocale()).weekOfWeekBasedYear());
        // The dashboard's short date, without the year, as it is the current one
        assertThat(today.getText())
                .contains(dashboardDates.formatDate(date))
                .doesNotContain(String.valueOf(date.getYear()))
                .endsWith(" " + week);
    }

    @Test
    void logTimeHiddenWithoutCreatePermissionTest() {
        User minimal = data.saveUser("minimal", "Minimal", "Access");
        assignRole(minimal, UiMinimalRole.CODE);
        User full = data.saveUser("full", "Full", "Access");
        assignRole(full, FullAccessRole.CODE);
        DashboardFragment dashboard = dashboard();
        JmixButton logTimeButton = component(dashboard, "logTimeButton");

        systemAuthenticator.runWithUser(minimal.getUsername(), dashboard::refresh);
        assertThat(logTimeButton.isVisible()).isFalse();

        systemAuthenticator.runWithUser(full.getUsername(), dashboard::refresh);
        assertThat(logTimeButton.isVisible()).isTrue();
    }

    @Test
    void logTimeOpensOwnEntryForTodayTest() {
        User user = data.saveUser("logger", "Log", "Ger");
        assignRole(user, FullAccessRole.CODE);
        DashboardFragment dashboard = dashboard();

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            dashboard.refresh();
            TimeEntryDetailView editor = clickLogTime(dashboard);

            TimeEntry entry = editor.getEditedEntity();
            assertThat(entry.getUser()).isNotNull();
            assertThat(entry.getUser().getId()).isEqualTo(user.getId());
            // Dated by the user's today, as Copy to today and every block count, not by the server's
            assertThat(entry.getDate()).isEqualTo(userTimeZoneSupport.today());
            editor.closeWithDiscard();
        });
    }

    @Test
    void saveInDialogRefreshesTest() {
        User user = data.saveUser("saver", "Save", "R");
        assignRole(user, FullAccessRole.CODE);
        Project project = data.saveProject("project", data.saveClient());
        Task task = data.saveTask(project, data.saveTaskType());
        DashboardFragment dashboard = dashboard();
        CountingTestBlock block = addBlock(dashboard, "myTimeBlocks");

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            dashboard.refresh();
            int before = block.getRefreshes();

            // A discarded dialog reloads nothing
            clickLogTime(dashboard).closeWithDiscard();
            assertThat(block.getRefreshes()).isEqualTo(before);

            TimeEntryDetailView editor = clickLogTime(dashboard);
            editor.getEditedEntity().setTask(task);
            editor.getEditedEntity().setSpentTime(WorkTime.ofHours(2));
            UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();

            assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(editor);
            assertThat(block.getRefreshes()).isEqualTo(before + 1);
        });
    }

    @Test
    void blockAsksTheDashboardToRefreshTest() {
        // The team section refreshes only for a user who approves
        User approver = saveApprover();
        DashboardFragment dashboard = dashboard();
        CountingTestBlock asking = addBlock(dashboard, "myTimeBlocks");
        CountingTestBlock other = addBlock(dashboard, "myTeamBlocks");
        int before = other.getRefreshes();

        systemAuthenticator.runWithUser(approver.getUsername(), asking::refreshDashboard);

        assertThat(other.getRefreshes()).isEqualTo(before + 1);
    }

    @Test
    void showingAgainRefreshesTest() {
        MainView mainView = mainView();
        DashboardFragment dashboard = dashboard();
        CountingTestBlock block = addBlock(dashboard, "myTimeBlocks");
        int before = block.getRefreshes();

        viewBuilders.view(mainView, TaskTypeListView.class).open();
        View<?> opened = UiTestUtils.getCurrentView();
        assertThat(opened).isInstanceOf(TaskTypeListView.class);
        // Hidden behind a tab, the dashboard reloads nothing
        assertThat(block.getRefreshes()).isEqualTo(before);

        opened.close(StandardOutcome.CLOSE);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(mainView);
        assertThat(block.getRefreshes()).isEqualTo(before + 1);
    }

    @Test
    void hostReadyAgainKeepsOneRefreshPerShowTest() {
        MainView mainView = mainView();
        DashboardFragment dashboard = dashboard();
        CountingTestBlock block = addBlock(dashboard, "myTimeBlocks");

        // Navigating to the main view while it is open fires its ready event again for the same instance
        ViewControllerUtils.fireEvent(mainView, new View.ReadyEvent(mainView));
        ViewControllerUtils.fireEvent(mainView, new View.ReadyEvent(mainView));
        int before = block.getRefreshes();

        viewBuilders.view(mainView, TaskTypeListView.class).open();
        UiTestUtils.<View<?>>getCurrentView().close(StandardOutcome.CLOSE);

        assertThat(block.getRefreshes()).isEqualTo(before + 1);
    }

    @Test
    void headingsShowOnlyWithBothSectionsTest() {
        // The team section shows only to a user who approves; its own blocks give way to the test's
        User approver = saveApprover();
        DashboardFragment dashboard = dashboard();
        HasComponents myTeamBlocks = component(dashboard, "myTeamBlocks");
        myTeamBlocks.removeAll();

        systemAuthenticator.runWithUser(approver.getUsername(), () -> assertHeadings(dashboard));
    }

    private void assertHeadings(DashboardFragment dashboard) {
        Component myTimeHeading = component(dashboard, "myTimeHeading");
        Component myTeamHeading = component(dashboard, "myTeamHeading");
        Component myTimeBlocks = component(dashboard, "myTimeBlocks");
        Component myTeamBlocks = component(dashboard, "myTeamBlocks");

        // No team block: the team section hides, and so do both headings
        CountingTestBlock timeBlock = addBlock(dashboard, "myTimeBlocks");
        dashboard.refresh();
        assertThat(myTimeBlocks.isVisible()).isTrue();
        assertThat(myTeamBlocks.isVisible()).isFalse();
        assertThat(myTimeHeading.isVisible()).isFalse();
        assertThat(myTeamHeading.isVisible()).isFalse();

        // A team block that shows: both sections and both headings show
        CountingTestBlock teamBlock = addBlock(dashboard, "myTeamBlocks");
        dashboard.refresh();
        assertThat(myTeamBlocks.isVisible()).isTrue();
        assertThat(myTimeHeading.isVisible()).isTrue();
        assertThat(myTeamHeading.isVisible()).isTrue();

        // A section whose every block hides itself hides too, and the headings with it
        teamBlock.setShown(false);
        dashboard.refresh();
        assertThat(teamBlock.isVisible()).isFalse();
        assertThat(myTeamBlocks.isVisible()).isFalse();
        assertThat(myTimeHeading.isVisible()).isFalse();
        assertThat(timeBlock.isVisible()).isTrue();
    }

    /**
     * Saves a user with Full access who is an Approver on a project, so the dashboard shows them the team section.
     */
    private User saveApprover() {
        User approver = data.saveUser("approver", "Isaac", "Kleiner");
        assignRole(approver, FullAccessRole.CODE);
        Project project = data.saveProject("approved", data.saveClient());
        data.saveParticipant(project, approver, data.saveRole(ProjectRoleType.APPROVER));
        return approver;
    }

    private MainView mainView() {
        View<?> view = UiTestUtils.getCurrentView();
        assertThat(view).isInstanceOf(MainView.class);
        return (MainView) view;
    }

    private DashboardFragment dashboard() {
        return UiTestUtils.getComponent(mainView(), "dashboard");
    }

    @SuppressWarnings("unchecked")
    private static <T> T component(DashboardFragment dashboard, String id) {
        return (T) FragmentUtils.getComponent(dashboard, id);
    }

    private CountingTestBlock addBlock(DashboardFragment dashboard, String sectionId) {
        CountingTestBlock block = fragments.create(dashboard, CountingTestBlock.class);
        HasComponents section = component(dashboard, sectionId);
        section.add(block);
        return block;
    }

    private static TimeEntryDetailView clickLogTime(DashboardFragment dashboard) {
        JmixButton logTimeButton = component(dashboard, "logTimeButton");
        assertThat(logTimeButton.isVisible()).isTrue();
        logTimeButton.click();

        View<?> dialog = UiTestUtils.getLastOpenedViewDialog();
        assertThat(dialog).isInstanceOf(TimeEntryDetailView.class);
        return (TimeEntryDetailView) dialog;
    }

    private void assignRole(User user, String roleCode) {
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        });
    }
}
