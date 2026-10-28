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

package com.glebfox.timesheets.calendar;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.app.CalendarSupport;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import com.google.common.collect.Lists;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.fullcalendarflowui.component.FullCalendar;
import io.jmix.fullcalendarflowui.component.data.CalendarEvent;
import io.jmix.fullcalendarflowui.component.data.ContainerCalendarDataProvider;
import io.jmix.fullcalendarflowui.component.data.EntityCalendarEvent;
import io.jmix.fullcalendarflowui.component.data.ListCalendarDataProvider;
import io.jmix.fullcalendarflowui.component.event.AbstractEventMoveEvent.OldValues;
import io.jmix.fullcalendarflowui.component.event.DatesSetEvent;
import io.jmix.fullcalendarflowui.component.event.DisplayModeInfo;
import io.jmix.fullcalendarflowui.component.event.EventClickEvent;
import io.jmix.fullcalendarflowui.component.event.EventDropEvent;
import io.jmix.fullcalendarflowui.kit.component.model.CalendarDisplayModes;
import io.jmix.fullcalendarflowui.kit.component.model.CalendarDuration;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import io.jmix.tabbedmode.testassist.dialog.DialogInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class CalendarUiTest {

    // December 2024 starts on Sunday, so its month grid runs from December 1 to January 4
    static final LocalDate GRID_START = LocalDate.of(2024, 12, 1);
    static final LocalDate GRID_END = LocalDate.of(2025, 1, 5);
    static final LocalDate DAY = LocalDate.of(2024, 12, 3);

    static final String USERNAME = "admin";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    CalendarSupport calendarSupport;

    List<Object> entities = new ArrayList<>();

    @Test
    void moveEditedTimeEntryTest() {
        /* Given */
        TimeEntry timeEntry = createTimeEntry();

        systemAuthenticator.runWithUser(USERNAME, () -> {
            CalendarView calendarView = openCalendar();
            editSpentTime(calendarView, timeEntry, WorkTime.ofHours(2));

            /* When */
            moveTimeEntry(calendarView, timeEntry, DAY.plusDays(1));
        });

        /* Then */
        TimeEntry saved = dataManager.load(Id.of(timeEntry)).one();
        assertThat(saved.getDate()).isEqualTo(DAY.plusDays(1));
        assertThat(saved.getSpentTime()).isEqualTo(WorkTime.ofHours(2));
    }

    @Test
    void removeEditedTimeEntryTest() {
        /* Given */
        TimeEntry timeEntry = createTimeEntry();

        systemAuthenticator.runWithUser(USERNAME, () -> {
            CalendarView calendarView = openCalendar();
            editSpentTime(calendarView, timeEntry, WorkTime.ofHours(2));

            /* When */
            // The calendar's context menu calls this with the entry it holds
            calendarSupport.removeTimeEntry(findCalendarEvent(calendarView, timeEntry).getEntity(), __ -> {
            });
            DialogInfo confirmationDialog = Objects.requireNonNull(UiTestUtils.getLastOpenedDialog(),
                    "No confirmation dialog opened");
            Button yesButton = confirmationDialog.getButtons().stream()
                    .filter(button -> "yes".equals(button.getId().orElse(null)))
                    .findFirst()
                    .orElseThrow();
            yesButton.click();
        });

        /* Then */
        assertThat(dataManager.load(Id.of(timeEntry)).optional()).isEmpty();
    }

    @Test
    void weekendsAreMarkedTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* When */
            CalendarView calendarView = openCalendar();

            /* Then */
            // admin has no assignments, so the default schedule plans the grid: 8 hours Monday to Friday,
            // with no holiday calendar in the test profile
            List<CalendarEvent> dayMarks = getDayMarks(calendarView);
            assertThat(dayMarks).extracting(CalendarUiTest::dateOf)
                    .hasSize(10)
                    .containsExactlyElementsOf(GRID_START.datesUntil(GRID_END)
                            .filter(date -> date.getDayOfWeek() == DayOfWeek.SATURDAY
                                    || date.getDayOfWeek() == DayOfWeek.SUNDAY)
                            .toList());
            assertThat(dayMarks).extracting(CalendarEvent::getClassNames).containsOnly("calendar-view-unplanned-day");
            assertThat(dayMarks).extracting(CalendarEvent::getTitle).containsOnlyNulls();
        });
    }

    @Test
    void refreshReloadsThePlanTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* Given */
            CalendarView calendarView = openCalendar();
            assertThat(getDayMarks(calendarView)).extracting(CalendarUiTest::dateOf).doesNotContain(DAY);

            // Saved after the calendar has loaded its plan: Saturdays become workdays, and Tuesday, Dec 3 a holiday
            assignScheduleWithHoliday();

            /* When */
            JmixButton refreshBtn = UiTestUtils.getComponent(calendarView, "refreshBtn");
            refreshBtn.click();

            /* Then */
            List<CalendarEvent> dayMarks = getDayMarks(calendarView);
            assertThat(dayMarks).extracting(CalendarUiTest::dateOf)
                    .doesNotContain(LocalDate.of(2024, 12, 7))
                    .contains(LocalDate.of(2024, 12, 8));
            assertThat(dayMarks).filteredOn(dayMark -> dateOf(dayMark).equals(DAY))
                    .singleElement()
                    .extracting(CalendarEvent::getClassNames, CalendarEvent::getTitle)
                    .containsExactly("calendar-view-unplanned-day calendar-view-holiday-day-off", "Test holiday");
        });
    }

    @Test
    void navigatingReplacesTheDayMarksTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* Given */
            CalendarView calendarView = openCalendar();

            /* When */
            // January 2025 starts on Wednesday, so its month grid runs from December 29 to February 1
            LocalDate januaryGridStart = LocalDate.of(2024, 12, 29);
            LocalDate januaryGridEnd = LocalDate.of(2025, 2, 2);
            FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
            ComponentUtil.fireEvent(calendar,
                    new DatesSetEvent(calendar, true, januaryGridStart, januaryGridEnd,
                            new DisplayModeInfo(januaryGridStart, januaryGridEnd,
                                    LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1),
                                    CalendarDisplayModes.DAY_GRID_MONTH)));

            /* Then */
            assertThat(getDayMarks(calendarView)).extracting(CalendarUiTest::dateOf)
                    .allMatch(date -> !date.isBefore(januaryGridStart))
                    .contains(LocalDate.of(2025, 2, 1));
        });
    }

    @AfterEach
    void tearDown() {
        // The tests change and remove what they created, so the instances in the list are stale
        List<Object> reloaded = Lists.reverse(entities).stream()
                .map(entity -> dataManager.load(Id.of(entity))
                        .hint(PersistenceHints.SOFT_DELETION, false)
                        .optional())
                .flatMap(Optional::stream)
                .toList();

        dataManager.save(
                new SaveContext()
                        .removing(reloaded)
                        .setHint(PersistenceHints.SOFT_DELETION, false)
        );
    }

    private CalendarView openCalendar() {
        viewBuilders.view(UiTestUtils.getCurrentView(), CalendarView.class).open();
        CalendarView calendarView = UiTestUtils.getCurrentView();

        // The browser reports the displayed dates, which makes the calendar load its entries
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ComponentUtil.fireEvent(calendar,
                new DatesSetEvent(calendar, true, GRID_START, GRID_END, monthDisplayModeInfo()));

        return calendarView;
    }

    private void editSpentTime(CalendarView calendarView, TimeEntry timeEntry, WorkTime spentTime) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        EntityCalendarEvent<TimeEntryCalendarWrapper> calendarEvent = findCalendarEvent(calendarView, timeEntry);
        ComponentUtil.fireEvent(calendar,
                new EventClickEvent(calendar, true, null, calendarEvent, getDataProvider(calendarView),
                        monthDisplayModeInfo()));

        TimeEntryDetailView detailView = UiTestUtils.getCurrentView();
        TypedTextField<WorkTime> spentTimeField = UiTestUtils.getComponent(detailView, "spentTimeField");
        spentTimeField.setTypedValue(spentTime);

        JmixButton saveAndCloseBtn = UiTestUtils.getComponent(detailView, "saveAndCloseBtn");
        saveAndCloseBtn.click();
    }

    private void moveTimeEntry(CalendarView calendarView, TimeEntry timeEntry, LocalDate date) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        EntityCalendarEvent<TimeEntryCalendarWrapper> calendarEvent = findCalendarEvent(calendarView, timeEntry);
        LocalDate oldDate = calendarEvent.getEntity().getDate();

        // The calendar applies the new date to the dropped event before it fires the event
        calendarEvent.setStartDateTime(date.atStartOfDay());
        ComponentUtil.fireEvent(calendar,
                new EventDropEvent(calendar, true, calendarEvent, getDataProvider(calendarView),
                        List.of(), List.of(), new OldValues(oldDate.atStartOfDay(), null, true),
                        CalendarDuration.ofDays(1), null, monthDisplayModeInfo()));
    }

    @SuppressWarnings("unchecked")
    private EntityCalendarEvent<TimeEntryCalendarWrapper> findCalendarEvent(CalendarView calendarView,
                                                                           TimeEntry timeEntry) {
        return getDataProvider(calendarView).getItems().stream()
                .map(calendarEvent -> (EntityCalendarEvent<TimeEntryCalendarWrapper>) calendarEvent)
                .filter(calendarEvent -> timeEntry.getId().equals(calendarEvent.getEntity().getTimeEntry().getId()))
                .findFirst()
                .orElseThrow();
    }

    private ContainerCalendarDataProvider<TimeEntryCalendarWrapper> getDataProvider(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        return calendar.getDataProvider("timeEntriesDataProvider");
    }

    private List<CalendarEvent> getDayMarks(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ListCalendarDataProvider dayMarksProvider = Objects.requireNonNull(
                calendar.getDataProvider(CalendarView.DAY_MARKS_PROVIDER_ID), "The calendar has no day marks");
        return dayMarksProvider.getItems();
    }

    private static LocalDate dateOf(CalendarEvent dayMark) {
        return Objects.requireNonNull(dayMark.getStartDateTime(), "A day mark has no date").toLocalDate();
    }

    private DisplayModeInfo monthDisplayModeInfo() {
        return new DisplayModeInfo(GRID_START, GRID_END, GRID_START, GRID_START.plusMonths(1),
                CalendarDisplayModes.DAY_GRID_MONTH);
    }

    private TimeEntry createTimeEntry() {
        Client client = dataManager.create(Client.class);
        client.setName("Calendar UI Test Client");
        entities.add(client);

        Project project = dataManager.create(Project.class);
        project.setName("Calendar UI Test Project");
        project.setClient(client);
        project.setStatus(ProjectStatus.OPEN);
        entities.add(project);

        TaskType taskType = dataManager.create(TaskType.class);
        taskType.setName("Calendar UI Test Task Type");
        entities.add(taskType);

        Task task = dataManager.create(Task.class);
        task.setName("Calendar UI Test Task");
        task.setProject(project);
        task.setType(taskType);
        task.setStatus(TaskStatus.ACTIVE);
        entities.add(task);

        TimeEntry timeEntry = dataManager.create(TimeEntry.class);
        timeEntry.setDate(DAY);
        timeEntry.setUser(loadUser());
        timeEntry.setTask(task);
        timeEntry.setSpentTime(WorkTime.ofHours(1));
        timeEntry.setStatus(TimeEntryStatus.NEW);
        entities.add(timeEntry);

        dataManager.saveAll(entities);

        return timeEntry;
    }

    // Assigns admin, from the start of the grid, to 8 hours Monday to Saturday with a calendar
    // where Tuesday, Dec 3 is a day off
    private void assignScheduleWithHoliday() {
        HolidayCalendar holidayCalendar = dataManager.create(HolidayCalendar.class);
        holidayCalendar.setName("Calendar UI Test Holiday Calendar");

        HolidayCalendarDay holiday = dataManager.create(HolidayCalendarDay.class);
        holiday.setCalendar(holidayCalendar);
        holiday.setDate(DAY);
        holiday.setType(HolidayCalendarDayType.DAY_OFF);
        holiday.setName("Test holiday");

        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName("Calendar UI Test Schedule");
        schedule.setWorkdayTime(WorkTime.ofHours(8));
        schedule.setSaturday(true);
        schedule.setHolidayCalendar(holidayCalendar);

        WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
        assignment.setUser(loadUser());
        assignment.setWorkSchedule(schedule);
        assignment.setStartDate(GRID_START);

        // The order lets the teardown remove each before what it refers to
        List<Object> created = List.of(holidayCalendar, holiday, schedule, assignment);
        entities.addAll(created);
        dataManager.saveAll(created);
    }

    private User loadUser() {
        return dataManager.load(User.class)
                .query("e.username = ?1", USERNAME)
                .one();
    }
}
