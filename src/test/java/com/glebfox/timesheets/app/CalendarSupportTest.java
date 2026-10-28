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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.app.util.WeekSummary;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryCalendarWrapper;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.data.PersistenceHints;
import io.jmix.fullcalendarflowui.component.data.CalendarEvent;
import io.jmix.fullcalendarflowui.component.model.Display;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class CalendarSupportTest {

    // December 2024 starts on Sunday and ends on Tuesday; it has 22 workdays
    static final YearMonth DECEMBER_2024 = YearMonth.of(2024, 12);

    @Autowired
    DataManager dataManager;
    @Autowired
    CalendarSupport calendarSupport;
    @Autowired
    WorkPlanSupport workPlanSupport;
    @Autowired
    CurrentAuthentication currentAuthentication;

    List<Object> entities = new ArrayList<>();

    @Test
    void spentTimeByDayTest() {
        /* Given */
        LocalDate day1 = LocalDate.of(2024, 12, 2);
        LocalDate day2 = LocalDate.of(2024, 12, 3);

        List<TimeEntryCalendarWrapper> timeEntries = List.of(
                timeEntry(day1, WorkTime.ofMinutes(20)),
                timeEntry(day1, WorkTime.ofMinutes(20)),
                timeEntry(day1, WorkTime.ofMinutes(20)),
                timeEntry(day2, WorkTime.of(7, 45)),
                timeEntry(day2, WorkTime.ofMinutes(15))
        );

        /* When */
        Map<LocalDate, WorkTime> spentTimeByDay = calendarSupport.calculateSpentTimeByDay(timeEntries);

        /* Then */
        assertThat(spentTimeByDay).containsOnly(
                Map.entry(day1, WorkTime.ofHours(1)),
                Map.entry(day2, WorkTime.ofHours(8))
        );
    }

    @Test
    void weeksSummaryStartingOnMondayTest() {
        /* Given */
        Map<LocalDate, WorkTime> spentTimeByDay = calendarSupport.calculateSpentTimeByDay(decemberTimeEntries());

        /* When */
        List<WeekSummary> weeksSummary =
                calendarSupport.calculateWeeksSummary(DECEMBER_2024, DayOfWeek.MONDAY, spentTimeByDay,
                        loadDecemberPlan());
        FactAndPlan monthSummary = calendarSupport.calculateMonthSummary(weeksSummary);

        /* Then */
        assertThat(weeksSummary).extracting(WeekSummary::total).containsExactly(
                // Nov 25 - Dec 1: only Sunday, Dec 1, is in the month
                factAndPlan(WorkTime.ofHours(2), WorkTime.ZERO),
                // Dec 2 - Dec 8
                factAndPlan(WorkTime.ofHours(40), WorkTime.ofHours(40)),
                // Dec 9 - Dec 15
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                // Dec 16 - Dec 22
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                // Dec 23 - Dec 29
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                // Dec 30 - Jan 5: only Monday and Tuesday are in the month
                factAndPlan(WorkTime.of(1, 30), WorkTime.ofHours(16))
        );

        assertThat(weeksSummary.get(0).total().isExcess()).isTrue();
        assertThat(weeksSummary.get(1).total().isMatch()).isTrue();
        assertThat(weeksSummary.get(5).total().isInsufficient()).isTrue();

        // Only the days of the month, not of the whole calendar row
        assertThat(weeksSummary.get(0).firstDay()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(weeksSummary.get(0).lastDay()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(weeksSummary.get(5).firstDay()).isEqualTo(LocalDate.of(2024, 12, 30));
        assertThat(weeksSummary.get(5).lastDay()).isEqualTo(LocalDate.of(2024, 12, 31));

        assertThat(monthSummary).isEqualTo(factAndPlan(WorkTime.of(43, 30), WorkTime.ofHours(176)));
    }

    @Test
    void weeksSummaryStartingOnSundayTest() {
        /* Given */
        Map<LocalDate, WorkTime> spentTimeByDay = calendarSupport.calculateSpentTimeByDay(decemberTimeEntries());

        /* When */
        List<WeekSummary> weeksSummary =
                calendarSupport.calculateWeeksSummary(DECEMBER_2024, DayOfWeek.SUNDAY, spentTimeByDay,
                        loadDecemberPlan());
        FactAndPlan monthSummary = calendarSupport.calculateMonthSummary(weeksSummary);

        /* Then */
        assertThat(weeksSummary).extracting(WeekSummary::total).containsExactly(
                // Dec 1 - Dec 7
                factAndPlan(WorkTime.ofHours(42), WorkTime.ofHours(40)),
                // Dec 8 - Dec 14
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                // Dec 15 - Dec 21
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                // Dec 22 - Dec 28
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                // Dec 29 - Jan 4: only Sunday to Tuesday are in the month
                factAndPlan(WorkTime.of(1, 30), WorkTime.ofHours(16))
        );

        // Only the days of the month, not of the whole calendar row
        assertThat(weeksSummary.get(0).firstDay()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(weeksSummary.get(0).lastDay()).isEqualTo(LocalDate.of(2024, 12, 7));
        assertThat(weeksSummary.get(4).firstDay()).isEqualTo(LocalDate.of(2024, 12, 29));
        assertThat(weeksSummary.get(4).lastDay()).isEqualTo(LocalDate.of(2024, 12, 31));

        assertThat(monthSummary).isEqualTo(factAndPlan(WorkTime.of(43, 30), WorkTime.ofHours(176)));
    }

    @Test
    void weeksSummaryFollowsThePlanTest() {
        /* Given */
        Map<LocalDate, WorkTime> spentTimeByDay = calendarSupport.calculateSpentTimeByDay(decemberTimeEntries());
        // Monday, Dec 2 is a holiday and Saturday, Dec 7 is worked for 4 hours
        WorkPlan plan = decemberPlan(Map.of(
                LocalDate.of(2024, 12, 2), WorkTime.ZERO,
                LocalDate.of(2024, 12, 7), WorkTime.ofHours(4)));

        /* When */
        List<WeekSummary> weeksSummary =
                calendarSupport.calculateWeeksSummary(DECEMBER_2024, DayOfWeek.MONDAY, spentTimeByDay, plan);
        FactAndPlan monthSummary = calendarSupport.calculateMonthSummary(weeksSummary);

        /* Then */
        assertThat(weeksSummary).extracting(WeekSummary::total).containsExactly(
                factAndPlan(WorkTime.ofHours(2), WorkTime.ZERO),
                factAndPlan(WorkTime.ofHours(40), WorkTime.ofHours(36)),
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                factAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)),
                factAndPlan(WorkTime.of(1, 30), WorkTime.ofHours(16))
        );
        assertThat(monthSummary).isEqualTo(factAndPlan(WorkTime.of(43, 30), WorkTime.ofHours(172)));
    }

    @Test
    void weeksSummaryRequiresAPlanOfTheWholeMonthTest() {
        WorkPlan plan = new WorkPlan(Map.of(LocalDate.of(2024, 12, 2), WorkTime.ofHours(8)));

        assertThatThrownBy(() -> calendarSupport.calculateWeeksSummary(DECEMBER_2024, DayOfWeek.MONDAY, Map.of(), plan))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void dayMarksTest() {
        // Monday, Nov 2 to Sunday, Nov 8, 2026: Wednesday a holiday, Saturday a moved workday
        WorkPlan plan = new WorkPlan(
                Map.of(LocalDate.of(2026, 11, 2), WorkTime.ofHours(8),
                        LocalDate.of(2026, 11, 3), WorkTime.ofHours(8),
                        LocalDate.of(2026, 11, 4), WorkTime.ZERO,
                        LocalDate.of(2026, 11, 5), WorkTime.ofHours(8),
                        LocalDate.of(2026, 11, 6), WorkTime.ofHours(8),
                        LocalDate.of(2026, 11, 7), WorkTime.ofHours(8),
                        LocalDate.of(2026, 11, 8), WorkTime.ZERO),
                Map.of(LocalDate.of(2026, 11, 4), new WorkPlan.HolidayDay("Unity Day", HolidayCalendarDayType.DAY_OFF),
                        LocalDate.of(2026, 11, 7), new WorkPlan.HolidayDay("Working Saturday", HolidayCalendarDayType.WORKDAY)));

        List<CalendarEvent> marks = calendarSupport.createDayMarks(plan, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9));

        assertThat(marks).extracting(CalendarEvent::getStartDateTime, CalendarEvent::getClassNames,
                        CalendarEvent::getTitle, CalendarEvent::getDescription)
                .containsExactly(
                        tuple(LocalDate.of(2026, 11, 4).atStartOfDay(),
                                "calendar-view-unplanned-day calendar-view-holiday-day-off", "Unity Day", "Unity Day"),
                        tuple(LocalDate.of(2026, 11, 7).atStartOfDay(),
                                "calendar-view-holiday-workday", "Working Saturday", "Working Saturday"),
                        tuple(LocalDate.of(2026, 11, 8).atStartOfDay(), "calendar-view-unplanned-day", null, null));
        assertThat(marks).extracting(CalendarEvent::getDisplay).containsOnly(Display.BACKGROUND);
        assertThat(marks).extracting(CalendarEvent::getAllDay).containsOnly(true);

        // A day's mark keeps its id from one load to the next, and each day has an id of its own
        List<CalendarEvent> reloadedMarks =
                calendarSupport.createDayMarks(plan, LocalDate.of(2026, 11, 2), LocalDate.of(2026, 11, 9));
        assertThat(reloadedMarks).extracting(CalendarEvent::getId)
                .containsExactlyElementsOf(marks.stream().map(CalendarEvent::getId).toList());
        assertThat(marks).extracting(CalendarEvent::getId).doesNotHaveDuplicates();
    }

    @Test
    void loadTimeEntriesTest() {
        /* Given */
        User currentUser = (User) currentAuthentication.getUser();

        User otherUser = dataManager.create(User.class);
        otherUser.setUsername("calendar-test-user-" + System.currentTimeMillis());
        entities.add(otherUser);

        Task task = createTask();

        LocalDate start = LocalDate.of(2024, 11, 24);
        LocalDate end = LocalDate.of(2025, 1, 5);

        TimeEntry beforeStart = createTimeEntry(currentUser, task, start.minusDays(1));
        TimeEntry atStart = createTimeEntry(currentUser, task, start);
        TimeEntry beforeEnd = createTimeEntry(currentUser, task, end.minusDays(1));
        TimeEntry atEnd = createTimeEntry(currentUser, task, end);
        TimeEntry otherUsers = createTimeEntry(otherUser, task, start);

        dataManager.saveAll(entities);

        /* When */
        List<TimeEntryCalendarWrapper> loaded = calendarSupport.loadTimeEntries(start, end);

        /* Then */
        assertThat(loaded)
                .extracting(TimeEntryCalendarWrapper::getTimeEntry)
                .containsExactlyInAnyOrder(atStart, beforeEnd)
                .doesNotContain(beforeStart, atEnd, otherUsers);
    }

    @AfterEach
    void tearDown() {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }

        dataManager.save(
                new SaveContext()
                        .removing(Lists.reverse(entities))
                        .setHint(PersistenceHints.SOFT_DELETION, false)
        );
    }

    private List<TimeEntryCalendarWrapper> decemberTimeEntries() {
        List<TimeEntryCalendarWrapper> timeEntries = new ArrayList<>();

        // Outside the month
        timeEntries.add(timeEntry(LocalDate.of(2024, 11, 30), WorkTime.ofHours(5)));
        timeEntries.add(timeEntry(LocalDate.of(2025, 1, 1), WorkTime.ofHours(3)));

        // Sunday
        timeEntries.add(timeEntry(LocalDate.of(2024, 12, 1), WorkTime.ofHours(2)));

        // A full week, Monday to Friday, split into entries that do not add up as decimal hours
        for (int day = 2; day <= 6; day++) {
            timeEntries.add(timeEntry(LocalDate.of(2024, 12, day), WorkTime.of(7, 40)));
            timeEntries.add(timeEntry(LocalDate.of(2024, 12, day), WorkTime.ofMinutes(20)));
        }

        // The last day of the month
        timeEntries.add(timeEntry(LocalDate.of(2024, 12, 31), WorkTime.of(1, 30)));

        return timeEntries;
    }

    private WorkPlan loadDecemberPlan() {
        // admin has no assignments, so the default schedule plans the month: 8 hours Monday to Friday,
        // with no holiday calendar in the test profile
        return workPlanSupport.loadWorkPlan((User) currentAuthentication.getUser(),
                DECEMBER_2024.atDay(1), DECEMBER_2024.plusMonths(1).atDay(1));
    }

    private WorkPlan decemberPlan(Map<LocalDate, WorkTime> overrides) {
        Map<LocalDate, WorkTime> plannedTimeByDay = new HashMap<>();
        DECEMBER_2024.atDay(1).datesUntil(DECEMBER_2024.plusMonths(1).atDay(1)).forEach(day -> {
            boolean weekend = day.getDayOfWeek() == DayOfWeek.SATURDAY || day.getDayOfWeek() == DayOfWeek.SUNDAY;
            plannedTimeByDay.put(day, weekend ? WorkTime.ZERO : WorkTime.ofHours(8));
        });
        plannedTimeByDay.putAll(overrides);

        return new WorkPlan(plannedTimeByDay);
    }

    private TimeEntryCalendarWrapper timeEntry(LocalDate date, WorkTime spentTime) {
        TimeEntry timeEntry = dataManager.create(TimeEntry.class);
        timeEntry.setDate(date);
        timeEntry.setSpentTime(spentTime);

        return calendarSupport.wrapTimeEntry(timeEntry);
    }

    private FactAndPlan factAndPlan(WorkTime fact, WorkTime plan) {
        return new FactAndPlan(fact, plan);
    }

    private Task createTask() {
        Client client = dataManager.create(Client.class);
        client.setName("Calendar Test Client");
        entities.add(client);

        Project project = dataManager.create(Project.class);
        project.setName("Calendar Test Project");
        project.setClient(client);
        project.setStatus(ProjectStatus.OPEN);
        entities.add(project);

        TaskType taskType = dataManager.create(TaskType.class);
        taskType.setName("Calendar Test Task Type");
        entities.add(taskType);

        Task task = dataManager.create(Task.class);
        task.setName("Calendar Test Task");
        task.setProject(project);
        task.setType(taskType);
        task.setStatus(TaskStatus.ACTIVE);
        entities.add(task);

        return task;
    }

    private TimeEntry createTimeEntry(User user, Task task, LocalDate date) {
        TimeEntry timeEntry = dataManager.create(TimeEntry.class);
        timeEntry.setDate(date);
        timeEntry.setUser(user);
        timeEntry.setTask(task);
        timeEntry.setSpentTime(WorkTime.ofHours(1));
        timeEntry.setStatus(TimeEntryStatus.NEW);
        entities.add(timeEntry);

        return timeEntry;
    }
}
