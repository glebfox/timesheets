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

package com.glebfox.timesheets.view.workschedule;

import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.view.main.MainView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.Route;
import io.jmix.core.MetadataTools;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.checkboxgroup.JmixCheckboxGroup;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.util.EnumSet;
import java.util.Set;

@Route(value = "work-schedules/:id", layout = MainView.class)
@ViewController("ts_WorkSchedule.detail")
@ViewDescriptor("work-schedule-detail-view.xml")
@EditedEntityContainer("workScheduleDc")
public class WorkScheduleDetailView extends StandardDetailView<WorkSchedule> {

    @ViewComponent
    private JmixCheckboxGroup<DayOfWeek> workdaysField;
    @ViewComponent
    private EntityComboBox<HolidayCalendar> holidayCalendarField;
    @ViewComponent
    private Switch isDefaultField;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private MetadataTools metadataTools;
    @Autowired
    private ComponentSupport componentSupport;

    private final Span workdaysSummary = new Span();

    private boolean loadedAsDefault;
    private boolean fillingWorkdaysField;

    @Subscribe
    public void onInit(final InitEvent event) {
        initWorkdaysField();
    }

    private void initWorkdaysField() {
        workdaysField.setItems(componentSupport.getDaysOfWeek());
        workdaysField.setItemLabelGenerator(componentSupport::formatDayOfWeekShort);

        workdaysSummary.setId("workdaysSummary");
        workdaysField.setHelperComponent(workdaysSummary);
    }

    @Subscribe
    public void onReady(final ReadyEvent event) {
        WorkSchedule schedule = getEditedEntity();

        fillWorkdaysField(schedule);
        updateWorkdaysSummary();

        loadedAsDefault = Boolean.TRUE.equals(schedule.getIsDefault());
        if (loadedAsDefault) {
            isDefaultField.setHelperText(messageBundle.getMessage("isDefaultField.helperText"));
        }
        updateReadOnlyFields();

        HolidayCalendar holidayCalendar = schedule.getHolidayCalendar();
        if (holidayCalendar != null && holidayCalendar.getDeletedDate() != null) {
            holidayCalendarField.setHelperText(messageBundle.getMessage("holidayCalendarField.deletedHelperText"));
        }
    }

    @Subscribe
    public void onReadOnlyChange(final ReadOnlyChangeEvent event) {
        updateReadOnlyFields();
    }

    private void updateReadOnlyFields() {
        // The view's read-only mode reaches only the fields bound to a property
        workdaysField.setReadOnly(isReadOnly());
        // The bound switch would be made editable again when the view leaves its read-only mode
        isDefaultField.setReadOnly(isReadOnly() || loadedAsDefault);
    }

    @Install(to = "workdayTimeField", subject = "validator")
    private void workdayTimeFieldValidator(final @Nullable WorkTime value) {
        if (value != null && value.toMinutes() == 0) {
            throw new ValidationException(messageBundle.getMessage("workdayTimeField.zeroError"));
        }
    }

    @Subscribe("workdaysField")
    public void onWorkdaysFieldComponentValueChange(
            final ComponentValueChangeEvent<CheckboxGroup<DayOfWeek>, Set<DayOfWeek>> event) {
        if (!fillingWorkdaysField) {
            setWorkdays(getEditedEntity(), event.getValue());
        }
    }

    @Subscribe(id = "workScheduleDc", target = Target.DATA_CONTAINER)
    public void onWorkScheduleDcItemPropertyChange(final InstanceContainer.ItemPropertyChangeEvent<WorkSchedule> event) {
        if ("holidayCalendar".equals(event.getProperty())) {
            // The field offers only non-deleted calendars, so the newly chosen one is not deleted
            holidayCalendarField.setHelperText(null);
        }

        updateWorkdaysSummary();
    }

    private void fillWorkdaysField(WorkSchedule schedule) {
        // Filling the field must not write the flags back: a null flag counts as off but is not false
        fillingWorkdaysField = true;
        try {
            workdaysField.setValue(getWorkdays(schedule));
        } finally {
            fillingWorkdaysField = false;
        }
    }

    private void updateWorkdaysSummary() {
        WorkSchedule schedule = getEditedEntity();
        int workdays = getWorkdays(schedule).size();
        String workdaysText = messageBundle.formatMessage(
                PluralForms.key("workdaysSummary.workdays", workdays, currentAuthentication.getLocale()), workdays);

        WorkTime workdayTime = schedule.getWorkdayTime();
        if (workdayTime == null) {
            workdaysSummary.setText(workdaysText);
            return;
        }

        WorkTime weekTotal = WorkTime.ofMinutes(workdayTime.toMinutes() * workdays);
        workdaysSummary.setText(messageBundle.formatMessage("workdaysSummary.text", workdaysText, formatTotal(weekTotal)));
    }

    // As the calendar view formats its totals
    private String formatTotal(WorkTime time) {
        return time.getMinutes() == 0
                ? messageBundle.formatMessage("totalHours.format", time.getHours())
                : metadataTools.format(time);
    }

    /**
     * Returns the days of the week that are on in a work schedule; a day whose flag is null is off.
     */
    static Set<DayOfWeek> getWorkdays(WorkSchedule schedule) {
        Set<DayOfWeek> workdays = EnumSet.noneOf(DayOfWeek.class);
        addIfOn(workdays, DayOfWeek.MONDAY, schedule.getMonday());
        addIfOn(workdays, DayOfWeek.TUESDAY, schedule.getTuesday());
        addIfOn(workdays, DayOfWeek.WEDNESDAY, schedule.getWednesday());
        addIfOn(workdays, DayOfWeek.THURSDAY, schedule.getThursday());
        addIfOn(workdays, DayOfWeek.FRIDAY, schedule.getFriday());
        addIfOn(workdays, DayOfWeek.SATURDAY, schedule.getSaturday());
        addIfOn(workdays, DayOfWeek.SUNDAY, schedule.getSunday());
        return workdays;
    }

    private static void addIfOn(Set<DayOfWeek> workdays, DayOfWeek day, @Nullable Boolean flag) {
        if (Boolean.TRUE.equals(flag)) {
            workdays.add(day);
        }
    }

    private static void setWorkdays(WorkSchedule schedule, Set<DayOfWeek> workdays) {
        schedule.setMonday(workdays.contains(DayOfWeek.MONDAY));
        schedule.setTuesday(workdays.contains(DayOfWeek.TUESDAY));
        schedule.setWednesday(workdays.contains(DayOfWeek.WEDNESDAY));
        schedule.setThursday(workdays.contains(DayOfWeek.THURSDAY));
        schedule.setFriday(workdays.contains(DayOfWeek.FRIDAY));
        schedule.setSaturday(workdays.contains(DayOfWeek.SATURDAY));
        schedule.setSunday(workdays.contains(DayOfWeek.SUNDAY));
    }
}
