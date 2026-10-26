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

package com.glebfox.timesheets.view.workscheduleassignment;

import com.glebfox.timesheets.app.WorkScheduleAssignmentSupport;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.google.common.base.Preconditions;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.Messages;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.SupportsTypedValue;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.multiselectcomboboxpicker.JmixMultiSelectComboBoxPicker;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Assigns several users to a work schedule from one start date. Opened as a dialog from the schedule editor,
 * which hands it the editor's assignments. Refuses users who already have an assignment from that date;
 * otherwise closes with {@link StandardOutcome#SAVE}, and the opener adds one assignment per user
 * of {@link #getUsers()} from {@link #getStartDate()}.
 */
@ViewController(WorkScheduleUsersAddView.ID)
@ViewDescriptor("work-schedule-users-add-view.xml")
@DialogMode(width = "480px", closeOnEsc = true)
public class WorkScheduleUsersAddView extends StandardView {

    public static final String ID = "ts_WorkScheduleUsersAddView";

    @ViewComponent
    private JmixMultiSelectComboBoxPicker<User> usersField;
    @ViewComponent
    private TypedDatePicker<LocalDate> startDateField;
    @ViewComponent
    private Span workScheduleText;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private Messages messages;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ViewValidation viewValidation;
    @Autowired
    private WorkScheduleAssignmentSupport workScheduleAssignmentSupport;

    @Nullable
    private Supplier<Collection<WorkScheduleAssignment>> editorAssignments;
    private Predicate<WorkScheduleAssignment> removedInEditor = assignment -> false;
    private boolean usersRefused;

    /**
     * Sets the assignments to check the start date against: those of the opening editor. They may belong
     * to any users. Required: the view does not open without them.
     *
     * @param editorAssignments supplies the editor's assignments, saved or added there and not removed
     * @param removedInEditor   tells whether a saved assignment is removed in the editor, so its row no longer
     *                          counts
     */
    public void setEditorAssignments(Supplier<Collection<WorkScheduleAssignment>> editorAssignments,
                                     Predicate<WorkScheduleAssignment> removedInEditor) {
        this.editorAssignments = editorAssignments;
        this.removedInEditor = removedInEditor;
    }

    /**
     * Names the schedule that the users are assigned to.
     */
    public void setWorkSchedule(WorkSchedule workSchedule) {
        String name = metadataTools.getInstanceName(workSchedule);
        workScheduleText.setText(messageBundle.formatMessage("workScheduleText.text", name));
        // A new schedule may have no name yet
        workScheduleText.setVisible(!name.isBlank());
    }

    /**
     * Returns the users to assign.
     */
    public List<User> getUsers() {
        return List.copyOf(Objects.requireNonNull(usersField.getTypedValue()));
    }

    /**
     * Returns the date the assignments start from.
     */
    public LocalDate getStartDate() {
        return Objects.requireNonNull(startDateField.getTypedValue());
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        // The fields are not bound to an entity, so they get the required message that bound fields get
        usersField.setRequiredMessage(formatRequiredMessage(usersField.getLabel()));
        startDateField.setRequiredMessage(formatRequiredMessage(startDateField.getLabel()));
    }

    private String formatRequiredMessage(String label) {
        return messages.formatMessage("", "validation.required.defaultMsg", label);
    }

    @Subscribe
    public void onBeforeShow(final BeforeShowEvent event) {
        // Without the editor's assignments the check would miss the ones added or removed there
        Preconditions.checkState(editorAssignments != null,
                "setEditorAssignments() must be called before the view opens");
    }

    @Subscribe("usersField")
    public void onUsersFieldTypedValueChange(
            final SupportsTypedValue.TypedValueChangeEvent<JmixMultiSelectComboBoxPicker<User>, Collection<User>> event) {
        clearRefusedUsers();
    }

    @Subscribe("startDateField")
    public void onStartDateFieldTypedValueChange(
            final SupportsTypedValue.TypedValueChangeEvent<TypedDatePicker<LocalDate>, LocalDate> event) {
        clearRefusedUsers();
    }

    @Subscribe("okAction")
    public void onOkAction(final ActionPerformedEvent event) {
        clearRefusedUsers();

        ValidationErrors errors = viewValidation.validateUiComponents(getContent());
        if (errors.isEmpty()) {
            errors = validateUsers();
        }
        if (!errors.isEmpty()) {
            viewValidation.showValidationErrors(errors);
            viewValidation.focusProblemComponent(errors);
            return;
        }

        close(StandardOutcome.SAVE);
    }

    private ValidationErrors validateUsers() {
        ValidationErrors errors = new ValidationErrors();

        List<User> assigned = findAssignedUsers(getUsers(), getStartDate());
        if (!assigned.isEmpty()) {
            String users = assigned.stream()
                    .map(metadataTools::getInstanceName)
                    .sorted()
                    .collect(Collectors.joining(", "));
            String message = messageBundle.formatMessage("usersField.duplicateError", users);
            // Marked before the message is set: marking runs the field's validators, which clear the message
            // of a field that is invalid already
            usersField.setInvalid(true);
            usersField.setErrorMessage(message);
            // Both fields are marked, as the pair of user and date is taken; the users are listed once
            startDateField.setInvalid(true);
            errors.add(usersField, message);
            usersRefused = true;
        }
        return errors;
    }

    private List<User> findAssignedUsers(List<User> users, LocalDate startDate) {
        Collection<WorkScheduleAssignment> held = Objects.requireNonNull(editorAssignments).get();
        Set<UUID> assignedUserIds = held.stream()
                .filter(assignment -> startDate.equals(assignment.getStartDate()) && assignment.getUser() != null)
                .map(assignment -> assignment.getUser().getId())
                .collect(Collectors.toCollection(HashSet::new));

        // A saved row counts only when the editor does not hold it: a held assignment stands for its row
        // as edited there, and a removed one is gone
        Set<UUID> heldIds = held.stream()
                .map(WorkScheduleAssignment::getId)
                .collect(Collectors.toSet());
        workScheduleAssignmentSupport.loadAssignmentsFrom(users, startDate).stream()
                .filter(saved -> !heldIds.contains(saved.getId()) && !removedInEditor.test(saved))
                .forEach(saved -> assignedUserIds.add(saved.getUser().getId()));

        return users.stream()
                .filter(user -> assignedUserIds.contains(user.getId()))
                .toList();
    }

    private void clearRefusedUsers() {
        if (usersRefused) {
            usersRefused = false;
            // Runs the fields' own validation again, which clears the refusal
            usersField.setInvalid(false);
            startDateField.setInvalid(false);
        }
    }
}
