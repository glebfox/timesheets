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
import com.glebfox.timesheets.app.util.UnsavedInputSupport;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.SupportsTypedValue;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.multiselectcomboboxpicker.JmixMultiSelectComboBoxPicker;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Assigns several users to a work schedule from one start date. Opened as a dialog from the schedule editor,
 * which hands it the editor's assignments as they stand when it opens. Refuses users who already have
 * an assignment from that date; otherwise closes with {@link StandardOutcome#SAVE}, and the opener adds one
 * assignment per user of {@link #getUsers()} from {@link #getStartDate()}. Closed any other way while a field
 * differs from how it opened, it asks whether to discard the input: Discard closes it, Cancel keeps it open.
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
    private MetadataTools metadataTools;
    @Autowired
    private ViewValidation viewValidation;
    @Autowired
    private WorkScheduleAssignmentSupport workScheduleAssignmentSupport;
    @Autowired
    private UnsavedInputSupport unsavedInputSupport;

    private Collection<WorkScheduleAssignment> editorAssignments = List.of();
    private Set<UUID> removedIds = Set.of();
    private List<Object> openedValues = List.of();
    private boolean usersRefused;

    /**
     * Sets the assignments to check the start date against: those of the opening editor as they stand when
     * the view opens. They may belong to any users. The view keeps copies, so a later change of the given
     * collections does not reach it.
     *
     * @param editorAssignments the editor's assignments, saved or added there and not removed, possibly none
     * @param removedIds        the ids of the saved assignments removed in the editor, whose rows no longer count,
     *                          possibly none
     */
    public void setEditorAssignments(Collection<WorkScheduleAssignment> editorAssignments,
                                     Collection<UUID> removedIds) {
        this.editorAssignments = List.copyOf(Objects.requireNonNull(editorAssignments));
        this.removedIds = Set.copyOf(Objects.requireNonNull(removedIds));
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
    public void onReady(final ReadyEvent event) {
        openedValues = getFieldValues();
    }

    @Subscribe
    public void onBeforeClose(final BeforeCloseEvent event) {
        // OK takes the input; Cancel, Esc, and the close button ask before they discard a change
        unsavedInputSupport.askBeforeDiscarding(event, !getFieldValues().equals(openedValues));
    }

    private List<Object> getFieldValues() {
        // The users as a set, and no users as an empty one, whether the field was never filled or was emptied since
        Collection<User> users = usersField.getTypedValue();
        return Arrays.asList(users == null ? Set.of() : Set.copyOf(users), startDateField.getValue());
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
        Set<UUID> assignedUserIds = editorAssignments.stream()
                .filter(assignment -> startDate.equals(assignment.getStartDate()) && assignment.getUser() != null)
                .map(assignment -> assignment.getUser().getId())
                .collect(Collectors.toCollection(HashSet::new));

        // A saved row counts only when the editor does not hold it: a held assignment stands for its row
        // as edited there, and a removed one is gone
        Set<UUID> heldIds = editorAssignments.stream()
                .map(WorkScheduleAssignment::getId)
                .collect(Collectors.toSet());
        workScheduleAssignmentSupport.loadAssignmentsFrom(users, startDate).stream()
                .filter(saved -> !heldIds.contains(saved.getId()) && !removedIds.contains(saved.getId()))
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
