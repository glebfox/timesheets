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

package com.glebfox.timesheets.view.absence;

import com.glebfox.timesheets.app.AbsenceSupport;
import com.glebfox.timesheets.component.daterange.DateRangeField;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Edits one absence. Its dates are a {@link DateRangeField} bound to the absence's first and last day, which writes
 * an empty last day as the first day, so a one-day absence needs no last day. Refuses a range that shares a day with
 * another absence of the same user.
 */
@Route(value = "absences/:id", layout = MainView.class)
@ViewController("ts_Absence.detail")
@ViewDescriptor("absence-detail-view.xml")
@EditedEntityContainer("absenceDc")
@DialogMode(width = "480px", closeOnEsc = true)
public class AbsenceDetailView extends StandardDetailView<Absence> {

    @ViewComponent
    private EntityComboBox<AbsenceType> typeField;
    @ViewComponent
    private DateRangeField datesField;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private AbsenceSupport absenceSupport;

    private boolean overlapRefused;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        AbsenceType type = getEditedEntity().getType();
        if (type != null && type.getDeletedDate() != null) {
            typeField.setHelperText(messageBundle.getMessage("typeField.deletedHelperText"));
        }
    }

    @Subscribe(id = "absenceDc", target = Target.DATA_CONTAINER)
    public void onAbsenceDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<Absence> event) {
        switch (event.getProperty()) {
            case "user" -> clearOverlapRefusal();
            // The field offers only non-deleted types, so the newly chosen one is not deleted
            case "type" -> typeField.setHelperText(null);
            default -> {
            }
        }
    }

    @Subscribe
    public void onValidation(final ValidationEvent event) {
        Absence absence = getEditedEntity();
        if (absence.getUser() == null || absence.getFirstDay() == null || absence.getLastDay() == null) {
            return;
        }

        List<Absence> overlapping = absenceSupport.findOverlapping(absence.getUser(),
                absence.getFirstDay(), absence.getLastDay(), absence.getId());
        if (!overlapping.isEmpty()) {
            String absences = overlapping.stream()
                    .map(metadataTools::getInstanceName)
                    .collect(Collectors.joining(", "));
            String message = messageBundle.formatMessage("datesField.overlapError", absences);

            // Marked before the message is set: a validation of the field while it is invalid resets its message,
            // and the mark itself runs none
            datesField.setInvalid(true);
            datesField.setErrorMessage(message);
            overlapRefused = true;

            ValidationErrors errors = new ValidationErrors();
            errors.add(datesField, message);
            event.addErrors(errors);
        }
    }

    // A change of a date clears the refusal by itself. A change of the user does not touch the dates, and clears
    // only the refusal this view made: clearing runs the field's validation, which would mark empty dates of a new
    // absence as required
    private void clearOverlapRefusal() {
        if (overlapRefused) {
            overlapRefused = false;
            datesField.setInvalid(false);
        }
    }
}
