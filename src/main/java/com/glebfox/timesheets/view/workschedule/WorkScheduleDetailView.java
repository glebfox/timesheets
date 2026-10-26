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

import com.glebfox.timesheets.app.WorkScheduleAssignmentSupport;
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.main.MainView;
import com.glebfox.timesheets.view.workscheduleassignment.WorkScheduleUsersAddView;
import com.vaadin.flow.component.AbstractField.ComponentValueChangeEvent;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.Route;
import io.jmix.core.EntityStates;
import io.jmix.core.Sort;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.flowui.component.checkbox.Switch;
import io.jmix.flowui.component.checkboxgroup.JmixCheckboxGroup;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.exception.ValidationException;
import io.jmix.flowui.model.CollectionChangeType;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.model.DataContext;
import io.jmix.flowui.model.InstanceContainer;
import io.jmix.flowui.view.*;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
    private DataGrid<WorkScheduleAssignment> assignmentsDataGrid;
    @ViewComponent
    private CollectionContainer<WorkScheduleAssignment> assignmentsDc;
    @ViewComponent
    private CollectionLoader<WorkScheduleAssignment> assignmentsDl;
    @ViewComponent
    private MessageBundle messageBundle;

    @Autowired
    private CurrentAuthentication currentAuthentication;
    @Autowired
    private DatatypeFormatter datatypeFormatter;
    @Autowired
    private EntityStates entityStates;
    @Autowired
    private ComponentSupport componentSupport;
    @Autowired
    private WorkScheduleAssignmentSupport workScheduleAssignmentSupport;

    private final Span workdaysSummary = new Span();

    private boolean loadedAsDefault;
    private boolean fillingWorkdaysField;
    // The assignments in force today, found when the table is drawn and dropped whenever its assignments change
    @Nullable
    private Set<WorkScheduleAssignment> currentAssignments;

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
    public void onBeforeShow(final BeforeShowEvent event) {
        // A new schedule has no assignments to load
        if (!entityStates.isNew(getEditedEntity())) {
            assignmentsDl.setParameter("workSchedule", getEditedEntity());
            assignmentsDl.load();
        }
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

        assignmentsDataGrid.sort(GridSortOrder.desc(assignmentsDataGrid.getColumnByKey("startDate")).build());
    }

    @Subscribe
    public void onReadOnlyChange(final ReadOnlyChangeEvent event) {
        updateReadOnlyFields();
    }

    private void updateReadOnlyFields() {
        // The view's read-only mode reaches only the fields bound to a property
        workdaysField.setReadOnly(isReadOnly());
        // The binding and the view's read-only mode set whether the switch is editable, and would make it
        // editable again when the view leaves its read-only mode; the default's switch stays locked
        if (loadedAsDefault) {
            isDefaultField.setReadOnly(true);
        }
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

    @Subscribe(id = "assignmentsDc", target = Target.DATA_CONTAINER)
    public void onAssignmentsDcCollectionChange(
            final CollectionContainer.CollectionChangeEvent<WorkScheduleAssignment> event) {
        currentAssignments = null;

        // An added assignment is added last and an edited one stays in place, whatever its date, while the grid
        // sorts its rows only when its sort order changes. Sorting again also redraws every row, so the Current
        // badge moves when an edit changes which assignment is in force today
        if (event.getChangeType() == CollectionChangeType.ADD_ITEMS
                || event.getChangeType() == CollectionChangeType.SET_ITEM) {
            // Unsorted, the table keeps the order the assignments are loaded in: newest first
            componentSupport.sortAsShown(assignmentsDataGrid, assignmentsDc, Sort.by(Sort.Direction.DESC, "startDate"));
        }
    }

    @Install(to = "assignmentsDataGrid.add", subject = "initializer")
    private void assignmentsDataGridAddInitializer(final WorkScheduleAssignment assignment) {
        assignment.setWorkSchedule(getEditedEntity());
    }

    @Install(to = "assignmentsDataGrid.add", subject = "viewConfigurer")
    private void assignmentsDataGridAddViewConfigurer(final WorkScheduleUsersAddView view) {
        view.setWorkSchedule(getEditedEntity());
    }

    @Install(to = "assignmentsDataGrid.edit", subject = "afterSaveHandler")
    private void assignmentsDataGridEditAfterSaveHandler(final WorkScheduleAssignment assignment) {
        // An assignment moved to another schedule leaves the table; it stays changed in the data context,
        // so the editor's OK saves it with that schedule
        if (!getEditedEntity().getId().equals(assignment.getWorkSchedule().getId())) {
            WorkScheduleAssignment listed = assignmentsDc.getItemOrNull(assignment.getId());
            if (listed != null) {
                assignmentsDc.getMutableItems().remove(listed);
            }
        }
    }

    @Supply(to = "assignmentsDataGrid.user", subject = "renderer")
    private Renderer<WorkScheduleAssignment> assignmentsDataGridUserRenderer() {
        return componentSupport.createUserRenderer(WorkScheduleAssignment::getUser);
    }

    @Supply(to = "assignmentsDataGrid.startDate", subject = "renderer")
    private Renderer<WorkScheduleAssignment> assignmentsDataGridStartDateRenderer() {
        return new ComponentRenderer<>(assignment -> {
            Span cell = new Span(datatypeFormatter.formatLocalDate(assignment.getStartDate()));
            cell.addClassName("text-with-badges");

            if (getCurrentAssignments().contains(assignment)) {
                cell.add(componentSupport.createBadge(
                        messageBundle.getMessage("currentBadge.text"), BadgeStyle.SUCCESS));
            }
            return cell;
        });
    }

    private Set<WorkScheduleAssignment> getCurrentAssignments() {
        if (currentAssignments == null) {
            currentAssignments = findCurrentAssignments();
        }
        return currentAssignments;
    }

    private Set<WorkScheduleAssignment> findCurrentAssignments() {
        // A user's assignment here is in force today only if none of the user's assignments to any schedule
        // starts later and on or before today. A listed assignment stands for its saved row as edited here,
        // and a saved row removed here is gone
        List<WorkScheduleAssignment> listed = assignmentsDc.getItems();
        List<User> users = listed.stream()
                .map(WorkScheduleAssignment::getUser)
                .distinct()
                .toList();
        DataContext dataContext = getViewData().getDataContext();

        Map<UUID, WorkScheduleAssignment> assignments = new HashMap<>();
        workScheduleAssignmentSupport.loadAssignmentsOfUsers(users).stream()
                // The data context compares entities by id, so it recognizes a row loaded apart from it
                .filter(saved -> !dataContext.isRemoved(saved))
                .forEach(saved -> assignments.put(saved.getId(), saved));
        listed.forEach(assignment -> assignments.put(assignment.getId(), assignment));

        return workScheduleAssignmentSupport.findCurrent(assignments.values(), LocalDate.now());
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
        workdaysSummary.setText(messageBundle.formatMessage("workdaysSummary.text", workdaysText,
                componentSupport.formatWorkTimeTotal(weekTotal)));
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
