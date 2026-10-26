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
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.holidaycalendar.HolidayCalendarDetailView;
import com.glebfox.timesheets.view.holidaycalendar.HolidayCalendarListView;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDayDetailView;
import com.glebfox.timesheets.view.holidaycalendarday.HolidayCalendarDaysAddView;
import com.glebfox.timesheets.view.user.UserDetailView;
import com.glebfox.timesheets.view.user.UserListView;
import com.glebfox.timesheets.view.workschedule.DefaultWorkScheduleDeleteView;
import com.glebfox.timesheets.view.workschedule.WorkScheduleDetailView;
import com.glebfox.timesheets.view.workschedule.WorkScheduleListView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleAssignmentDetailView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleUsersAddView;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.multiselectcomboboxpicker.JmixMultiSelectComboBoxPicker;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
import io.jmix.flowui.view.StandardDetailView;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class WorkScheduleManagementRoleUiTest {

    static final String USERNAME_PREFIX = "work-schedule-management-ui-test-";
    static final String NAME_PREFIX = "Work schedule management UI test ";

    static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    static final LocalDate JAN_1 = LocalDate.of(2027, 1, 1);

    // The security actions of the user list, by the dropdown items that show them; Role assignments has a button
    static final Map<String, String> DROPDOWN_ITEMS_BY_ACTION = Map.of(
            "changePassword", "changePasswordItem",
            "resetPassword", "resetPasswordItem",
            "showUserSubstitutionsAction", "showUserSubstitutionsItem");

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    String holder;
    List<Id<RoleAssignmentEntity>> roleAssignmentIds = new ArrayList<>();
    List<Id<WorkSchedule>> scheduleIds = new ArrayList<>();
    List<Id<HolidayCalendar>> calendarIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User user = saveUser("Holder");
        assignRole(user, UiMinimalRole.CODE);
        assignRole(user, WorkScheduleManagementRole.CODE);
        holder = user.getUsername();
    }

    @Test
    void holderEditsAssignmentsInAReadOnlyUserEditor() {
        User other = saveUser("Other");
        WorkSchedule schedule = saveSchedule();

        systemAuthenticator.runWithUser(holder, () -> {
            UserDetailView editor = openUserEditor(other);

            assertThat(editor.isReadOnly()).isTrue();
            assertThat(UiTestUtils.<TypedTextField<String>>getComponent(editor, "firstNameField").isReadOnly())
                    .isTrue();

            DataGrid<WorkScheduleAssignment> grid = UiTestUtils.getComponent(editor, "assignmentsDataGrid");
            Action create = action(grid, "create");
            assertThat(grid.getSelectedItems()).isEmpty();
            assertThat(create.isEnabled()).isTrue();
            create.actionPerform(grid);

            WorkScheduleAssignmentDetailView dialog = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(),
                    "the assignment editor");
            assertThat(dialog.isReadOnly()).isFalse();
            UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(dialog, "startDateField").setTypedValue(NOV_1);
            UiTestUtils.<EntityComboBox<WorkSchedule>>getComponent(dialog, "workScheduleField").setValue(schedule);
            clickOk(dialog);
            assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);

            clickOk(editor);
            assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        });

        List<WorkScheduleAssignment> assignments = dataManager.load(WorkScheduleAssignment.class)
                .query("e.user.id = :userId")
                .parameter("userId", other.getId())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("workSchedule", FetchPlan.INSTANCE_NAME))
                .list();
        assertThat(assignments).singleElement().satisfies(assignment -> {
            assertThat(assignment.getStartDate()).isEqualTo(NOV_1);
            assertThat(assignment.getWorkSchedule().getId()).isEqualTo(schedule.getId());
        });

        // OK saved the assignment only: no update of the user reached the database
        User reloaded = dataManager.load(Id.of(other)).one();
        assertThat(reloaded.getVersion()).isEqualTo(other.getVersion());
        assertThat(reloaded.getFirstName()).isEqualTo(other.getFirstName());
        assertThat(reloaded.getLastName()).isEqualTo(other.getLastName());
        assertThat(reloaded.getEmail()).isEqualTo(other.getEmail());
        assertThat(reloaded.getActive()).isEqualTo(other.getActive());
    }

    @Test
    void holderSeesNoPasswordOrRoleActions() {
        systemAuthenticator.runWithUser(holder, () -> assertSecurityActionsVisible(openUserList(), false));
    }

    @Test
    void adminStillSeesTheSecurityActions() {
        systemAuthenticator.runWithUser("admin", () -> assertSecurityActionsVisible(openUserList(), true));
    }

    @Test
    void holderOpensTheSettingsScreens() {
        User user = saveUser("Assigned");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(user, schedule);
        HolidayCalendar calendar = saveCalendarWithJan1();
        // Opening the default deletion dialog needs the seeded default
        WorkSchedule seedDefault = dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .one();

        systemAuthenticator.runWithUser(holder, () -> {
            checkWorkScheduleScreens(schedule, user, seedDefault);
            checkHolidayCalendarScreens(calendar);
        });

        assertThat(dataManager.load(Id.of(seedDefault)).one().getIsDefault()).isTrue();
    }

    @AfterEach
    void tearDown() {
        // Each test user with its assignments, then the role assignments, schedules, and calendars.
        // All are soft-deleted, so they are removed for good, reloaded by id
        List<User> users = dataManager.load(User.class)
                .query("e.username like :prefix")
                .parameter("prefix", USERNAME_PREFIX + "%")
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list();
        for (User user : users) {
            List<WorkScheduleAssignment> assignments = dataManager.load(WorkScheduleAssignment.class)
                    .query("e.user.id = :userId")
                    .parameter("userId", user.getId())
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list();
            if (!assignments.isEmpty()) {
                hardRemove(assignments.toArray());
            }
            hardRemove(user);
        }
        roleAssignmentIds.forEach(this::hardRemoveIfPresent);
        scheduleIds.forEach(this::hardRemoveIfPresent);
        // A calendar's days before the calendar: deleting a calendar does not delete its days
        for (Id<HolidayCalendar> id : calendarIds) {
            List<HolidayCalendarDay> days = dataManager.load(HolidayCalendarDay.class)
                    .query("e.calendar.id = :calendarId")
                    .parameter("calendarId", id.getValue())
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list();
            if (!days.isEmpty()) {
                hardRemove(days.toArray());
            }
            hardRemoveIfPresent(id);
        }
        roleAssignmentIds.clear();
        scheduleIds.clear();
        calendarIds.clear();
    }

    private void checkWorkScheduleScreens(WorkSchedule schedule, User assignedUser, WorkSchedule seedDefault) {
        viewBuilders.view(UiTestUtils.getCurrentView(), WorkScheduleListView.class).open();
        WorkScheduleListView list = UiTestUtils.getCurrentView();
        DataGrid<WorkSchedule> grid = UiTestUtils.getComponent(list, "workSchedulesDataGrid");

        WorkScheduleDetailView created = openFromList(grid, "create");
        created.closeWithDiscard();

        // Make default needs the update permission; it is not performed, since it would change the default
        grid.select(listed(grid, item -> item.getId().equals(schedule.getId())));
        assertThat(action(grid, "makeDefault").isEnabled()).isTrue();

        WorkScheduleDetailView editor = openFromList(grid, "edit");
        assertThat(editor.getEditedEntity().getId()).isEqualTo(schedule.getId());
        DataGrid<WorkScheduleAssignment> assignmentsGrid = UiTestUtils.getComponent(editor, "assignmentsDataGrid");

        WorkScheduleUsersAddView addDialog = performForDialog(assignmentsGrid, "add");
        JmixMultiSelectComboBoxPicker<User> usersField = UiTestUtils.getComponent(addDialog, "usersField");
        Action lookup = Objects.requireNonNull(usersField.getAction("entityLookup"), "the users lookup");
        lookup.actionPerform(usersField);
        UserListView usersLookup = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the user list");
        usersLookup.close(StandardOutcome.CLOSE);
        addDialog.close(StandardOutcome.CLOSE);

        assignmentsGrid.select(listed(assignmentsGrid,
                assignment -> assignment.getUser().getId().equals(assignedUser.getId())));
        WorkScheduleAssignmentDetailView assignmentEditor = performForDialog(assignmentsGrid, "edit");
        assertThat(assignmentEditor.isReadOnly()).isFalse();
        assignmentEditor.closeWithDiscard();
        editor.closeWithDiscard();

        grid.deselectAll();
        grid.select(listed(grid, item -> item.getId().equals(seedDefault.getId())));
        DefaultWorkScheduleDeleteView deleteDialog = performForDialog(grid, "remove");
        deleteDialog.close(StandardOutcome.CLOSE);
    }

    private void checkHolidayCalendarScreens(HolidayCalendar calendar) {
        viewBuilders.view(UiTestUtils.getCurrentView(), HolidayCalendarListView.class).open();
        HolidayCalendarListView list = UiTestUtils.getCurrentView();
        DataGrid<HolidayCalendar> grid = UiTestUtils.getComponent(list, "holidayCalendarsDataGrid");

        HolidayCalendarDetailView created = openFromList(grid, "create");
        created.closeWithDiscard();

        grid.select(listed(grid, item -> item.getId().equals(calendar.getId())));
        HolidayCalendarDetailView editor = openFromList(grid, "edit");
        assertThat(editor.getEditedEntity().getId()).isEqualTo(calendar.getId());
        DataGrid<HolidayCalendarDay> daysGrid = UiTestUtils.getComponent(editor, "daysDataGrid");

        HolidayCalendarDaysAddView daysDialog = performForDialog(daysGrid, "create");
        daysDialog.close(StandardOutcome.CLOSE);

        daysGrid.select(listed(daysGrid, day -> JAN_1.equals(day.getDate())));
        HolidayCalendarDayDetailView dayEditor = performForDialog(daysGrid, "edit");
        assertThat(dayEditor.isReadOnly()).isFalse();
        dayEditor.closeWithDiscard();
        editor.closeWithDiscard();
    }

    /**
     * Performs a list action that opens a detail view in place of the list, and checks that the view is editable.
     */
    private static <E, V extends StandardDetailView<E>> V openFromList(DataGrid<E> grid, String actionId) {
        Action action = action(grid, actionId);
        assertThat(action.isEnabled()).as("action %s", actionId).isTrue();
        action.actionPerform(grid);

        V detail = UiTestUtils.getCurrentView();
        assertThat(detail.isReadOnly()).isFalse();
        return detail;
    }

    private static <E, V extends View<?>> V performForDialog(DataGrid<E> grid, String actionId) {
        Action action = action(grid, actionId);
        assertThat(action.isEnabled()).as("action %s", actionId).isTrue();
        action.actionPerform(grid);

        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the dialog of action " + actionId);
    }

    private UserListView openUserList() {
        viewBuilders.view(UiTestUtils.getCurrentView(), UserListView.class).open();
        return UiTestUtils.getCurrentView();
    }

    private UserDetailView openUserEditor(User user) {
        UserListView list = openUserList();
        DataGrid<User> grid = UiTestUtils.getComponent(list, "usersDataGrid");
        grid.select(listed(grid, item -> item.getId().equals(user.getId())));
        Action edit = action(grid, "edit");
        assertThat(edit.isEnabled()).isTrue();
        edit.actionPerform(grid);

        return UiTestUtils.getCurrentView();
    }

    private static void assertSecurityActionsVisible(UserListView list, boolean visible) {
        DataGrid<User> grid = UiTestUtils.getComponent(list, "usersDataGrid");

        assertThat(action(grid, "showRoleAssignments").isVisible()).as("showRoleAssignments").isEqualTo(visible);
        assertThat(UiTestUtils.<JmixButton>getComponent(list, "showRoleAssignmentsBtn").isVisible())
                .as("showRoleAssignmentsBtn").isEqualTo(visible);

        DropdownButton additionalBtn = UiTestUtils.getComponent(list, "additionalBtn");
        DROPDOWN_ITEMS_BY_ACTION.forEach((actionId, itemId) -> {
            assertThat(action(grid, actionId).isVisible()).as(actionId).isEqualTo(visible);
            assertThat(Objects.requireNonNull(additionalBtn.getItem(itemId), itemId).isVisible())
                    .as(itemId).isEqualTo(visible);
        });
        // With all its items hidden, the dropdown is hidden too, not left as an empty menu
        assertThat(additionalBtn.isVisible()).as("additionalBtn").isEqualTo(visible);
    }

    private static <E> Action action(DataGrid<E> grid, String id) {
        return Objects.requireNonNull(grid.getAction(id), "action " + id);
    }

    private static <E> E listed(DataGrid<E> grid, Predicate<E> condition) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(condition)
                .findFirst()
                .orElseThrow(() -> new AssertionError("No such item in " + grid.getId().orElse("the grid")));
    }

    private static void clickOk(View<?> view) {
        UiTestUtils.<JmixButton>getComponent(view, "saveAndCloseBtn").click();
    }

    private User saveUser(String firstName) {
        User user = dataManager.create(User.class);
        user.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        user.setFirstName(firstName);
        user.setLastName("Tester");
        user.setEmail(firstName.toLowerCase() + "@example.com");
        return dataManager.save(user);
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        roleAssignmentIds.add(Id.of(dataManager.save(assignment)));
    }

    private WorkSchedule saveSchedule() {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(NAME_PREFIX + UUID.randomUUID());
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        WorkSchedule saved = dataManager.save(schedule);
        scheduleIds.add(Id.of(saved));
        return saved;
    }

    private void saveAssignment(User user, WorkSchedule schedule) {
        WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
        assignment.setUser(user);
        assignment.setWorkSchedule(schedule);
        assignment.setStartDate(NOV_1);
        dataManager.saveWithoutReload(assignment);
    }

    private HolidayCalendar saveCalendarWithJan1() {
        HolidayCalendar calendar = dataManager.create(HolidayCalendar.class);
        calendar.setName(NAME_PREFIX + UUID.randomUUID());
        HolidayCalendar saved = dataManager.save(calendar);
        calendarIds.add(Id.of(saved));

        HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
        day.setCalendar(saved);
        day.setDate(JAN_1);
        day.setType(HolidayCalendarDayType.DAY_OFF);
        day.setName("Holiday");
        dataManager.saveWithoutReload(day);
        return saved;
    }

    private void hardRemoveIfPresent(Id<?> id) {
        dataManager.load(id)
                .hint(PersistenceHints.SOFT_DELETION, false)
                .optional()
                .ifPresent(this::hardRemove);
    }

    private void hardRemove(Object... entities) {
        dataManager.save(new SaveContext()
                .removing(entities)
                .setHint(PersistenceHints.SOFT_DELETION, false));
    }
}
