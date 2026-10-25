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

import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.view.*;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * Edits one day of a holiday calendar. Opened as a dialog from the calendar editor, which hands it
 * the calendar's days as they stand in the editor, so a date another day already has is refused.
 */
@ViewController("ts_HolidayCalendarDay.detail")
@ViewDescriptor("holiday-calendar-day-detail-view.xml")
@EditedEntityContainer("holidayCalendarDayDc")
@DialogMode(width = "480px", closeOnEsc = true)
public class HolidayCalendarDayDetailView extends StandardDetailView<HolidayCalendarDay> {

    @ViewComponent
    private MessageBundle messageBundle;

    private Supplier<Collection<HolidayCalendarDay>> calendarDays = List::of;

    /**
     * Sets the days of the calendar to check the edited day's date against: the days of the calendar editor,
     * saved or added there and not removed. The edited day itself may be among them.
     *
     * @param calendarDays supplies the calendar's days when the date is checked
     */
    public void setCalendarDays(Supplier<Collection<HolidayCalendarDay>> calendarDays) {
        this.calendarDays = calendarDays;
    }

    @Install(to = "dateField", subject = "validator")
    private void dateFieldValidator(final @Nullable LocalDate value) {
        if (value == null) {
            return;
        }

        UUID editedId = getEditedEntity().getId();
        boolean taken = calendarDays.get().stream()
                .anyMatch(day -> !day.getId().equals(editedId) && value.equals(day.getDate()));
        if (taken) {
            throw new ValidationException(messageBundle.getMessage("dateField.duplicateError"));
        }
    }
}
