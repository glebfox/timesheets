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
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
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
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.View;
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
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class CalendarUiTest {

    // December 2024 starts on Sunday, so its month grid runs from December 1 to January 4
    static final LocalDate GRID_START = LocalDate.of(2024, 12, 1);
    static final LocalDate GRID_END = LocalDate.of(2025, 1, 5);
    // The same month with weeks that start on Monday, as in Russian: from November 25 to January 5
    static final LocalDate MONDAY_GRID_START = LocalDate.of(2024, 11, 25);
    static final LocalDate MONDAY_GRID_END = LocalDate.of(2025, 1, 6);
    static final LocalDate DAY = LocalDate.of(2024, 12, 3);
    // January 2025 starts on Wednesday, so its month grid runs from December 29 to February 1
    static final LocalDate JANUARY_GRID_START = LocalDate.of(2024, 12, 29);
    static final LocalDate JANUARY_GRID_END = LocalDate.of(2025, 2, 2);
    // Monday, Dec 9 to Wednesday, Dec 11: workdays of the default schedule, inside one week row
    static final LocalDate ABSENCE_FIRST_DAY = LocalDate.of(2024, 12, 9);
    static final LocalDate ABSENCE_LAST_DAY = LocalDate.of(2024, 12, 11);
    static final String ABSENCE_TYPE_NAME = "Calendar UI Test Absence Type";

    static final String USERNAME = "admin";
    static final Locale RUSSIAN = Locale.of("ru", "RU");

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
    void showDateBeforeOpenTest() {
        /* Given */
        LocalDate date = LocalDate.now().minusMonths(3);
        TimeEntry created = createTimeEntry();
        TimeEntry timeEntry = dataManager.load(Id.of(created)).one();
        timeEntry.setDate(date);
        dataManager.save(timeEntry);

        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* When */
            viewBuilders.view(UiTestUtils.getCurrentView(), CalendarView.class)
                    .withViewConfigurer(view -> view.showDate(date))
                    .open();

            /* Then */
            CalendarView calendarView = UiTestUtils.getCurrentView();
            FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
            assertThat(calendar.getInitialDate()).isEqualTo(date);

            // The browser reports the dates of the month the calendar opened on
            LocalDate monthStart = date.withDayOfMonth(1);
            LocalDate monthEnd = monthStart.plusMonths(1);
            ComponentUtil.fireEvent(calendar,
                    new DatesSetEvent(calendar, true, monthStart.minusDays(7), monthEnd.plusDays(7),
                            new DisplayModeInfo(monthStart.minusDays(7), monthEnd.plusDays(7), monthStart, monthEnd,
                                    CalendarDisplayModes.DAY_GRID_MONTH)));
            assertThat(findCalendarEvent(calendarView, timeEntry)).isNotNull();
        });
    }

    @Test
    void showDateAfterOpenTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            CalendarView calendarView = openCalendar();
            FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
            // Drops what opening the calendar queued for the browser
            browserCalls(calendar);
            LocalDate date = LocalDate.now().plusMonths(1);

            /* When */
            calendarView.showDate(date);

            /* Then */
            assertThat(browserCalls(calendar))
                    .anyMatch(call -> call.contains("navigateToDate") && call.contains("$0"));
        });
    }

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
    void movingRejectedEntrySendsItToNew() {
        /* Given */
        TimeEntry timeEntry = createTimeEntry();
        TimeEntry rejected = dataManager.load(Id.of(timeEntry)).one();
        rejected.setStatus(TimeEntryStatus.REJECTED);
        rejected.setRejectionReason("Wrong day");
        dataManager.save(rejected);

        systemAuthenticator.runWithUser(USERNAME, () -> {
            CalendarView calendarView = openCalendar();

            /* When */
            moveTimeEntry(calendarView, timeEntry, DAY.plusDays(1));

            /* Then */
            assertThat(findCalendarEvent(calendarView, timeEntry).getEntity().getStatus())
                    .isEqualTo(TimeEntryStatus.NEW);
        });

        TimeEntry saved = dataManager.load(Id.of(timeEntry)).one();
        assertThat(saved.getDate()).isEqualTo(DAY.plusDays(1));
        assertThat(saved.getStatus()).isEqualTo(TimeEntryStatus.NEW);
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
            showJanuary(calendarView);

            /* Then */
            assertThat(getDayMarks(calendarView)).extracting(CalendarUiTest::dateOf)
                    .allMatch(date -> !date.isBefore(JANUARY_GRID_START))
                    .contains(LocalDate.of(2025, 2, 1));
        });
    }

    @Test
    void absenceBarsShowOnTheirDaysTest() {
        /* Given */
        Absence absence = absent();

        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* When */
            CalendarView calendarView = openCalendar();

            /* Then */
            assertThat(getAbsenceBars(calendarView)).singleElement()
                    .extracting(CalendarEvent::getId, CalendarEvent::getTitle,
                            CalendarEvent::getStartDateTime, CalendarEvent::getEndDateTime)
                    .containsExactly(absence.getId() + "/" + ABSENCE_FIRST_DAY, ABSENCE_TYPE_NAME,
                            ABSENCE_FIRST_DAY.atStartOfDay(), ABSENCE_LAST_DAY.plusDays(1).atStartOfDay());
            assertAbsenceDaysShaded(calendarView);
        });
    }

    @Test
    void refreshShowsAndDropsAbsenceBarsTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* Given */
            CalendarView calendarView = openCalendar();
            assertThat(getAbsenceBars(calendarView)).isEmpty();

            /* When */
            // Saved after the calendar has loaded its plan
            Absence absence = absent();
            refresh(calendarView);

            /* Then */
            assertThat(getAbsenceBars(calendarView)).extracting(CalendarEvent::getId)
                    .containsExactly(absence.getId() + "/" + ABSENCE_FIRST_DAY);
            assertAbsenceDaysShaded(calendarView);

            /* When */
            remove(absence);
            refresh(calendarView);

            /* Then */
            assertThat(getAbsenceBars(calendarView)).isEmpty();
            assertAbsenceDaysNotShaded(calendarView);

            /* When */
            Absence nextAbsence = absent();
            showJanuary(calendarView);
            showDecember(calendarView);

            /* Then */
            assertThat(getAbsenceBars(calendarView)).extracting(CalendarEvent::getId)
                    .containsExactly(nextAbsence.getId() + "/" + ABSENCE_FIRST_DAY);
            assertAbsenceDaysShaded(calendarView);

            /* When */
            remove(nextAbsence);
            showJanuary(calendarView);
            showDecember(calendarView);

            /* Then */
            assertThat(getAbsenceBars(calendarView)).isEmpty();
            assertAbsenceDaysNotShaded(calendarView);
        });
    }

    @Test
    void clickingADayMarkDoesNothingTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* Given */
            CalendarView calendarView = openCalendar();
            // Sunday, Dec 1: the default schedule plans no weekend day
            CalendarEvent dayMark = getDayMarks(calendarView).getFirst();

            /* When */
            FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
            ComponentUtil.fireEvent(calendar,
                    new EventClickEvent(calendar, true, null, dayMark, getDayMarksProvider(calendarView),
                            monthDisplayModeInfo()));

            /* Then */
            assertThat(UiTestUtils.<View<?>>getCurrentView()).isSameAs(calendarView);
            assertThat(UiTestUtils.getOpenedViewDialogs()).isEmpty();
            assertThat(UiTestUtils.getOpenedDialogs()).isEmpty();
        });
    }

    @Test
    void monthBarIsNamedTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* When */
            CalendarView calendarView = openCalendar();

            /* Then */
            assertThat(getMonthBarName(calendarView)).isEqualTo("Logged of the month target");
        });
    }

    @Test
    void weekBarsAreNamedByTheirDatesTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* When */
            CalendarView calendarView = openCalendar();

            /* Then */
            // Weeks start on Sunday in English, and a week counts only the days of the month
            assertThat(getWeekBarNames(calendarView)).containsExactly(
                    "Week 01/12/2024 – 07/12/2024: logged of the plan",
                    "Week 08/12/2024 – 14/12/2024: logged of the plan",
                    "Week 15/12/2024 – 21/12/2024: logged of the plan",
                    "Week 22/12/2024 – 28/12/2024: logged of the plan",
                    "Week 29/12/2024 – 31/12/2024: logged of the plan");
        });
    }

    @Test
    void barsAreNamedInTheUserLanguageTest() {
        systemAuthenticator.runWithUser(USERNAME, () -> {
            /* Given */
            ((AbstractAuthenticationToken) Objects.requireNonNull(SecurityContextHelper.getAuthentication()))
                    .setDetails(ClientDetails.builder().locale(RUSSIAN).build());

            /* When */
            viewBuilders.view(UiTestUtils.getCurrentView(), CalendarView.class).open();
            CalendarView calendarView = UiTestUtils.getCurrentView();
            // In Russian the browser's grid starts on Monday, so December 2024 runs from November 25 to January 5
            FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
            ComponentUtil.fireEvent(calendar,
                    new DatesSetEvent(calendar, true, MONDAY_GRID_START, MONDAY_GRID_END,
                            new DisplayModeInfo(MONDAY_GRID_START, MONDAY_GRID_END,
                                    GRID_START, GRID_START.plusMonths(1), CalendarDisplayModes.DAY_GRID_MONTH)));

            /* Then */
            assertThat(getMonthBarName(calendarView)).isEqualTo("Отработано от плана на месяц");
            // A week counts only the days of the month, so Sunday, Dec 1, is a week of one day
            assertThat(getWeekBarNames(calendarView)).containsExactly(
                    "Неделя 01.12.2024: отработано от плана",
                    "Неделя 02.12.2024 – 08.12.2024: отработано от плана",
                    "Неделя 09.12.2024 – 15.12.2024: отработано от плана",
                    "Неделя 16.12.2024 – 22.12.2024: отработано от плана",
                    "Неделя 23.12.2024 – 29.12.2024: отработано от плана",
                    "Неделя 30.12.2024 – 31.12.2024: отработано от плана");
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

    /**
     * Returns the calls to the calendar's browser side queued since the last check, as a response would send them.
     */
    private static List<String> browserCalls(FullCalendar calendar) {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == calendar.getElement().getNode())
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
    }

    private CalendarView openCalendar() {
        viewBuilders.view(UiTestUtils.getCurrentView(), CalendarView.class).open();
        CalendarView calendarView = UiTestUtils.getCurrentView();
        showDecember(calendarView);

        return calendarView;
    }

    // The browser reports the displayed dates, which makes the calendar load its plan and entries
    private void showDecember(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ComponentUtil.fireEvent(calendar,
                new DatesSetEvent(calendar, true, GRID_START, GRID_END, monthDisplayModeInfo()));
    }

    private void showJanuary(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ComponentUtil.fireEvent(calendar,
                new DatesSetEvent(calendar, true, JANUARY_GRID_START, JANUARY_GRID_END,
                        new DisplayModeInfo(JANUARY_GRID_START, JANUARY_GRID_END,
                                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 2, 1),
                                CalendarDisplayModes.DAY_GRID_MONTH)));
    }

    private void refresh(CalendarView calendarView) {
        JmixButton refreshBtn = UiTestUtils.getComponent(calendarView, "refreshBtn");
        refreshBtn.click();
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
        return getDayMarksProvider(calendarView).getItems();
    }

    private ListCalendarDataProvider getDayMarksProvider(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        return Objects.requireNonNull(
                calendar.getDataProvider(CalendarView.DAY_MARKS_PROVIDER_ID), "The calendar has no day marks");
    }

    private List<CalendarEvent> getAbsenceBars(CalendarView calendarView) {
        FullCalendar calendar = UiTestUtils.getComponent(calendarView, "calendar");
        ListCalendarDataProvider absencesProvider = Objects.requireNonNull(
                calendar.getDataProvider(CalendarView.ABSENCES_PROVIDER_ID), "The calendar has no absence bars");
        return absencesProvider.getItems();
    }

    private static String getMonthBarName(CalendarView calendarView) {
        ProgressBar monthPercentBar = UiTestUtils.getComponent(calendarView, "monthPercentBar");
        return monthPercentBar.getElement().getAttribute("aria-label");
    }

    // Each week card holds one bar
    private static List<String> getWeekBarNames(CalendarView calendarView) {
        FlexLayout weeksSummaryWrapper = UiTestUtils.getComponent(calendarView, "weeksSummaryWrapper");
        return weeksSummaryWrapper.getChildren()
                .flatMap(Component::getChildren)
                .filter(ProgressBar.class::isInstance)
                .map(progressBar -> progressBar.getElement().getAttribute("aria-label"))
                .toList();
    }

    // The days of the absence plan nothing, so each is marked as a day without a plan
    private void assertAbsenceDaysShaded(CalendarView calendarView) {
        assertThat(getDayMarks(calendarView))
                .filteredOn(dayMark -> isAbsenceDay(dateOf(dayMark)))
                .extracting(CalendarUiTest::dateOf, CalendarEvent::getClassNames)
                .containsExactly(
                        tuple(ABSENCE_FIRST_DAY, "calendar-view-unplanned-day"),
                        tuple(ABSENCE_FIRST_DAY.plusDays(1), "calendar-view-unplanned-day"),
                        tuple(ABSENCE_LAST_DAY, "calendar-view-unplanned-day"));
    }

    private void assertAbsenceDaysNotShaded(CalendarView calendarView) {
        assertThat(getDayMarks(calendarView)).extracting(CalendarUiTest::dateOf)
                .noneMatch(CalendarUiTest::isAbsenceDay);
    }

    private static boolean isAbsenceDay(LocalDate date) {
        return !date.isBefore(ABSENCE_FIRST_DAY) && !date.isAfter(ABSENCE_LAST_DAY);
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

    // Saves an absence of admin from ABSENCE_FIRST_DAY to ABSENCE_LAST_DAY
    private Absence absent() {
        AbsenceType type = dataManager.create(AbsenceType.class);
        type.setName(ABSENCE_TYPE_NAME);

        Absence absence = dataManager.create(Absence.class);
        absence.setUser(loadUser());
        absence.setType(type);
        absence.setFirstDay(ABSENCE_FIRST_DAY);
        absence.setLastDay(ABSENCE_LAST_DAY);

        // The order lets the teardown remove each before what it refers to
        List<Object> created = List.of(type, absence);
        entities.addAll(created);
        dataManager.saveAll(created);

        return absence;
    }

    private void remove(Absence absence) {
        dataManager.remove(dataManager.load(Id.of(absence)).one());
    }

    private User loadUser() {
        return dataManager.load(User.class)
                .query("e.username = ?1", USERNAME)
                .one();
    }
}
