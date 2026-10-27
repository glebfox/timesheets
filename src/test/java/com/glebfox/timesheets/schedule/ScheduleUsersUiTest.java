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

package com.glebfox.timesheets.schedule;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.action.WorkScheduleAssignmentEditAction;
import com.glebfox.timesheets.action.WorkScheduleAssignmentRemoveAction;
import com.glebfox.timesheets.action.WorkScheduleUsersAddAction;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.user.UserListView;
import com.glebfox.timesheets.view.workschedule.WorkScheduleDetailView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleAssignmentDetailView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleUsersAddView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.core.SaveContext;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.multiselectcomboboxpicker.JmixMultiSelectComboBoxPicker;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.data.grid.DataGridItems;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class ScheduleUsersUiTest {

    static final String USERNAME_PREFIX = "schedule-users-test-";
    static final String NAME_PREFIX = "Schedule users test ";

    static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);
    static final LocalDate JAN_1 = LocalDate.of(2027, 1, 1);
    static final LocalDate FEB_1 = LocalDate.of(2027, 2, 1);
    static final Locale RUSSIAN = Locale.of("ru", "RU");

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    MetadataTools metadataTools;
    @Autowired
    DialogWindows dialogWindows;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    Messages messages;

    List<Id<User>> userIds = new ArrayList<>();
    List<Id<WorkSchedule>> scheduleIds = new ArrayList<>();

    @Test
    void addIsEnabledOnAnEmptyTable() {
        WorkScheduleDetailView editor = openNewSchedule();
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        assertThat(listed(grid)).isEmpty();
        assertThat(action(grid, "add")).isInstanceOf(WorkScheduleUsersAddAction.class);
        assertThat(action(grid, "edit")).isInstanceOf(WorkScheduleAssignmentEditAction.class);
        assertThat(action(grid, "remove")).isInstanceOf(WorkScheduleAssignmentRemoveAction.class);
        assertThat(action(grid, "add").getText()).isEqualTo("Add");

        assertThat(action(grid, "add").isEnabled()).isTrue();
        assertThat(action(grid, "edit").isEnabled()).isFalse();
        assertThat(action(grid, "remove").isEnabled()).isFalse();
    }

    @Test
    void addUsersCreatesOneAssignmentPerUser() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        WorkSchedule schedule = saveSchedule();
        WorkScheduleDetailView editor = openSchedule(schedule);

        WorkScheduleUsersAddView dialog = add(editor);
        assertThat(UiTestUtils.<Span>getComponent(dialog, "workScheduleText").getText())
                .isEqualTo("Schedule: " + schedule.getName());
        usersField(dialog).setTypedValue(List.of(a, b));
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(listed(assignmentsDataGrid(editor)))
                .extracting(assignment -> assignment.getUser().getId(), WorkScheduleAssignment::getStartDate,
                        assignment -> assignment.getWorkSchedule().getId())
                .containsExactlyInAnyOrder(
                        tuple(a.getId(), DEC_1, schedule.getId()),
                        tuple(b.getId(), DEC_1, schedule.getId()));
        // Nothing is saved before the schedule editor's OK
        assertThat(loadAssignments(schedule)).isEmpty();

        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        assertThat(loadAssignments(schedule))
                .extracting(assignment -> assignment.getUser().getId(), WorkScheduleAssignment::getStartDate)
                .containsExactlyInAnyOrder(tuple(a.getId(), DEC_1), tuple(b.getId(), DEC_1));
    }

    @Test
    void newScheduleGetsAssignmentsOnFirstSave() {
        User a = saveUser("Ann");
        WorkScheduleDetailView editor = openNewSchedule();

        WorkScheduleUsersAddView dialog = add(editor);
        // The schedule has no name to show yet
        assertThat(UiTestUtils.<Span>getComponent(dialog, "workScheduleText").isVisible()).isFalse();
        usersField(dialog).setTypedValue(List.of(a));
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);

        String name = uniqueName();
        UiTestUtils.<TypedTextField<String>>getComponent(editor, "nameField").setValue(name);
        UiTestUtils.<TypedTextField<WorkTime>>getComponent(editor, "workdayTimeField")
                .setTypedValue(WorkTime.ofHours(8));
        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        WorkSchedule saved = dataManager.load(WorkSchedule.class)
                .query("e.name = :name")
                .parameter("name", name)
                .one();
        scheduleIds.add(Id.of(saved));
        assertThat(loadAssignments(saved))
                .extracting(assignment -> assignment.getUser().getId(), WorkScheduleAssignment::getStartDate)
                .containsExactly(tuple(a.getId(), DEC_1));
    }

    @Test
    void usersAndStartDateAreRequired() {
        WorkScheduleDetailView editor = openSchedule(saveSchedule());

        WorkScheduleUsersAddView dialog = add(editor);
        clickOk(dialog);

        // Worded as the framework words the required message of a bound field
        assertThat(usersField(dialog).isInvalid()).isTrue();
        assertThat(usersField(dialog).getErrorMessage()).isEqualTo("Users required");
        assertThat(startDateField(dialog).isInvalid()).isTrue();
        assertThat(startDateField(dialog).getErrorMessage()).isEqualTo("Start date required");
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(listed(assignmentsDataGrid(editor))).isEmpty();
    }

    @Test
    void russianRequiredMessagesNameTheFields() {
        WorkSchedule schedule = saveSchedule();

        systemAuthenticator.runWithUser("admin", () -> {
            ((AbstractAuthenticationToken) Objects.requireNonNull(SecurityContextHelper.getAuthentication()))
                    .setDetails(ClientDetails.builder().locale(RUSSIAN).build());
            WorkScheduleUsersAddView dialog = add(openSchedule(schedule));
            clickOk(dialog);

            assertThat(usersField(dialog).getErrorMessage()).isEqualTo("Заполните поле \"Пользователи\"");
            assertThat(startDateField(dialog).getErrorMessage()).isEqualTo("Заполните поле \"Дата начала\"");
        });
    }

    @Test
    void usersLookupAddsTheSelectedUsers() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        WorkScheduleDetailView editor = openSchedule(saveSchedule());
        WorkScheduleUsersAddView dialog = add(editor);

        Action lookup = Objects.requireNonNull(usersField(dialog).getAction("entityLookup"), "the lookup action");
        assertThat(lookup.isEnabled()).isTrue();
        lookup.actionPerform(usersField(dialog));

        UserListView userList = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the user list");
        DataGrid<User> usersDataGrid = UiTestUtils.getComponent(userList, "usersDataGrid");
        assertThat(usersDataGrid.isMultiSelect()).isTrue();
        Objects.requireNonNull(usersDataGrid.getItems()).getItems().stream()
                .filter(user -> user.getId().equals(a.getId()) || user.getId().equals(b.getId()))
                .forEach(usersDataGrid::select);
        assertThat(usersDataGrid.getSelectedItems()).hasSize(2);
        UiTestUtils.<JmixButton>getComponent(userList, "selectBtn").click();

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(userList);
        assertThat(usersField(dialog).getTypedValue())
                .extracting(User::getId)
                .containsExactlyInAnyOrder(a.getId(), b.getId());
    }

    @Test
    void dialogKeepsACopyOfTheAssignments() {
        User a = saveUser("Ann");
        WorkSchedule schedule = saveSchedule();
        WorkScheduleUsersAddView dialog = add(openSchedule(schedule));
        // Added in the editor and not saved yet
        WorkScheduleAssignment added = dataManager.create(WorkScheduleAssignment.class);
        added.setUser(a);
        added.setWorkSchedule(schedule);
        added.setStartDate(DEC_1);
        List<WorkScheduleAssignment> assignments = new ArrayList<>(List.of(added));
        dialog.setEditorAssignments(assignments, List.of());

        // A change of the given collection after the dialog gets it does not reach the dialog
        assignments.clear();
        usersField(dialog).setTypedValue(List.of(a));
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);

        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(usersField(dialog).getErrorMessage())
                .isEqualTo("Already assigned from this date: " + metadataTools.getInstanceName(a));
    }

    @Test
    void dialogKeepsACopyOfTheRemovedIds() {
        User a = saveUser("Ann");
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment saved = saveAssignment(a, schedule, DEC_1);
        WorkScheduleUsersAddView dialog = add(openSchedule(schedule));
        List<UUID> removedIds = new ArrayList<>(List.of(saved.getId()));
        dialog.setEditorAssignments(List.of(), removedIds);

        // A change of the given collection after the dialog gets it does not reach the dialog
        removedIds.clear();
        usersField(dialog).setTypedValue(List.of(a));
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);

        // The saved row from that date counts as removed in the editor, so it is not a duplicate
        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
    }

    @Test
    void removalIsKnownOnlyFromTheGivenIds() {
        User a = saveUser("Ann");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(a, schedule, DEC_1);
        WorkScheduleDetailView editor = openSchedule(schedule);
        remove(editor, a);

        WorkScheduleUsersAddView dialog = add(editor);
        // Removed in the editor's data context, but its id is not handed to the dialog
        dialog.setEditorAssignments(List.of(), List.of());
        usersField(dialog).setTypedValue(List.of(a));
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);

        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(usersField(dialog).getErrorMessage())
                .isEqualTo("Already assigned from this date: " + metadataTools.getInstanceName(a));
    }

    @Test
    void dialogRefusesNoAssignmentsOrRemovedIds() {
        WorkScheduleUsersAddView dialog =
                dialogWindows.view(UiTestUtils.getCurrentView(), WorkScheduleUsersAddView.class).build().getView();

        assertThatThrownBy(() -> dialog.setEditorAssignments(null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> dialog.setEditorAssignments(List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void addUsersRefusesUsersAssignedFromThatDate() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        User c = saveUser("Cid");
        WorkSchedule schedule = saveSchedule();
        WorkSchedule otherSchedule = saveSchedule();
        saveAssignment(a, otherSchedule, DEC_1);
        saveAssignment(b, otherSchedule, DEC_1);
        WorkScheduleDetailView editor = openSchedule(schedule);

        WorkScheduleUsersAddView dialog = add(editor);
        usersField(dialog).setTypedValue(List.of(c, b, a));
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);

        // Every such user is named, and nothing is added
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(usersField(dialog).isInvalid()).isTrue();
        assertThat(usersField(dialog).getErrorMessage()).isEqualTo("Already assigned from this date: %s, %s"
                .formatted(metadataTools.getInstanceName(a), metadataTools.getInstanceName(b)));
        assertThat(startDateField(dialog).isInvalid()).isTrue();
        assertThat(listed(assignmentsDataGrid(editor))).isEmpty();

        // Another date clears the refusal
        startDateField(dialog).setTypedValue(JAN_1);
        assertThat(usersField(dialog).isInvalid()).isFalse();
        assertThat(startDateField(dialog).isInvalid()).isFalse();
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(listed(assignmentsDataGrid(editor)))
                .extracting(assignment -> assignment.getUser().getId())
                .containsExactlyInAnyOrder(a.getId(), b.getId(), c.getId());
    }

    @Test
    void addUsersSeesUnsavedAssignments() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        WorkScheduleDetailView editor = openSchedule(saveSchedule());

        addUsers(editor, DEC_1, a);

        WorkScheduleUsersAddView dialog = add(editor);
        usersField(dialog).setTypedValue(List.of(a, b));
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);

        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(usersField(dialog).getErrorMessage())
                .isEqualTo("Already assigned from this date: " + metadataTools.getInstanceName(a));
        assertThat(listed(assignmentsDataGrid(editor))).singleElement()
                .extracting(assignment -> assignment.getUser().getId()).isEqualTo(a.getId());
    }

    @Test
    void addUsersSeesEditedAssignments() {
        User a = saveUser("Ann");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(a, schedule, DEC_1);
        WorkScheduleDetailView editor = openSchedule(schedule);

        WorkScheduleAssignmentDetailView dialog = edit(editor, a);
        UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(dialog, "startDateField").setTypedValue(JAN_1);
        clickOk(dialog);

        // The saved row from that date is held in this editor and starts later there, so it is not a duplicate
        addUsers(editor, DEC_1, a);

        assertThat(dates(assignmentsDataGrid(editor))).containsExactly(DEC_1, JAN_1);
    }

    @Test
    void removedAssignmentsDateCanBeReusedInTheSameEditor() {
        User a = saveUser("Ann");
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment removed = saveAssignment(a, schedule, DEC_1);
        WorkScheduleDetailView editor = openSchedule(schedule);

        remove(editor, a);
        addUsers(editor, DEC_1, a);

        // The saved row from that date is removed in this editor, so it is not a duplicate
        assertThat(listed(assignmentsDataGrid(editor))).singleElement().satisfies(assignment -> {
            assertThat(assignment.getStartDate()).isEqualTo(DEC_1);
            assertThat(assignment.getId()).isNotEqualTo(removed.getId());
        });

        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        assertThat(loadAssignments(schedule)).singleElement().satisfies(assignment -> {
            assertThat(assignment.getStartDate()).isEqualTo(DEC_1);
            assertThat(assignment.getId()).isNotEqualTo(removed.getId());
        });
        assertThat(dataManager.load(Id.of(removed))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .one()
                .getDeletedDate()).isNotNull();
    }

    @Test
    void cancelDiscardsAddedUsers() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment kept = saveAssignment(b, schedule, NOV_1);
        WorkScheduleDetailView editor = openSchedule(schedule);

        // Loading the assignments changes nothing, so an untouched editor closes without asking
        assertThat(ViewControllerUtils.getViewData(editor).getDataContext().hasChanges()).isFalse();

        addUsers(editor, DEC_1, a);
        assertThat(listed(assignmentsDataGrid(editor))).hasSize(2);

        UiTestUtils.<JmixButton>getComponent(editor, "closeBtn").click();
        DialogInfo saveConfirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a dialog about the unsaved changes");
        dialogButton(saveConfirmation, "close").click();

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        assertThat(loadAssignments(schedule)).extracting(WorkScheduleAssignment::getId)
                .containsExactly(kept.getId());
        assertThat(dataManager.load(WorkScheduleAssignment.class)
                .query("e.user.id = :userId")
                .parameter("userId", a.getId())
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list()).isEmpty();
    }

    @Test
    void changingTheScheduleMovesTheAssignmentOut() {
        User a = saveUser("Ann");
        WorkSchedule schedule = saveSchedule();
        WorkSchedule otherSchedule = saveSchedule();
        WorkScheduleAssignment moved = saveAssignment(a, schedule, DEC_1);
        WorkScheduleDetailView editor = openSchedule(schedule);

        WorkScheduleAssignmentDetailView dialog = edit(editor, a);
        EntityPicker<User> userField = UiTestUtils.getComponent(dialog, "userField");
        assertThat(userField.isVisible()).isTrue();
        assertThat(userField.isReadOnly()).isTrue();
        assertThat(Objects.requireNonNull(userField.getValue()).getId()).isEqualTo(a.getId());
        UiTestUtils.<EntityComboBox<WorkSchedule>>getComponent(dialog, "workScheduleField").setValue(otherSchedule);
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(listed(assignmentsDataGrid(editor))).isEmpty();
        // Nothing is saved before the schedule editor's OK
        assertThat(loadAssignments(schedule)).extracting(WorkScheduleAssignment::getId)
                .containsExactly(moved.getId());

        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        assertThat(loadAssignments(schedule)).isEmpty();
        assertThat(loadAssignments(otherSchedule)).singleElement().satisfies(assignment -> {
            assertThat(assignment.getId()).isEqualTo(moved.getId());
            assertThat(assignment.getStartDate()).isEqualTo(DEC_1);
        });
    }

    @Test
    void onlyExplicitAssignmentsAreListed() {
        User unassigned = saveUser("Ann");
        User assigned = saveUser("Bob");
        WorkSchedule defaultSchedule = dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .one();
        saveAssignment(assigned, defaultSchedule, DEC_1);

        WorkScheduleDetailView editor = openSchedule(defaultSchedule);

        List<UUID> listedUserIds = listed(assignmentsDataGrid(editor)).stream()
                .map(assignment -> assignment.getUser().getId())
                .toList();
        assertThat(listedUserIds).contains(assigned.getId()).doesNotContain(unassigned.getId());
    }

    @Test
    void assignmentsLoadNewestFirstFromTheQuery() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(a, schedule, NOV_1);
        saveAssignment(b, schedule, JAN_1);
        WorkScheduleDetailView editor = openSchedule(schedule);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        // The order is the query's own, not a sort of the table
        assertThat(grid.getSortOrder()).isEmpty();
        assertThat(dates(grid)).containsExactly(JAN_1, NOV_1);
    }

    @Test
    void addedUsersAreAddedAtTheTop() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        User c = saveUser("Cid");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(a, schedule, NOV_1);
        saveAssignment(b, schedule, JAN_1);
        WorkScheduleDetailView editor = openSchedule(schedule);

        // Older than the first assignment listed, and still first
        addUsers(editor, DEC_1, c);

        assertThat(dates(assignmentsDataGrid(editor))).containsExactly(DEC_1, JAN_1, NOV_1);
    }

    @Test
    void editedAssignmentStaysInPlace() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        User c = saveUser("Cid");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(a, schedule, NOV_1);
        saveAssignment(b, schedule, DEC_1);
        saveAssignment(c, schedule, JAN_1);
        WorkScheduleDetailView editor = openSchedule(schedule);

        WorkScheduleAssignmentDetailView dialog = edit(editor, a);
        UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(dialog, "startDateField").setTypedValue(FEB_1);
        clickOk(dialog);

        assertThat(dates(assignmentsDataGrid(editor))).containsExactly(JAN_1, DEC_1, FEB_1);
    }

    @Test
    void currentBadgeTakesOtherSchedulesIntoAccount() {
        LocalDate today = LocalDate.now();
        User current = saveUser("Ann");
        User movedOn = saveUser("Bob");
        User future = saveUser("Cid");
        WorkSchedule schedule = saveSchedule();
        WorkSchedule otherSchedule = saveSchedule();
        saveAssignment(current, schedule, today.minusMonths(1));
        saveAssignment(movedOn, schedule, today.minusMonths(2));
        // Already in force, so Bob's assignment to this schedule is not
        saveAssignment(movedOn, otherSchedule, today.minusMonths(1));
        saveAssignment(future, schedule, today.plusMonths(1));

        WorkScheduleDetailView editor = openSchedule(schedule);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        assertThat(badges(grid, listed(grid, current))).singleElement().satisfies(badge -> {
            assertThat(badge.getText()).isEqualTo("Current");
            assertThat(badge.hasThemeName(BadgeVariant.SUCCESS.getVariantName())).isTrue();
        });
        assertThat(badges(grid, listed(grid, movedOn))).isEmpty();
        assertThat(badges(grid, listed(grid, future))).isEmpty();
    }

    @Test
    void currentBadgeFollowsChangesInTheEditor() {
        LocalDate today = LocalDate.now();
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(a, schedule, today.minusMonths(2));
        WorkScheduleAssignment removed = saveAssignment(a, schedule, today.minusMonths(1));
        WorkScheduleAssignment postponed = saveAssignment(b, schedule, today.minusMonths(1));
        WorkScheduleDetailView editor = openSchedule(schedule);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        assertThat(badges(grid, listed(grid, removed))).hasSize(1);
        assertThat(badges(grid, listed(grid, postponed))).hasSize(1);

        // A removed assignment no longer counts, though its row is still saved
        grid.deselectAll();
        grid.select(listed(grid, removed));
        removeSelected(grid);
        assertThat(badges(grid, listed(grid, a))).hasSize(1);

        // An assignment as edited here counts, not as saved
        WorkScheduleAssignmentDetailView dialog = edit(editor, b);
        UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(dialog, "startDateField")
                .setTypedValue(today.plusMonths(1));
        clickOk(dialog);
        assertThat(badges(grid, listed(grid, b))).isEmpty();
    }

    @Test
    void editRedrawsEveryRow() {
        LocalDate today = LocalDate.now();
        User a = saveUser("Ann");
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment past = saveAssignment(a, schedule, today.minusMonths(1));
        WorkScheduleAssignment future = saveAssignment(a, schedule, today.plusMonths(1));
        WorkScheduleDetailView editor = openSchedule(schedule);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        List<DataGridItems.ItemSetChangeEvent<WorkScheduleAssignment>> redraws = new ArrayList<>();
        Objects.requireNonNull(grid.getItems()).addItemSetChangeListener(redraws::add);

        // The edited row takes the Current badge from the other one, which must be drawn again too
        WorkScheduleAssignmentDetailView dialog = edit(editor, listed(grid, future));
        UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(dialog, "startDateField").setTypedValue(today);
        clickOk(dialog);

        assertThat(badges(grid, listed(grid, future))).hasSize(1);
        assertThat(badges(grid, listed(grid, past))).isEmpty();
        assertThat(redraws).isNotEmpty();
    }

    @Test
    void userAndStartDateColumnsAreAutoWidth() {
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(openNewSchedule());

        for (String key : List.of("user", "startDate")) {
            Grid.Column<WorkScheduleAssignment> column = Objects.requireNonNull(grid.getColumnByKey(key));
            assertThat(column.isAutoWidth()).as(key).isTrue();
            assertThat(column.getWidth()).as(key).isNull();
        }
    }

    @Test
    void columnsAreMeasuredAgainWhenAssignmentsAreAddedOrEdited() {
        User a = saveUser("Ann");
        User b = saveUser("Bob");
        WorkSchedule schedule = saveSchedule();
        saveAssignment(a, schedule, NOV_1);
        WorkScheduleDetailView editor = openSchedule(schedule);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        // Drops what opening the editor queued for the browser
        browserCalls(grid);

        // An added assignment can have a longer user name or a badge the rows the table was sized to lack
        addUsers(editor, DEC_1, b);
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("recalculateColumnWidths"));

        // So can an edited one
        WorkScheduleAssignmentDetailView dialog = edit(editor, a);
        UiTestUtils.<TypedDatePicker<LocalDate>>getComponent(dialog, "startDateField").setTypedValue(JAN_1);
        browserCalls(grid);
        clickOk(dialog);
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("recalculateColumnWidths"));
    }

    @Test
    void cancelWithAChangeAsksAndDiscardClosesWithoutAdding() {
        User a = saveUser("Ann");
        WorkScheduleDetailView editor = openSchedule(saveSchedule());
        WorkScheduleUsersAddView dialog = add(editor);
        usersField(dialog).setTypedValue(List.of(a));

        UiTestUtils.<JmixButton>getComponent(dialog, "cancelBtn").click();

        DialogInfo question = unsavedChangesQuestion();
        assertThat(UiTestUtils.getOpenedViewDialogs()).contains(dialog);

        dialogButton(question, "yes").click();

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(listed(assignmentsDataGrid(editor))).isEmpty();
    }

    @Test
    void closeWithAChangeAsksAndCancelKeepsTheDialog() {
        WorkScheduleDetailView editor = openSchedule(saveSchedule());
        WorkScheduleUsersAddView dialog = add(editor);
        startDateField(dialog).setTypedValue(DEC_1);

        // How Esc and the dialog's close button close it
        dialog.closeWithDefaultAction();

        DialogInfo question = unsavedChangesQuestion();
        dialogButton(question, "no").click();

        assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(dialog);
        assertThat(startDateField(dialog).getTypedValue()).isEqualTo(DEC_1);
        assertThat(listed(assignmentsDataGrid(editor))).isEmpty();
    }

    @Test
    void unchangedDialogClosesWithoutAsking() {
        User a = saveUser("Ann");
        WorkScheduleDetailView editor = openSchedule(saveSchedule());

        WorkScheduleUsersAddView untouched = add(editor);
        UiTestUtils.<JmixButton>getComponent(untouched, "cancelBtn").click();

        assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(untouched);

        // Fields changed and changed back are as the dialog opened
        WorkScheduleUsersAddView restored = add(editor);
        usersField(restored).setTypedValue(List.of(a));
        startDateField(restored).setTypedValue(DEC_1);
        Action clear = Objects.requireNonNull(usersField(restored).getAction("clearLookup"), "the clear action");
        assertThat(clear.isEnabled()).isTrue();
        clear.actionPerform(usersField(restored));
        startDateField(restored).clear();
        restored.closeWithDefaultAction();

        assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(restored);
        assertThat(listed(assignmentsDataGrid(editor))).isEmpty();
    }

    @Test
    void okClosesWithoutAsking() {
        User a = saveUser("Ann");
        WorkScheduleDetailView editor = openSchedule(saveSchedule());
        WorkScheduleUsersAddView dialog = add(editor);
        usersField(dialog).setTypedValue(List.of(a));
        startDateField(dialog).setTypedValue(DEC_1);

        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(dates(assignmentsDataGrid(editor))).containsExactly(DEC_1);
    }

    @AfterEach
    void tearDown() {
        // Each test user with every assignment it has, those saved through the editor and those removed included,
        // before the user; then the schedules the assignments referred to
        for (Id<User> userId : userIds) {
            List<WorkScheduleAssignment> assignments = dataManager.load(WorkScheduleAssignment.class)
                    .query("e.user.id = :userId")
                    .parameter("userId", userId.getValue())
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list();
            if (!assignments.isEmpty()) {
                hardRemove(assignments.toArray());
            }
            dataManager.load(userId)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(this::hardRemove);
        }
        for (Id<WorkSchedule> scheduleId : scheduleIds) {
            dataManager.load(scheduleId)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(this::hardRemove);
        }
        userIds.clear();
        scheduleIds.clear();
    }

    private WorkScheduleDetailView openNewSchedule() {
        viewBuilders.detail(UiTestUtils.getCurrentView(), WorkSchedule.class).newEntity().open();
        return UiTestUtils.getCurrentView();
    }

    private WorkScheduleDetailView openSchedule(WorkSchedule schedule) {
        viewBuilders.detail(UiTestUtils.getCurrentView(), WorkSchedule.class).editEntity(schedule).open();
        return UiTestUtils.getCurrentView();
    }

    private static WorkScheduleUsersAddView add(WorkScheduleDetailView editor) {
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        Action add = action(grid, "add");

        assertThat(add.isEnabled()).isTrue();
        add.actionPerform(grid);

        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the add users dialog");
    }

    private static void addUsers(WorkScheduleDetailView editor, LocalDate startDate, User... users) {
        WorkScheduleUsersAddView dialog = add(editor);
        usersField(dialog).setTypedValue(List.of(users));
        startDateField(dialog).setTypedValue(startDate);
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).as("the add users dialog is closed").doesNotContain(dialog);
    }

    private static WorkScheduleAssignmentDetailView edit(WorkScheduleDetailView editor, User user) {
        return edit(editor, listed(assignmentsDataGrid(editor), user));
    }

    private static WorkScheduleAssignmentDetailView edit(WorkScheduleDetailView editor,
                                                         WorkScheduleAssignment assignment) {
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        Action edit = action(grid, "edit");

        grid.deselectAll();
        grid.select(assignment);
        assertThat(edit.isEnabled()).isTrue();
        edit.actionPerform(grid);

        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the assignment editor");
    }

    private static void remove(WorkScheduleDetailView editor, User user) {
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        grid.deselectAll();
        grid.select(listed(grid, user));
        removeSelected(grid);
    }

    private static void removeSelected(DataGrid<WorkScheduleAssignment> grid) {
        Action remove = action(grid, "remove");

        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);

        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(), "a remove confirmation");
        dialogButton(confirmation, "yes").click();
    }

    private static void clickOk(WorkScheduleUsersAddView dialog) {
        UiTestUtils.<JmixButton>getComponent(dialog, "okBtn").click();
    }

    private static void clickOk(View<?> view) {
        UiTestUtils.<JmixButton>getComponent(view, "saveAndCloseBtn").click();
    }

    private static DataGrid<WorkScheduleAssignment> assignmentsDataGrid(WorkScheduleDetailView editor) {
        return UiTestUtils.getComponent(editor, "assignmentsDataGrid");
    }

    private static Action action(DataGrid<WorkScheduleAssignment> grid, String id) {
        return Objects.requireNonNull(grid.getAction(id), "action " + id);
    }

    private static JmixMultiSelectComboBoxPicker<User> usersField(WorkScheduleUsersAddView dialog) {
        return UiTestUtils.getComponent(dialog, "usersField");
    }

    private static TypedDatePicker<LocalDate> startDateField(WorkScheduleUsersAddView dialog) {
        return UiTestUtils.getComponent(dialog, "startDateField");
    }

    private static List<WorkScheduleAssignment> listed(DataGrid<WorkScheduleAssignment> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream().toList();
    }

    private static WorkScheduleAssignment listed(DataGrid<WorkScheduleAssignment> grid, User user) {
        Optional<WorkScheduleAssignment> listed = listed(grid).stream()
                .filter(assignment -> user.getId().equals(assignment.getUser().getId()))
                .findFirst();
        assertThat(listed).as("an assignment of %s in the table", user.getUsername()).isPresent();
        return listed.get();
    }

    private static WorkScheduleAssignment listed(DataGrid<WorkScheduleAssignment> grid,
                                                 WorkScheduleAssignment assignment) {
        Optional<WorkScheduleAssignment> listed = listed(grid).stream()
                .filter(item -> assignment.getId().equals(item.getId()))
                .findFirst();
        assertThat(listed).as("the assignment from %s in the table", assignment.getStartDate()).isPresent();
        return listed.get();
    }

    private static List<LocalDate> dates(DataGrid<WorkScheduleAssignment> grid) {
        return listed(grid).stream()
                .map(WorkScheduleAssignment::getStartDate)
                .toList();
    }

    private static List<Badge> badges(DataGrid<WorkScheduleAssignment> grid, WorkScheduleAssignment assignment) {
        ComponentRenderer<?, WorkScheduleAssignment> renderer = (ComponentRenderer<?, WorkScheduleAssignment>)
                Objects.requireNonNull(grid.getColumnByKey("startDate")).getRenderer();
        Component cell = renderer.createComponent(assignment);
        return cell.getChildren()
                .filter(Badge.class::isInstance)
                .map(Badge.class::cast)
                .toList();
    }

    private DialogInfo unsavedChangesQuestion() {
        DialogInfo question = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a question about the unsaved changes");
        assertThat(question.getDialog().getHeaderTitle())
                .isEqualTo(messages.getMessage("dialogs.closeUnsaved.title"));
        return question;
    }

    private static List<String> browserCalls(DataGrid<WorkScheduleAssignment> grid) {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == grid.getElement().getNode())
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
    }

    private static Button dialogButton(DialogInfo dialog, String id) {
        return dialog.getButtons().stream()
                .filter(button -> id.equals(button.getId().orElse(null)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No '" + id + "' button in the dialog"));
    }

    private static String uniqueName() {
        return NAME_PREFIX + UUID.randomUUID();
    }

    /**
     * Loads the non-deleted assignments to a schedule with their users.
     */
    private List<WorkScheduleAssignment> loadAssignments(WorkSchedule schedule) {
        return dataManager.load(WorkScheduleAssignment.class)
                .query("e.workSchedule.id = :scheduleId")
                .parameter("scheduleId", schedule.getId())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.INSTANCE_NAME))
                .list();
    }

    private User saveUser(String firstName) {
        User user = dataManager.create(User.class);
        user.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        user.setFirstName(firstName);
        user.setLastName("Test");
        User saved = dataManager.save(user);
        userIds.add(Id.of(saved));
        return saved;
    }

    private WorkSchedule saveSchedule() {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(uniqueName());
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        WorkSchedule saved = dataManager.save(schedule);
        scheduleIds.add(Id.of(saved));
        return saved;
    }

    private WorkScheduleAssignment saveAssignment(User user, WorkSchedule schedule, LocalDate startDate) {
        WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
        assignment.setUser(user);
        assignment.setWorkSchedule(schedule);
        assignment.setStartDate(startDate);
        return dataManager.save(assignment);
    }

    private void hardRemove(Object... entities) {
        dataManager.save(new SaveContext()
                .removing(entities)
                .setHint(PersistenceHints.SOFT_DELETION, false));
    }
}
