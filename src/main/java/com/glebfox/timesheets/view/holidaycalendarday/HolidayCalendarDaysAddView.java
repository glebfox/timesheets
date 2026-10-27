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

package com.glebfox.timesheets.view.holidaycalendarday;

import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.vaadin.flow.component.html.Span;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.SupportsTypedValue;
import io.jmix.flowui.component.datepicker.TypedDatePicker;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.util.UnknownOperationResult;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Adds days to a holiday calendar from a range of dates, one day per date, each with the same name and type.
 * Opened as a dialog from the calendar editor, which hands it the calendar's days as they stand in the editor.
 * Refuses a range that ends before it starts, is longer than 31 days, or has a date the calendar already has;
 * otherwise closes with {@link StandardOutcome#SAVE}, and the opener adds the days of {@link #getDates()}.
 * Closed any other way after a field changed, it asks whether to discard the input, as a detail view does.
 */
@ViewController(HolidayCalendarDaysAddView.ID)
@ViewDescriptor("holiday-calendar-days-add-view.xml")
@DialogMode(width = "480px", closeOnEsc = true)
public class HolidayCalendarDaysAddView extends StandardView {

    public static final String ID = "ts_HolidayCalendarDaysAddView";

    /**
     * The longest range accepted, so a typo in a year does not add a year of days.
     */
    private static final int MAX_DAYS = 31;

    @ViewComponent
    private TypedTextField<String> nameField;
    @ViewComponent
    private TypedDatePicker<LocalDate> fromField;
    @ViewComponent
    private TypedDatePicker<LocalDate> toField;
    @ViewComponent
    private JmixRadioButtonGroup<HolidayCalendarDayType> typeField;
    @ViewComponent
    private Span rangeSummary;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private ViewValidation viewValidation;
    @Autowired
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private CurrentAuthentication currentAuthentication;

    private Collection<HolidayCalendarDay> calendarDays = List.of();
    private List<Object> openedValues = List.of();
    private boolean datesRefused;

    /**
     * Sets the days of the calendar to check the range against: the days of the calendar editor as they stand
     * when the view opens, saved or added there and not removed. The view keeps a copy, so a later change
     * of the given collection does not reach it.
     *
     * @param calendarDays the calendar's days, possibly none
     */
    public void setCalendarDays(Collection<HolidayCalendarDay> calendarDays) {
        this.calendarDays = List.copyOf(Objects.requireNonNull(calendarDays));
    }

    /**
     * Returns the name of the days to add.
     */
    public String getDayName() {
        return Objects.requireNonNull(nameField.getTypedValue());
    }

    /**
     * Returns the type of the days to add.
     */
    public HolidayCalendarDayType getDayType() {
        return Objects.requireNonNull(typeField.getValue());
    }

    /**
     * Returns the dates of the days to add, in order: every date from the first day to the last one,
     * weekends included, or only the first day when the last one is empty.
     */
    public List<LocalDate> getDates() {
        LocalDate from = Objects.requireNonNull(fromField.getTypedValue());
        return from.datesUntil(getLastDay(from).plusDays(1)).toList();
    }

    @Subscribe
    public void onInit(final InitEvent event) {
        // Most calendar days are days off
        typeField.setValue(HolidayCalendarDayType.DAY_OFF);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        openedValues = getFieldValues();
    }

    @Subscribe
    public void onBeforeClose(final BeforeCloseEvent event) {
        // OK takes the input; Cancel, Esc, and the close button ask before they discard a change
        if (!event.closedWith(StandardOutcome.SAVE)
                && event.getCloseAction() instanceof ChangeTrackerCloseAction closeAction
                && closeAction.isCheckForUnsavedChanges()
                && !getFieldValues().equals(openedValues)) {
            UnknownOperationResult result = new UnknownOperationResult();
            viewValidation.showUnsavedChangesDialog(this)
                    .onDiscard(() -> result.resume(close(StandardOutcome.DISCARD)))
                    .onCancel(result::fail);
            event.preventClose(result);
        }
    }

    private List<Object> getFieldValues() {
        // The values as the fields show them, nulls included
        return Arrays.asList(nameField.getValue(), fromField.getValue(), toField.getValue(), typeField.getValue());
    }

    @Subscribe("fromField")
    public void onFromFieldTypedValueChange(
            final SupportsTypedValue.TypedValueChangeEvent<TypedDatePicker<LocalDate>, LocalDate> event) {
        // The last day's picker offers no day before the first one
        toField.setMin(event.getValue());
        onDatesChange();
    }

    @Subscribe("toField")
    public void onToFieldTypedValueChange(
            final SupportsTypedValue.TypedValueChangeEvent<TypedDatePicker<LocalDate>, LocalDate> event) {
        onDatesChange();
    }

    private void onDatesChange() {
        clearRefusedDates();
        rangeSummary.setText(formatRangeSummary());
    }

    @Subscribe("okAction")
    public void onOkAction(final ActionPerformedEvent event) {
        clearRefusedDates();

        ValidationErrors errors = viewValidation.validateUiComponents(getContent());
        if (errors.isEmpty()) {
            errors = validateDates();
        }
        if (!errors.isEmpty()) {
            viewValidation.showValidationErrors(errors);
            viewValidation.focusProblemComponent(errors);
            return;
        }

        close(StandardOutcome.SAVE);
    }

    private ValidationErrors validateDates() {
        ValidationErrors errors = new ValidationErrors();
        LocalDate from = Objects.requireNonNull(fromField.getTypedValue());
        LocalDate to = getLastDay(from);

        if (to.isBefore(from)) {
            // A safety net: the last day's minimum refuses such a day before OK gets here
            refuseDates(toField, messageBundle.getMessage("toField.beforeFromError"), errors);
        } else if (countDays(from, to) > MAX_DAYS) {
            refuseDates(toField, messageBundle.getMessage("toField.tooLongError"), errors);
        } else {
            List<LocalDate> taken = findTakenDates(from, to);
            if (!taken.isEmpty()) {
                String dates = taken.stream()
                        .map(datatypeFormatter::formatLocalDate)
                        .collect(Collectors.joining(", "));
                refuseDates(fromField, messageBundle.formatMessage("fromField.duplicateError", dates), errors);
                if (toField.getTypedValue() != null) {
                    // Both dates are marked, as the range is; the dates are listed once, under the first one
                    toField.setInvalid(true);
                }
            }
        }
        return errors;
    }

    private List<LocalDate> findTakenDates(LocalDate from, LocalDate to) {
        return calendarDays.stream()
                .map(HolidayCalendarDay::getDate)
                .filter(date -> date != null && !date.isBefore(from) && !date.isAfter(to))
                .distinct()
                .sorted()
                .toList();
    }

    private void refuseDates(TypedDatePicker<LocalDate> field, String message, ValidationErrors errors) {
        // Marked before the message is set: marking runs the field's validators, which clear the message
        // of a field that is invalid already
        field.setInvalid(true);
        field.setErrorMessage(message);
        errors.add(field, message);
        datesRefused = true;
    }

    private void clearRefusedDates() {
        if (datesRefused) {
            datesRefused = false;
            // Runs the fields' own validation again, which clears the refusal
            fromField.setInvalid(false);
            toField.setInvalid(false);
        }
    }

    private String formatRangeSummary() {
        LocalDate from = fromField.getTypedValue();
        if (from == null) {
            return "";
        }
        LocalDate to = getLastDay(from);
        if (to.isBefore(from)) {
            return "";
        }

        long days = countDays(from, to);
        String daysText = messageBundle.formatMessage(
                PluralForms.key("rangeSummary.days", days, currentAuthentication.getLocale()), days);
        if (days == 1) {
            return messageBundle.formatMessage("rangeSummary.oneDay",
                    daysText, messageBundle.getMessage("rangeSummary.weekday." + from.getDayOfWeek()));
        }
        return messageBundle.formatMessage("rangeSummary.range", daysText,
                messageBundle.getMessage("rangeSummary.from." + from.getDayOfWeek()),
                messageBundle.getMessage("rangeSummary.to." + to.getDayOfWeek()));
    }

    private LocalDate getLastDay(LocalDate from) {
        LocalDate to = toField.getTypedValue();
        return to != null ? to : from;
    }

    private static long countDays(LocalDate from, LocalDate to) {
        return ChronoUnit.DAYS.between(from, to) + 1;
    }
}
