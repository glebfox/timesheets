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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.UserTimeZoneSupport;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.project.ProjectDetailView;
import com.glebfox.timesheets.view.project.ProjectListView;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.glebfox.timesheets.view.timeentry.TimeEntryListView;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import io.jmix.core.DataManager;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.flowuidata.entity.UserSettingsItem;
import io.jmix.fullcalendarflowui.component.FullCalendar;
import io.jmix.fullcalendarflowui.component.data.ContainerCalendarDataProvider;
import io.jmix.fullcalendarflowui.component.data.EntityCalendarEvent;
import io.jmix.fullcalendarflowui.component.event.DatesSetEvent;
import io.jmix.fullcalendarflowui.component.event.DisplayModeInfo;
import io.jmix.fullcalendarflowui.kit.component.model.CalendarDisplayModes;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each acting user holds a job role alone, with the row-level role that the combination table gives it. The fixture
 * is project P, where {@code worker} and {@code member} are Members and {@code manager} is the Manager, with an entry
 * of the worker's and one of the member's on a day of December 2024, which the calendar shows, and one of the
 * member's today; project O, where {@code observer} is an Observer; and project Q, where none of the three takes
 * part.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class JobRolesUiTest {

    static final String USERNAME_PREFIX = "job-roles-ui-test-";

    // December 2024 starts on Sunday, so its month grid runs from December 1 to January 4
    static final LocalDate GRID_START = LocalDate.of(2024, 12, 1);
    static final LocalDate GRID_END = LocalDate.of(2025, 1, 5);
    static final LocalDate DAY = LocalDate.of(2024, 12, 3);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserTimeZoneSupport userTimeZoneSupport;

    ApprovalTestData data;

    User worker;
    User manager;
    User observer;
    User member;

    Project observed;
    Project unshared;

    TimeEntry workerEntry;
    TimeEntry memberEntry;
    TimeEntry memberEntryToday;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));

        worker = saveHolder("worker", WorkerRole.CODE, ProjectScopeRole.CODE);
        manager = saveHolder("manager", ManagerRole.CODE, ProjectScopeRole.CODE);
        observer = saveHolder("observer", ObserverRole.CODE, ClientScopeRole.CODE);
        member = data.saveUser("member", "Member", "Member");

        Client client = data.saveClient();
        TaskType taskType = data.saveTaskType();
        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);

        Project managed = data.saveProject("P", client);
        Task task = data.saveTask(managed, taskType);
        data.saveParticipant(managed, worker, memberRole);
        data.saveParticipant(managed, member, memberRole);
        data.saveParticipant(managed, manager, data.saveRole(ProjectRoleType.MANAGER));

        observed = data.saveProject("O", client);
        data.saveTask(observed, taskType);
        data.saveParticipant(observed, observer, data.saveRole(ProjectRoleType.OBSERVER));
        data.saveParticipant(observed, member, memberRole);

        unshared = data.saveProject("Q", client);
        data.saveParticipant(unshared, member, memberRole);

        workerEntry = data.saveEntry(task, worker, DAY, NEW, null);
        memberEntry = data.saveEntry(task, member, DAY, NEW, null);
        // The manager's today, which the approval screen counts its periods from
        LocalDate today = systemAuthenticator.withUser(manager.getUsername(), userTimeZoneSupport::today);
        memberEntryToday = data.saveEntry(task, member, today, NEW, null);
    }

    @Test
    void workerOpensTheCalendarAndMyTimeEntries() {
        as(worker, () -> {
            CalendarView calendarView = openCalendar();
            assertThat(calendarEntryIds(calendarView))
                    .contains(workerEntry.getId())
                    .doesNotContain(memberEntry.getId());

            View<?> list = openView(MyTimeEntryListView.class);
            assertThat(itemIds(container(list, "timeEntriesDc")))
                    .contains(workerEntry.getId())
                    .doesNotContain(memberEntry.getId(), memberEntryToday.getId());
        });
    }

    @Test
    void observerOpensTheProjectListAndAProjectReadOnly() {
        as(observer, () -> {
            View<?> list = openView(ProjectListView.class);
            assertThat(itemIds(container(list, "projectsDc")))
                    .contains(observed.getId())
                    .doesNotContain(unshared.getId());
            DataGrid<Project> projectsDataGrid = UiTestUtils.getComponent(list, "projectsDataGrid");
            assertThat(Objects.requireNonNull(projectsDataGrid.getAction("create")).isEnabled()).isFalse();

            viewBuilders.detail(UiTestUtils.getCurrentView(), Project.class, ProjectDetailView.class)
                    .editEntity(observed)
                    .open();
            ProjectDetailView editor = UiTestUtils.getCurrentView();
            attachSelectedTab();

            assertThat(editor.isReadOnly()).isTrue();
            assertThat(UiTestUtils.<TypedTextField<String>>getComponent(editor, "nameField").isReadOnly()).isTrue();
            DataGrid<Task> tasksDataGrid = UiTestUtils.getComponent(editor, "tasksDataGrid");
            assertThat(Objects.requireNonNull(tasksDataGrid.getAction("create")).isEnabled()).isFalse();
        });
    }

    @Test
    void managerOpensTheApprovalScreenAndTheTimeEntriesList() {
        as(manager, () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), TimeEntryApprovalView.class).open();
            TimeEntryApprovalView approvalView = UiTestUtils.getCurrentView();
            assertThat(approvalView.getRosterPeople()).extracting(person -> person.user().getUsername())
                    .contains(member.getUsername());
            assertThat(approvalView.getRosterPeople().stream()
                    .flatMap(person -> person.entries().stream())
                    .map(TimeEntry::getId))
                    .contains(memberEntryToday.getId());

            View<?> list = openView(TimeEntryListView.class);
            CollectionContainer<TimeEntry> container = container(list, "timeEntriesDc");
            assertThat(itemIds(container)).contains(workerEntry.getId(), memberEntry.getId());

            // The list logs time for anyone, and the manager may not set an entry's user
            DataGrid<TimeEntry> grid = UiTestUtils.getComponent(list, "timeEntriesDataGrid");
            assertThat(Objects.requireNonNull(grid.getAction("create")).isVisible()).isFalse();
            assertThat(Objects.requireNonNull(grid.getAction("copy")).isVisible()).isFalse();

            // Another person's entry opens read only: the manager reads it but may not change it
            grid.select(container.getItem(memberEntry.getId()));
            Action edit = Objects.requireNonNull(grid.getAction("edit"));
            assertThat(edit.isEnabled()).isTrue();
            edit.actionPerform(grid);

            TimeEntryDetailView editor = UiTestUtils.getCurrentView();
            assertThat(editor.getEditedEntity().getId()).isEqualTo(memberEntry.getId());
            assertThat(editor.isReadOnly()).isTrue();
        });
    }

    @AfterEach
    void tearDown() {
        // The settings the screens saved for the holders first, then the fixture
        List<String> usernames = Stream.of(worker, manager, observer)
                .filter(Objects::nonNull)
                .map(User::getUsername)
                .toList();
        dataManager.load(UserSettingsItem.class)
                .query("e.username in :usernames")
                .parameter("usernames", usernames)
                .list()
                .forEach(dataManager::remove);
        data.removeAll();
    }

    private User saveHolder(String name, String jobRole, String rowLevelRole) {
        User user = data.saveUser(name, name, name);
        assignRole(user, jobRole, RoleAssignmentRoleType.RESOURCE);
        assignRole(user, rowLevelRole, RoleAssignmentRoleType.ROW_LEVEL);
        return user;
    }

    private void assignRole(User user, String roleCode, String roleType) {
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(roleType);
        });
    }

    private void as(User user, Runnable action) {
        systemAuthenticator.runWithUser(user.getUsername(), action);
    }

    private View<?> openView(Class<? extends View<?>> viewClass) {
        viewBuilders.view(UiTestUtils.getCurrentView(), viewClass).open();
        return UiTestUtils.getCurrentView();
    }

    private static <E> CollectionContainer<E> container(View<?> view, String containerId) {
        return ViewControllerUtils.getViewData(view).getContainer(containerId);
    }

    private static List<Object> itemIds(CollectionContainer<?> container) {
        return container.getItems().stream()
                .map(EntityValues::getId)
                .toList();
    }

    private CalendarView openCalendar() {
        viewBuilders.view(UiTestUtils.getCurrentView(), CalendarView.class).open();
        CalendarView calendarView = UiTestUtils.getCurrentView();

        // The browser reports the displayed dates, which makes the calendar load its plan and entries
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ComponentUtil.fireEvent(calendar, new DatesSetEvent(calendar, true, GRID_START, GRID_END,
                new DisplayModeInfo(GRID_START, GRID_END, GRID_START, GRID_START.plusMonths(1),
                        CalendarDisplayModes.DAY_GRID_MONTH)));

        return calendarView;
    }

    @SuppressWarnings("unchecked")
    private static List<Object> calendarEntryIds(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ContainerCalendarDataProvider<TimeEntryCalendarWrapper> dataProvider = Objects.requireNonNull(
                calendar.getDataProvider("timeEntriesDataProvider"), "The calendar has no time entries");
        return dataProvider.getItems().stream()
                .map(calendarEvent -> (EntityCalendarEvent<TimeEntryCalendarWrapper>) calendarEvent)
                .map(calendarEvent -> (Object) calendarEvent.getEntity().getTimeEntry().getId())
                .toList();
    }

    /**
     * The tab sheet attaches the selected tab's content to the view only before the client response, which a UI test
     * never sends; until then a grid on it is not in the view.
     */
    private static void attachSelectedTab() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
    }
}
