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
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleAssignmentDetailView;
import com.google.common.collect.Lists;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.Query;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.DataComponents;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class WorkScheduleAssignmentEditorUiTest {

    static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    static final LocalDate NOV_15 = LocalDate.of(2026, 11, 15);
    static final LocalDate DEC_1 = LocalDate.of(2026, 12, 1);

    @Autowired
    DataManager dataManager;
    @Autowired
    DialogWindows dialogWindows;
    @Autowired
    DataComponents dataComponents;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void startDateInTheDatabaseIsRefused() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        saveAssignment(user, schedule, NOV_1);

        WorkScheduleAssignmentDetailView editor = openNew(user, schedule, newParentContext(), List.of());
        startDateField(editor).setTypedValue(NOV_1);
        clickOk(editor);

        assertRefused(editor);
    }

    @Test
    void startDateAmongSiblingsIsRefused() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        // Added in the editor and not saved yet
        WorkScheduleAssignment unsaved = newAssignment(user, schedule, DEC_1);

        WorkScheduleAssignmentDetailView editor = openNew(user, schedule, newParentContext(), List.of(unsaved));
        startDateField(editor).setTypedValue(DEC_1);
        clickOk(editor);

        assertRefused(editor);
    }

    @Test
    void siblingsOfOtherUsersDoNotCount() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment otherUsers = newAssignment(saveUser(), schedule, DEC_1);
        DataContext parentContext = newParentContext();

        WorkScheduleAssignmentDetailView editor = openNew(user, schedule, parentContext, List.of(otherUsers));
        startDateField(editor).setTypedValue(DEC_1);
        clickOk(editor);

        assertAccepted(editor, parentContext, DEC_1);
    }

    @Test
    void siblingWinsOverItsDatabaseRow() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment saved = saveAssignment(user, schedule, NOV_1);
        // The editor's copy of the saved row, moved to another date and not saved yet
        WorkScheduleAssignment moved = dataManager.load(Id.of(saved)).one();
        moved.setStartDate(NOV_15);
        DataContext parentContext = newParentContext();

        WorkScheduleAssignmentDetailView editor = openNew(user, schedule, parentContext, List.of(moved));
        startDateField(editor).setTypedValue(NOV_1);
        clickOk(editor);

        assertAccepted(editor, parentContext, NOV_1);
    }

    @Test
    void assignmentRemovedInTheEditorIsNotADuplicate() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment saved = saveAssignment(user, schedule, NOV_1);
        DataContext parentContext = newParentContext();

        // Removed in the editor: gone from its container, so not among the siblings, and not deleted until
        // the editor's OK
        WorkScheduleAssignmentDetailView editor =
                openNew(user, schedule, parentContext, List.of(), List.of(saved.getId()));
        startDateField(editor).setTypedValue(NOV_1);
        clickOk(editor);

        assertAccepted(editor, parentContext, NOV_1);
    }

    @Test
    void removalIsKnownOnlyFromTheGivenIds() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment saved = saveAssignment(user, schedule, NOV_1);
        // Removed in the parent data context, but its id is not handed to the editor
        DataContext parentContext = newParentContext();
        parentContext.remove(parentContext.merge(saved));

        WorkScheduleAssignmentDetailView editor = openNew(user, schedule, parentContext, List.of());
        startDateField(editor).setTypedValue(NOV_1);
        clickOk(editor);

        assertRefused(editor);
    }

    @Test
    void ownStartDateIsNotADuplicate() {
        User user = saveUser();
        WorkScheduleAssignment saved = saveAssignment(user, saveSchedule(), NOV_1);
        WorkSchedule otherSchedule = saveSchedule();
        DataContext parentContext = newParentContext();

        WorkScheduleAssignmentDetailView editor = openEdit(saved, parentContext, List.of(saved));
        workScheduleField(editor).setValue(otherSchedule);
        clickOk(editor);

        assertAccepted(editor, parentContext, NOV_1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void scheduleChoiceOffersOnlyNonDeletedSchedules() {
        WorkSchedule kept = saveSchedule();
        WorkSchedule deleted = saveSchedule();
        remove(deleted);

        WorkScheduleAssignmentDetailView editor = openNew(saveUser(), null, newParentContext(), List.of());
        DataProvider<WorkSchedule, String> dataProvider =
                (DataProvider<WorkSchedule, String>) workScheduleField(editor).getDataProvider();

        // The empty filter is what the field sends when its drop-down opens
        List<UUID> optionIds = dataProvider.fetch(new Query<>(""))
                .map(WorkSchedule::getId)
                .toList();
        assertThat(optionIds).contains(kept.getId()).doesNotContain(deleted.getId());
    }

    @Test
    void assignmentOfADeletedScheduleShowsIt() {
        WorkSchedule deletedSchedule = saveSchedule();
        WorkScheduleAssignment saved = saveAssignment(saveUser(), deletedSchedule, NOV_1);
        remove(deletedSchedule);

        WorkScheduleAssignmentDetailView editor = openEdit(saved, newParentContext(), List.of(saved));

        EntityComboBox<WorkSchedule> workScheduleField = workScheduleField(editor);
        assertThat(workScheduleField.getValue()).isNotNull()
                .extracting(WorkSchedule::getId).isEqualTo(deletedSchedule.getId());
        assertThat(workScheduleField.getHelperText()).isEqualTo("This schedule is deleted");

        // The field offers only non-deleted schedules, so another choice is not deleted
        workScheduleField.setValue(saveSchedule());
        assertThat(workScheduleField.getHelperText()).isNull();
    }

    @Test
    void editorKeepsTheSiblingsItWasGiven() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        List<WorkScheduleAssignment> siblings = new ArrayList<>(List.of(newAssignment(user, schedule, DEC_1)));

        WorkScheduleAssignmentDetailView editor = openNew(user, schedule, newParentContext(), siblings);
        // A change of the given collection after the editor opens does not reach it
        siblings.clear();
        startDateField(editor).setTypedValue(DEC_1);
        clickOk(editor);

        assertRefused(editor);
    }

    @Test
    void editorKeepsTheRemovedIdsItWasGiven() {
        User user = saveUser();
        WorkSchedule schedule = saveSchedule();
        WorkScheduleAssignment saved = saveAssignment(user, schedule, NOV_1);
        List<UUID> removedIds = new ArrayList<>(List.of(saved.getId()));
        DataContext parentContext = newParentContext();

        WorkScheduleAssignmentDetailView editor = openNew(user, schedule, parentContext, List.of(), removedIds);
        // A change of the given collection after the editor opens does not reach it
        removedIds.clear();
        startDateField(editor).setTypedValue(NOV_1);
        clickOk(editor);

        assertAccepted(editor, parentContext, NOV_1);
    }

    @Test
    void editorRefusesNoSiblingsOrRemovedIds() {
        WorkScheduleAssignmentDetailView editor =
                dialogWindows.detail(UiTestUtils.getCurrentView(), WorkScheduleAssignment.class)
                        .withViewClass(WorkScheduleAssignmentDetailView.class)
                        .newEntity()
                        .withParentDataContext(newParentContext())
                        .build()
                        .getView();

        assertThatThrownBy(() -> editor.setSiblingAssignments(null, List.of()))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> editor.setSiblingAssignments(List.of(), null))
                .isInstanceOf(NullPointerException.class);
    }

    @AfterEach
    void tearDown() {
        // Assignments before the users and schedules they refer to; each reloaded by id right before removal,
        // since the tests soft-delete some of what they created
        for (Id<?> id : Lists.reverse(createdIds)) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    /**
     * Opens the editor for a new assignment as the user and schedule editors do: as a dialog whose OK merges
     * into the editor's data context, with the editor's assignments handed to it and none removed there.
     */
    private WorkScheduleAssignmentDetailView openNew(User user,
                                                     @Nullable WorkSchedule schedule,
                                                     DataContext parentContext,
                                                     Collection<WorkScheduleAssignment> siblings) {
        return openNew(user, schedule, parentContext, siblings, List.of());
    }

    /**
     * Opens the editor for a new assignment as the user and schedule editors do, with the editor's assignments
     * and the ids of those removed there handed to it.
     */
    private WorkScheduleAssignmentDetailView openNew(User user,
                                                     @Nullable WorkSchedule schedule,
                                                     DataContext parentContext,
                                                     Collection<WorkScheduleAssignment> siblings,
                                                     Collection<UUID> removedIds) {
        dialogWindows.detail(UiTestUtils.getCurrentView(), WorkScheduleAssignment.class)
                .withViewClass(WorkScheduleAssignmentDetailView.class)
                .newEntity()
                .withInitializer(assignment -> {
                    assignment.setUser(user);
                    assignment.setWorkSchedule(schedule);
                })
                .withParentDataContext(parentContext)
                .withViewConfigurer(view -> view.setSiblingAssignments(siblings, removedIds))
                .open();
        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the assignment editor");
    }

    private WorkScheduleAssignmentDetailView openEdit(WorkScheduleAssignment assignment,
                                                      DataContext parentContext,
                                                      Collection<WorkScheduleAssignment> siblings) {
        dialogWindows.detail(UiTestUtils.getCurrentView(), WorkScheduleAssignment.class)
                .withViewClass(WorkScheduleAssignmentDetailView.class)
                .editEntity(assignment)
                .withParentDataContext(parentContext)
                .withViewConfigurer(view -> view.setSiblingAssignments(siblings, List.of()))
                .open();
        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the assignment editor");
    }

    private DataContext newParentContext() {
        return dataComponents.createDataContext();
    }

    private static void assertRefused(WorkScheduleAssignmentDetailView editor) {
        assertThat(startDateField(editor).isInvalid()).isTrue();
        assertThat(startDateField(editor).getErrorMessage()).isEqualTo("Already assigned from this date");
        assertThat(UiTestUtils.<View<?>>getLastOpenedViewDialog()).isSameAs(editor);
    }

    private static void assertAccepted(WorkScheduleAssignmentDetailView editor,
                                       DataContext parentContext,
                                       LocalDate startDate) {
        assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(editor);
        assertThat(parentContext.getModified()).anySatisfy(entity ->
                assertThat(entity).isInstanceOfSatisfying(WorkScheduleAssignment.class, assignment ->
                        assertThat(assignment.getStartDate()).isEqualTo(startDate)));
    }

    private static void clickOk(WorkScheduleAssignmentDetailView editor) {
        UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();
    }

    private static TypedDatePicker<LocalDate> startDateField(WorkScheduleAssignmentDetailView editor) {
        return UiTestUtils.getComponent(editor, "startDateField");
    }

    private static EntityComboBox<WorkSchedule> workScheduleField(WorkScheduleAssignmentDetailView editor) {
        return UiTestUtils.getComponent(editor, "workScheduleField");
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername("assignment-editor-test-" + UUID.randomUUID());
        return save(user);
    }

    private WorkSchedule saveSchedule() {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName("Assignment editor test " + UUID.randomUUID());
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        return save(schedule);
    }

    private WorkScheduleAssignment newAssignment(User user, WorkSchedule schedule, LocalDate startDate) {
        WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
        assignment.setUser(user);
        assignment.setWorkSchedule(schedule);
        assignment.setStartDate(startDate);
        return assignment;
    }

    private WorkScheduleAssignment saveAssignment(User user, WorkSchedule schedule, LocalDate startDate) {
        return save(newAssignment(user, schedule, startDate));
    }

    private <T> T save(T entity) {
        T saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void remove(Object entity) {
        dataManager.remove(dataManager.load(Id.of(entity)).one());
    }
}
