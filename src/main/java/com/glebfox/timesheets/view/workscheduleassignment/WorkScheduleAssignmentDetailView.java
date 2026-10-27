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
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.valuepicker.EntityPicker;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.model.InstanceContainer;
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
import java.util.stream.Collectors;

/**
 * Edits one work schedule assignment. Opened as a dialog from the user editor and the schedule editor
 * with the editor's data context as the parent, so its OK only merges the assignment into the editor.
 * The opener hands it the editor's assignments and the ids of those removed there, so a start date the user
 * already has is refused, and the date of a removed one is not.
 */
@ViewController("ts_WorkScheduleAssignment.detail")
@ViewDescriptor("work-schedule-assignment-detail-view.xml")
@EditedEntityContainer("workScheduleAssignmentDc")
@DialogMode(width = "480px", closeOnEsc = true)
public class WorkScheduleAssignmentDetailView extends StandardDetailView<WorkScheduleAssignment> {

    @ViewComponent
    private EntityPicker<User> userField;
    @ViewComponent
    private EntityComboBox<WorkSchedule> workScheduleField;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private WorkScheduleAssignmentSupport workScheduleAssignmentSupport;

    private Collection<WorkScheduleAssignment> siblingAssignments = List.of();
    private Set<UUID> removedIds = Set.of();

    /**
     * Shows or hides the user field, which is read-only either way.
     */
    public void setUserFieldVisible(boolean visible) {
        userField.setVisible(visible);
    }

    /**
     * Sets what the edited assignment's start date is checked against: the assignments of the opening editor
     * as they stand when the view opens, saved or added there and not removed, and the ids of the saved
     * assignments removed there, whose dates are free again. The assignments may belong to several users,
     * and the edited assignment itself may be among them. The view keeps copies, so a later change of the given
     * collections does not reach it.
     *
     * @param siblings   the editor's assignments, possibly none
     * @param removedIds the ids of the assignments removed in the editor, possibly none
     */
    public void setSiblingAssignments(Collection<WorkScheduleAssignment> siblings, Collection<UUID> removedIds) {
        this.siblingAssignments = List.copyOf(Objects.requireNonNull(siblings));
        this.removedIds = Set.copyOf(Objects.requireNonNull(removedIds));
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        WorkSchedule workSchedule = getEditedEntity().getWorkSchedule();
        if (workSchedule != null && workSchedule.getDeletedDate() != null) {
            workScheduleField.setHelperText(messageBundle.getMessage("workScheduleField.deletedHelperText"));
        }
    }

    @Subscribe(id = "workScheduleAssignmentDc", target = Target.DATA_CONTAINER)
    public void onWorkScheduleAssignmentDcItemPropertyChange(
            final InstanceContainer.ItemPropertyChangeEvent<WorkScheduleAssignment> event) {
        if ("workSchedule".equals(event.getProperty())) {
            // The field offers only non-deleted schedules, so the newly chosen one is not deleted
            workScheduleField.setHelperText(null);
        }
    }

    @Install(to = "startDateField", subject = "validator")
    private void startDateFieldValidator(final @Nullable LocalDate value) {
        if (value != null && isStartDateTaken(value)) {
            throw new ValidationException(messageBundle.getMessage("startDateField.duplicateError"));
        }
    }

    private boolean isStartDateTaken(LocalDate startDate) {
        WorkScheduleAssignment edited = getEditedEntity();
        User user = edited.getUser();
        if (user == null) {
            return false;
        }

        boolean takenBySibling = siblingAssignments.stream()
                .anyMatch(sibling -> !sibling.getId().equals(edited.getId())
                        && sibling.getUser() != null
                        && user.getId().equals(sibling.getUser().getId())
                        && startDate.equals(sibling.getStartDate()));
        if (takenBySibling) {
            return true;
        }

        // A saved row counts only when the editor does not hold it: a sibling stands for its row as edited there,
        // a removed one is gone, and the edited assignment is not a duplicate of itself
        Set<UUID> heldIds = siblingAssignments.stream()
                .map(WorkScheduleAssignment::getId)
                .collect(Collectors.toCollection(HashSet::new));
        heldIds.addAll(removedIds);
        heldIds.add(edited.getId());
        return workScheduleAssignmentSupport.loadAssignmentsFrom(List.of(user), startDate).stream()
                .anyMatch(saved -> !heldIds.contains(saved.getId()));
    }
}
