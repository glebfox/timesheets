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

import com.glebfox.timesheets.app.util.DateRange;
import com.glebfox.timesheets.app.util.DateRangeFormatter;
import com.glebfox.timesheets.app.util.UnsavedInputSupport;
import com.glebfox.timesheets.component.daterange.DateRangeField;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.flowui.component.radiobuttongroup.JmixRadioButtonGroup;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.component.validation.ValidationErrors;
import io.jmix.flowui.kit.action.ActionPerformedEvent;
import io.jmix.flowui.view.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Adds days to a holiday calendar from a range of dates, one day per date, each with the same name and type.
 * Opened as a dialog from the calendar editor, which hands it the calendar's days as they stand in the editor.
 * The dates are an unbound {@link DateRangeField}, which refuses a range that ends before it starts; this view
 * refuses a range longer than 31 days or with a date the calendar already has. Otherwise it closes with
 * {@link StandardOutcome#SAVE}, and the opener adds the days of {@link #getDates()}.
 * Closed any other way while a field differs from how it opened, it asks whether to discard the input:
 * Yes closes it, No keeps it open.
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
    private DateRangeField datesField;
    @ViewComponent
    private JmixRadioButtonGroup<HolidayCalendarDayType> typeField;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private ViewValidation viewValidation;
    @Autowired
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private DateRangeFormatter dateRangeFormatter;
    @Autowired
    private UnsavedInputSupport unsavedInputSupport;

    private Collection<HolidayCalendarDay> calendarDays = List.of();
    private List<Object> openedValues = List.of();

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
        DateRange range = Objects.requireNonNull(datesField.getValue());
        return range.start().datesUntil(range.endOrStart().plusDays(1)).toList();
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
        unsavedInputSupport.askBeforeDiscarding(event, !getFieldValues().equals(openedValues));
    }

    private List<Object> getFieldValues() {
        // The values as the fields show them, nulls included
        return Arrays.asList(nameField.getValue(), datesField.getValue(), typeField.getValue());
    }

    @Subscribe("okAction")
    public void onOkAction(final ActionPerformedEvent event) {
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
        DateRange range = Objects.requireNonNull(datesField.getValue());
        LocalDate from = Objects.requireNonNull(range.start());
        LocalDate to = range.endOrStart();

        if (dateRangeFormatter.countDays(from, to) > MAX_DAYS) {
            refuseDates(messageBundle.getMessage("datesField.tooLongError"), errors);
        } else {
            List<LocalDate> taken = findTakenDates(from, to);
            if (!taken.isEmpty()) {
                String dates = taken.stream()
                        .map(datatypeFormatter::formatLocalDate)
                        .collect(Collectors.joining(", "));
                refuseDates(messageBundle.formatMessage("datesField.duplicateError", dates), errors);
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

    private void refuseDates(String message, ValidationErrors errors) {
        // Marked before the message is set: marking runs the field's validators, which clear the message
        // of a field that is invalid already. A change of either date clears the refusal by itself.
        datesField.setInvalid(true);
        datesField.setErrorMessage(message);
        errors.add(datesField, message);
    }
}
