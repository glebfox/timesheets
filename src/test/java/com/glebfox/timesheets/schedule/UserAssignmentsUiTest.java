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
import com.glebfox.timesheets.action.WorkScheduleAssignmentCreateAction;
import com.glebfox.timesheets.action.WorkScheduleAssignmentEditAction;
import com.glebfox.timesheets.action.WorkScheduleAssignmentRemoveAction;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.user.UserDetailView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleAssignmentDetailView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.badge.BadgeVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.textfield.JmixPasswordField;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class UserAssignmentsUiTest {

    static final String USERNAME_PREFIX = "user-assignments-test-";
    static final String NAME_PREFIX = "User assignments test ";

    static final LocalDate OCT_1 = LocalDate.of(2026, 10, 1);
    static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);
    static final LocalDate JAN_1 = LocalDate.of(2027, 1, 1);
    static final LocalDate FEB_1 = LocalDate.of(2027, 2, 1);

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    Messages messages;

    List<Id<WorkSchedule>> scheduleIds = new ArrayList<>();

    @Test
    void createIsEnabledOnAnEmptyTable() {
        UserDetailView editor = openUser(saveUser());
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        assertThat(listed(grid)).isEmpty();
        assertThat(grid.getEmptyStateText()).isEqualTo("No assignments");
        assertThat(action(grid, "create")).isInstanceOf(WorkScheduleAssignmentCreateAction.class);
        assertThat(action(grid, "edit")).isInstanceOf(WorkScheduleAssignmentEditAction.class);
        assertThat(action(grid, "remove")).isInstanceOf(WorkScheduleAssignmentRemoveAction.class);

        assertThat(action(grid, "create").isEnabled()).isTrue();
        assertThat(action(grid, "edit").isEnabled()).isFalse();
        assertThat(action(grid, "remove").isEnabled()).isFalse();
    }

    @Test
    void assignmentSavesWithTheUser() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        UserDetailView editor = openUser(user);

        WorkScheduleAssignmentDetailView dialog = create(editor);
        assertThat(UiTestUtils.<EntityPicker<User>>getComponent(dialog, "userField").isVisible()).isFalse();
        startDateField(dialog).setTypedValue(NOV_1);
        workScheduleField(dialog).setValue(schedule);
        clickOk(dialog);

        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(dates(assignmentsDataGrid(editor))).containsExactly(NOV_1);
        // Nothing is saved before the user editor's OK
        assertThat(loadAssignments(user)).isEmpty();

        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        assertThat(loadAssignments(user)).singleElement().satisfies(assignment -> {
            assertThat(assignment.getStartDate()).isEqualTo(NOV_1);
            assertThat(assignment.getWorkSchedule().getId()).isEqualTo(schedule.getId());
        });
    }

    @Test
    void cancelDiscardsAssignmentChanges() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        WorkSchedule otherSchedule = saveSchedule();
        WorkScheduleAssignment edited = saveAssignment(user, schedule, NOV_1);
        WorkScheduleAssignment removed = saveAssignment(user, schedule, DEC_1);
        UserDetailView editor = openUser(user);

        WorkScheduleAssignmentDetailView createDialog = create(editor);
        startDateField(createDialog).setTypedValue(JAN_1);
        workScheduleField(createDialog).setValue(schedule);
        clickOk(createDialog);

        WorkScheduleAssignmentDetailView editDialog = edit(editor, NOV_1);
        workScheduleField(editDialog).setValue(otherSchedule);
        clickOk(editDialog);

        remove(editor, DEC_1);

        assertThat(dates(assignmentsDataGrid(editor))).containsExactly(JAN_1, NOV_1);

        UiTestUtils.<JmixButton>getComponent(editor, "closeBtn").click();
        DialogInfo saveConfirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                "a dialog about the unsaved changes");
        dialogButton(saveConfirmation, "close").click();

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        assertThat(loadAssignments(user))
                .extracting(WorkScheduleAssignment::getId, WorkScheduleAssignment::getStartDate,
                        assignment -> assignment.getWorkSchedule().getId())
                .containsExactlyInAnyOrder(
                        tuple(edited.getId(), NOV_1, schedule.getId()),
                        tuple(removed.getId(), DEC_1, schedule.getId()));
    }

    @Test
    void newUserGetsAnAssignmentBeforeFirstSave() {
        WorkSchedule schedule = saveSchedule();
        viewBuilders.detail(UiTestUtils.getCurrentView(), User.class).newEntity().open();
        UserDetailView editor = UiTestUtils.getCurrentView();
        String username = uniqueUsername();
        UiTestUtils.<TypedTextField<String>>getComponent(editor, "usernameField").setValue(username);
        UiTestUtils.<JmixPasswordField>getComponent(editor, "passwordField").setValue("test-passwd");
        UiTestUtils.<JmixPasswordField>getComponent(editor, "confirmPasswordField").setValue("test-passwd");

        WorkScheduleAssignmentDetailView dialog = create(editor);
        startDateField(dialog).setTypedValue(NOV_1);
        workScheduleField(dialog).setValue(schedule);
        clickOk(dialog);
        assertThat(dates(assignmentsDataGrid(editor))).containsExactly(NOV_1);

        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        User saved = dataManager.load(User.class)
                .query("e.username = :username")
                .parameter("username", username)
                .one();
        assertThat(loadAssignments(saved)).singleElement().satisfies(assignment -> {
            assertThat(assignment.getStartDate()).isEqualTo(NOV_1);
            assertThat(assignment.getWorkSchedule().getId()).isEqualTo(schedule.getId());
        });
    }

    @Test
    void assignmentOfADeletedScheduleShowsAndSaves() {
        User user = saveUser();
        WorkSchedule deletedSchedule = saveSchedule();
        WorkScheduleAssignment saved = saveAssignment(user, deletedSchedule, NOV_1);
        dataManager.remove(dataManager.load(Id.of(deletedSchedule)).one());

        UserDetailView editor = openUser(user);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        WorkScheduleAssignment listedAssignment = listed(grid, NOV_1);
        assertThat(listedAssignment.getWorkSchedule()).isNotNull()
                .extracting(WorkSchedule::getName).isEqualTo(deletedSchedule.getName());
        assertThat(badges(grid, listedAssignment)).singleElement().satisfies(badge -> {
            assertThat(badge.getText()).isEqualTo("Deleted");
            assertThat(isNeutral(badge)).isTrue();
        });

        WorkScheduleAssignmentDetailView dialog = edit(editor, NOV_1);
        startDateField(dialog).setTypedValue(DEC_1);
        clickOk(dialog);
        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        WorkScheduleAssignment reloaded = dataManager.load(Id.of(saved))
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("workSchedule", FetchPlan.INSTANCE_NAME))
                .one();
        assertThat(reloaded.getStartDate()).isEqualTo(DEC_1);
        assertThat(reloaded.getWorkSchedule().getId()).isEqualTo(deletedSchedule.getId());
    }

    @Test
    void assignmentsAreSortedNewestFirst() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        saveAssignment(user, schedule, NOV_1);
        saveAssignment(user, schedule, JAN_1);
        saveAssignment(user, schedule, DEC_1);

        UserDetailView editor = openUser(user);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        assertThat(dates(grid)).containsExactly(JAN_1, DEC_1, NOV_1);

        // A created assignment takes its place by date, as does an edited one
        WorkScheduleAssignmentDetailView createDialog = create(editor);
        startDateField(createDialog).setTypedValue(OCT_1);
        workScheduleField(createDialog).setValue(schedule);
        clickOk(createDialog);

        assertThat(dates(grid)).containsExactly(JAN_1, DEC_1, NOV_1, OCT_1);

        WorkScheduleAssignmentDetailView editDialog = edit(editor, OCT_1);
        startDateField(editDialog).setTypedValue(FEB_1);
        clickOk(editDialog);

        assertThat(dates(grid)).containsExactly(FEB_1, JAN_1, DEC_1, NOV_1);
    }

    @Test
    void currentBadgeMarksTheAssignmentInForceToday() {
        LocalDate today = LocalDate.now();
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        saveAssignment(user, schedule, today.minusMonths(1));
        saveAssignment(user, schedule, today);
        saveAssignment(user, schedule, today.plusMonths(1));

        UserDetailView editor = openUser(user);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);

        assertThat(badges(grid, listed(grid, today))).singleElement().satisfies(badge -> {
            assertThat(badge.getText()).isEqualTo("Current");
            assertThat(badge.hasThemeName(BadgeVariant.SUCCESS.getVariantName())).isTrue();
        });
        assertThat(badges(grid, listed(grid, today.minusMonths(1)))).isEmpty();
        assertThat(badges(grid, listed(grid, today.plusMonths(1)))).isEmpty();
    }

    @Test
    void removeTakesEverySelectedAssignmentOnOk() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        saveAssignment(user, schedule, NOV_1);
        saveAssignment(user, schedule, DEC_1);
        WorkScheduleAssignment kept = saveAssignment(user, schedule, JAN_1);
        UserDetailView editor = openUser(user);
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        Action edit = action(grid, "edit");
        Action remove = action(grid, "remove");

        grid.select(listed(grid, NOV_1));
        assertThat(edit.isEnabled()).isTrue();
        assertThat(remove.isEnabled()).isTrue();
        grid.select(listed(grid, DEC_1));
        assertThat(grid.getSelectedItems()).hasSize(2);
        assertThat(edit.isEnabled()).isFalse();
        assertThat(remove.isEnabled()).isTrue();

        // The usual confirmation, and No keeps them
        remove.actionPerform(grid);
        DialogInfo declined = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(), "a remove confirmation");
        assertThat(declined.getDialog().getHeaderTitle()).isEqualTo(messages.getMessage("dialogs.Confirmation"));
        assertThat(declined.getContent().getElement().getTextRecursively())
                .isEqualTo(messages.getMessage("dialogs.Confirmation.Remove"));
        dialogButton(declined, "no").click();
        assertThat(dates(grid)).containsExactly(JAN_1, DEC_1, NOV_1);

        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);
        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(), "a remove confirmation");
        dialogButton(confirmation, "yes").click();

        assertThat(dates(grid)).containsExactly(JAN_1);
        // Nothing is removed until the user editor's OK
        assertThat(loadAssignments(user)).hasSize(3);

        clickOk(editor);

        assertThat(loadAssignments(user)).extracting(WorkScheduleAssignment::getId).containsExactly(kept.getId());
    }

    @Test
    void removedAssignmentsDateCanBeReusedInTheSameEditor() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment removed = saveAssignment(user, schedule, NOV_1);
        UserDetailView editor = openUser(user);

        remove(editor, NOV_1);
        WorkScheduleAssignmentDetailView dialog = create(editor);
        startDateField(dialog).setTypedValue(NOV_1);
        workScheduleField(dialog).setValue(schedule);
        clickOk(dialog);

        // The saved row from that date is removed in this editor, so it is not a duplicate
        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(dialog);
        assertThat(listed(assignmentsDataGrid(editor))).singleElement().satisfies(assignment -> {
            assertThat(assignment.getStartDate()).isEqualTo(NOV_1);
            assertThat(assignment.getId()).isNotEqualTo(removed.getId());
        });

        clickOk(editor);

        assertThat(UiTestUtils.<View<?>>getCurrentView()).isNotSameAs(editor);
        assertThat(loadAssignments(user)).singleElement().satisfies(assignment -> {
            assertThat(assignment.getStartDate()).isEqualTo(NOV_1);
            assertThat(assignment.getId()).isNotEqualTo(removed.getId());
        });
        assertThat(dataManager.load(Id.of(removed))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .one()
                .getDeletedDate()).isNotNull();
    }

    @AfterEach
    void tearDown() {
        // Each test user with every assignment it has, those saved through the editor and those removed included,
        // before the user; then the schedules the assignments referred to
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
        for (Id<WorkSchedule> id : scheduleIds) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(this::hardRemove);
        }
        scheduleIds.clear();
    }

    private UserDetailView openUser(User user) {
        viewBuilders.detail(UiTestUtils.getCurrentView(), User.class).editEntity(user).open();
        return UiTestUtils.getCurrentView();
    }

    private static WorkScheduleAssignmentDetailView create(UserDetailView editor) {
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        Action create = action(grid, "create");

        assertThat(create.isEnabled()).isTrue();
        create.actionPerform(grid);

        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the assignment editor");
    }

    private static WorkScheduleAssignmentDetailView edit(UserDetailView editor, LocalDate startDate) {
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        Action edit = action(grid, "edit");

        grid.deselectAll();
        grid.select(listed(grid, startDate));
        assertThat(edit.isEnabled()).isTrue();
        edit.actionPerform(grid);

        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the assignment editor");
    }

    private static void remove(UserDetailView editor, LocalDate startDate) {
        DataGrid<WorkScheduleAssignment> grid = assignmentsDataGrid(editor);
        Action remove = action(grid, "remove");

        grid.deselectAll();
        grid.select(listed(grid, startDate));
        assertThat(remove.isEnabled()).isTrue();
        remove.actionPerform(grid);

        DialogInfo confirmation = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(), "a remove confirmation");
        dialogButton(confirmation, "yes").click();
    }

    private static void clickOk(View<?> view) {
        UiTestUtils.<JmixButton>getComponent(view, "saveAndCloseBtn").click();
    }

    private static DataGrid<WorkScheduleAssignment> assignmentsDataGrid(UserDetailView editor) {
        return UiTestUtils.getComponent(editor, "assignmentsDataGrid");
    }

    private static Action action(DataGrid<WorkScheduleAssignment> grid, String id) {
        return Objects.requireNonNull(grid.getAction(id), "action " + id);
    }

    private static TypedDatePicker<LocalDate> startDateField(WorkScheduleAssignmentDetailView dialog) {
        return UiTestUtils.getComponent(dialog, "startDateField");
    }

    private static EntityComboBox<WorkSchedule> workScheduleField(WorkScheduleAssignmentDetailView dialog) {
        return UiTestUtils.getComponent(dialog, "workScheduleField");
    }

    private static List<WorkScheduleAssignment> listed(DataGrid<WorkScheduleAssignment> grid) {
        return Objects.requireNonNull(grid.getItems()).getItems().stream().toList();
    }

    private static WorkScheduleAssignment listed(DataGrid<WorkScheduleAssignment> grid, LocalDate startDate) {
        Optional<WorkScheduleAssignment> listed = listed(grid).stream()
                .filter(assignment -> startDate.equals(assignment.getStartDate()))
                .findFirst();
        assertThat(listed).as("an assignment from %s in the table", startDate).isPresent();
        return listed.get();
    }

    private static List<LocalDate> dates(DataGrid<WorkScheduleAssignment> grid) {
        return listed(grid).stream()
                .map(WorkScheduleAssignment::getStartDate)
                .toList();
    }

    private static List<Badge> badges(DataGrid<WorkScheduleAssignment> grid, WorkScheduleAssignment assignment) {
        ComponentRenderer<?, WorkScheduleAssignment> renderer = (ComponentRenderer<?, WorkScheduleAssignment>)
                Objects.requireNonNull(grid.getColumnByKey("workSchedule")).getRenderer();
        Component cell = renderer.createComponent(assignment);
        return cell.getChildren()
                .filter(Badge.class::isInstance)
                .map(Badge.class::cast)
                .toList();
    }

    // As BadgeStyle.NEUTRAL marks a badge
    private static boolean isNeutral(Badge badge) {
        return badge.hasClassName("aura-accent-neutral");
    }

    private static Button dialogButton(DialogInfo dialog, String id) {
        return dialog.getButtons().stream()
                .filter(button -> id.equals(button.getId().orElse(null)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No '" + id + "' button in the dialog"));
    }

    private static String uniqueUsername() {
        return USERNAME_PREFIX + UUID.randomUUID();
    }

    /**
     * Loads the non-deleted assignments of a user with their schedules, deleted schedules included.
     */
    private List<WorkScheduleAssignment> loadAssignments(User user) {
        return dataManager.load(WorkScheduleAssignment.class)
                .query("e.user.id = :userId")
                .parameter("userId", user.getId())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("workSchedule", FetchPlan.INSTANCE_NAME))
                .list();
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername(uniqueUsername());
        return dataManager.save(user);
    }

    private WorkSchedule saveSchedule() {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(NAME_PREFIX + UUID.randomUUID());
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
