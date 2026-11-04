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
import com.glebfox.timesheets.app.util.DateRangeFormatter;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.flowui.component.SupportsTypedValue;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Edits one absence. The last day is a field of the view's own, not bound to the absence: an empty one makes
 * a one-day absence, so the view sets the absence's last day to the field's date, or to the first day when the field
 * is empty. Refuses a range that shares a day with another absence of the same user.
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
    private TypedDatePicker<LocalDate> firstDayField;
    @ViewComponent
    private TypedDatePicker<LocalDate> lastDayField;
    @ViewComponent
    private Span rangeSummary;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private AbsenceSupport absenceSupport;
    @Autowired
    private DateRangeFormatter dateRangeFormatter;

    private boolean datesRefused;

    @Subscribe
    public void onReady(final ReadyEvent event) {
        Absence absence = getEditedEntity();

        // The stored days come with the absence, not as a change of a day, so the last day and its picker are set
        // from them here; an absence opened with only a first day gets it as its last day. A one-day absence
        // opens with the last day empty, as the view creates one, so it stays one day when its first day moves
        followFirstDay(absence.getFirstDay());
        if (!Objects.equals(absence.getLastDay(), absence.getFirstDay())) {
            lastDayField.setTypedValue(absence.getLastDay());
        }
        onDatesChange();
        updateReadOnlyFields();

        AbsenceType type = absence.getType();
        if (type != null && type.getDeletedDate() != null) {
            typeField.setHelperText(messageBundle.getMessage("typeField.deletedHelperText"));
        }
    }

    @Subscribe
    public void onReadOnlyChange(final ReadOnlyChangeEvent event) {
        updateReadOnlyFields();
    }

    private void updateReadOnlyFields() {
        // The view's read-only mode reaches only the fields bound to a property
        lastDayField.setReadOnly(isReadOnly());
    }

    @Subscribe(id = "absenceDc", target = Target.DATA_CONTAINER)
    public void onAbsenceDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<Absence> event) {
        switch (event.getProperty()) {
            case "user" -> clearRefusedDates();
            // The field offers only non-deleted types, so the newly chosen one is not deleted
            case "type" -> typeField.setHelperText(null);
            case "firstDay" -> {
                followFirstDay(event.getItem().getFirstDay());
                if (lastDayField.isInvalid()) {
                    // Runs the last day's validation against the new minimum, which a new minimum alone does not do
                    lastDayField.setInvalid(false);
                }
                onDatesChange();
            }
            default -> {
            }
        }
    }

    // The last day's picker offers no day before the first one and, while the last day is empty, opens at the first
    // day rather than at today, which may be months away from the absence
    private void followFirstDay(@Nullable LocalDate firstDay) {
        lastDayField.setMin(firstDay);
        // Once the picker has opened, Vaadin 25.3's overlay keeps the month of its first opening: a later change of
        // the first day moves the minimum and the position set here, but not the month the picker opens at, as
        // vaadin-date-picker-mixin's _getInitialPosition prefers the overlay content's own initialPosition. Seen
        // live on 2026-11-04; left as it is, since a workaround would reach into the component's internals
        lastDayField.setInitialPosition(firstDay);
    }

    @Subscribe("lastDayField")
    public void onLastDayFieldTypedValueChange(
            final SupportsTypedValue.TypedValueChangeEvent<TypedDatePicker<LocalDate>, LocalDate> event) {
        onDatesChange();
    }

    private void onDatesChange() {
        Absence absence = getEditedEntity();
        LocalDate lastDay = lastDayField.getTypedValue();
        // An empty last day makes a one-day absence
        LocalDate storedLastDay = lastDay != null ? lastDay : absence.getFirstDay();
        if (!Objects.equals(absence.getLastDay(), storedLastDay)) {
            absence.setLastDay(storedLastDay);
        }

        clearRefusedDates();
        rangeSummary.setText(absence.getFirstDay() != null && absence.getLastDay() != null
                ? dateRangeFormatter.formatSummary(absence.getFirstDay(), absence.getLastDay())
                : "");
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
            event.addErrors(refuseDates(messageBundle.formatMessage("firstDayField.overlapError", absences)));
        }
    }

    private ValidationErrors refuseDates(String message) {
        // Marked before the message is set: marking runs the field's validators, which clear the message
        // of a field that is invalid already
        firstDayField.setInvalid(true);
        firstDayField.setErrorMessage(message);
        if (lastDayField.getTypedValue() != null) {
            // Both dates are marked, as the range is; the absences are listed once, under the first one
            lastDayField.setInvalid(true);
        }
        datesRefused = true;

        ValidationErrors errors = new ValidationErrors();
        errors.add(firstDayField, message);
        return errors;
    }

    private void clearRefusedDates() {
        if (datesRefused) {
            datesRefused = false;
            // Runs the fields' own validation again, which clears the refusal
            firstDayField.setInvalid(false);
            lastDayField.setInvalid(false);
        }
    }
}
