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
import com.glebfox.timesheets.datatype.SpentTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectStatus;
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
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

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
    CurrentAuthentication currentAuthentication;

    List<Object> entities = new ArrayList<>();

    @Test
    void plannedTimeTest() {
        LocalDate monday = LocalDate.of(2024, 12, 2);

        for (int i = 0; i < 5; i++) {
            LocalDate workday = monday.plusDays(i);
            assertThat(calendarSupport.isWorkday(workday)).isTrue();
            assertThat(calendarSupport.getPlannedTime(workday)).isEqualTo(SpentTime.ofHours(8));
        }

        for (int i = 5; i < 7; i++) {
            LocalDate weekend = monday.plusDays(i);
            assertThat(calendarSupport.isWorkday(weekend)).isFalse();
            assertThat(calendarSupport.getPlannedTime(weekend)).isEqualTo(SpentTime.ZERO);
        }
    }

    @Test
    void spentTimeByDayTest() {
        /* Given */
        LocalDate day1 = LocalDate.of(2024, 12, 2);
        LocalDate day2 = LocalDate.of(2024, 12, 3);

        List<TimeEntryCalendarWrapper> timeEntries = List.of(
                timeEntry(day1, SpentTime.ofMinutes(20)),
                timeEntry(day1, SpentTime.ofMinutes(20)),
                timeEntry(day1, SpentTime.ofMinutes(20)),
                timeEntry(day2, SpentTime.of(7, 45)),
                timeEntry(day2, SpentTime.ofMinutes(15))
        );

        /* When */
        Map<LocalDate, SpentTime> spentTimeByDay = calendarSupport.calculateSpentTimeByDay(timeEntries);

        /* Then */
        assertThat(spentTimeByDay).containsOnly(
                Map.entry(day1, SpentTime.ofHours(1)),
                Map.entry(day2, SpentTime.ofHours(8))
        );
    }

    @Test
    void weeksSummaryStartingOnMondayTest() {
        /* Given */
        Map<LocalDate, SpentTime> spentTimeByDay = calendarSupport.calculateSpentTimeByDay(decemberTimeEntries());

        /* When */
        List<WeekSummary> weeksSummary =
                calendarSupport.calculateWeeksSummary(DECEMBER_2024, DayOfWeek.MONDAY, spentTimeByDay);
        FactAndPlan monthSummary = calendarSupport.calculateMonthSummary(weeksSummary);

        /* Then */
        assertThat(weeksSummary).extracting(WeekSummary::total).containsExactly(
                // Nov 25 - Dec 1: only Sunday, Dec 1, is in the month
                factAndPlan(SpentTime.ofHours(2), SpentTime.ZERO),
                // Dec 2 - Dec 8
                factAndPlan(SpentTime.ofHours(40), SpentTime.ofHours(40)),
                // Dec 9 - Dec 15
                factAndPlan(SpentTime.ZERO, SpentTime.ofHours(40)),
                // Dec 16 - Dec 22
                factAndPlan(SpentTime.ZERO, SpentTime.ofHours(40)),
                // Dec 23 - Dec 29
                factAndPlan(SpentTime.ZERO, SpentTime.ofHours(40)),
                // Dec 30 - Jan 5: only Monday and Tuesday are in the month
                factAndPlan(SpentTime.of(1, 30), SpentTime.ofHours(16))
        );

        assertThat(weeksSummary.get(0).total().isExcess()).isTrue();
        assertThat(weeksSummary.get(1).total().isMatch()).isTrue();
        assertThat(weeksSummary.get(5).total().isInsufficient()).isTrue();

        // Only the days of the month, not of the whole calendar row
        assertThat(weeksSummary.get(0).firstDay()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(weeksSummary.get(0).lastDay()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(weeksSummary.get(5).firstDay()).isEqualTo(LocalDate.of(2024, 12, 30));
        assertThat(weeksSummary.get(5).lastDay()).isEqualTo(LocalDate.of(2024, 12, 31));

        assertThat(monthSummary).isEqualTo(factAndPlan(SpentTime.of(43, 30), SpentTime.ofHours(176)));
    }

    @Test
    void weeksSummaryStartingOnSundayTest() {
        /* Given */
        Map<LocalDate, SpentTime> spentTimeByDay = calendarSupport.calculateSpentTimeByDay(decemberTimeEntries());

        /* When */
        List<WeekSummary> weeksSummary =
                calendarSupport.calculateWeeksSummary(DECEMBER_2024, DayOfWeek.SUNDAY, spentTimeByDay);
        FactAndPlan monthSummary = calendarSupport.calculateMonthSummary(weeksSummary);

        /* Then */
        assertThat(weeksSummary).extracting(WeekSummary::total).containsExactly(
                // Dec 1 - Dec 7
                factAndPlan(SpentTime.ofHours(42), SpentTime.ofHours(40)),
                // Dec 8 - Dec 14
                factAndPlan(SpentTime.ZERO, SpentTime.ofHours(40)),
                // Dec 15 - Dec 21
                factAndPlan(SpentTime.ZERO, SpentTime.ofHours(40)),
                // Dec 22 - Dec 28
                factAndPlan(SpentTime.ZERO, SpentTime.ofHours(40)),
                // Dec 29 - Jan 4: only Sunday to Tuesday are in the month
                factAndPlan(SpentTime.of(1, 30), SpentTime.ofHours(16))
        );

        // Only the days of the month, not of the whole calendar row
        assertThat(weeksSummary.get(0).firstDay()).isEqualTo(LocalDate.of(2024, 12, 1));
        assertThat(weeksSummary.get(0).lastDay()).isEqualTo(LocalDate.of(2024, 12, 7));
        assertThat(weeksSummary.get(4).firstDay()).isEqualTo(LocalDate.of(2024, 12, 29));
        assertThat(weeksSummary.get(4).lastDay()).isEqualTo(LocalDate.of(2024, 12, 31));

        assertThat(monthSummary).isEqualTo(factAndPlan(SpentTime.of(43, 30), SpentTime.ofHours(176)));
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
        timeEntries.add(timeEntry(LocalDate.of(2024, 11, 30), SpentTime.ofHours(5)));
        timeEntries.add(timeEntry(LocalDate.of(2025, 1, 1), SpentTime.ofHours(3)));

        // Sunday
        timeEntries.add(timeEntry(LocalDate.of(2024, 12, 1), SpentTime.ofHours(2)));

        // A full week, Monday to Friday, split into entries that do not add up as decimal hours
        for (int day = 2; day <= 6; day++) {
            timeEntries.add(timeEntry(LocalDate.of(2024, 12, day), SpentTime.of(7, 40)));
            timeEntries.add(timeEntry(LocalDate.of(2024, 12, day), SpentTime.ofMinutes(20)));
        }

        // The last day of the month
        timeEntries.add(timeEntry(LocalDate.of(2024, 12, 31), SpentTime.of(1, 30)));

        return timeEntries;
    }

    private TimeEntryCalendarWrapper timeEntry(LocalDate date, SpentTime spentTime) {
        TimeEntry timeEntry = dataManager.create(TimeEntry.class);
        timeEntry.setDate(date);
        timeEntry.setSpentTime(spentTime);

        return calendarSupport.wrapTimeEntry(timeEntry);
    }

    private FactAndPlan factAndPlan(SpentTime fact, SpentTime plan) {
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
        timeEntry.setSpentTime(SpentTime.ofHours(1));
        timeEntry.setStatus(TimeEntryStatus.NEW);
        entities.add(timeEntry);

        return timeEntry;
    }
}
