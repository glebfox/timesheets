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

import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.THURSDAY;
import static java.time.DayOfWeek.TUESDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

// November 2026 starts on Sunday: Monday 11-02, Wednesday 11-04, Saturday 11-07, Monday 11-09,
// Monday 11-16, Saturday 11-21, Monday 11-30. The seeded default, "Full time", plans 8 hours
// Monday to Friday with no holiday calendar.
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class WorkPlanSupportTest {

    static final LocalDate NOVEMBER_START = LocalDate.of(2026, 11, 1);
    static final LocalDate NOVEMBER_END = LocalDate.of(2026, 12, 1);
    static final Set<DayOfWeek> MONDAY_TO_FRIDAY = EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY);

    @Autowired
    DataManager dataManager;
    @Autowired
    WorkPlanSupport workPlanSupport;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void userWithoutAssignmentsGetsTheDefault() {
        User user = createUser();

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(2))).isEqualTo(WorkTime.ofHours(8));
        assertThat(plan.isWorkday(november(2))).isTrue();
        assertThat(plan.getPlannedTime(november(7))).isEqualTo(WorkTime.ZERO);
        assertThat(plan.isWorkday(november(7))).isFalse();
        assertThat(plan.covers(NOVEMBER_START)).isTrue();
        assertThat(plan.covers(NOVEMBER_END)).isFalse();
        assertThat(plan.findHolidayDay(november(4))).isEmpty();
    }

    @Test
    void assignmentAppliesFromItsStartDate() {
        User user = createUser();
        assign(user, halfTime(), november(9));

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(6))).isEqualTo(WorkTime.ofHours(8));
        assertThat(plan.getPlannedTime(november(9))).isEqualTo(WorkTime.ofHours(4));
        assertThat(plan.getPlannedTime(november(30))).isEqualTo(WorkTime.ofHours(4));
    }

    @Test
    void assignmentStartedBeforeTheRangeApplies() {
        User user = createUser();
        WorkSchedule sixDays = createSchedule("Six days", WorkTime.ofHours(6), null,
                EnumSet.of(MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY));
        assign(user, sixDays, LocalDate.of(2025, 11, 1));
        assign(user, halfTime(), november(16));

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(2))).isEqualTo(WorkTime.ofHours(6));
        assertThat(plan.getPlannedTime(november(7))).isEqualTo(WorkTime.ofHours(6));
        assertThat(plan.getPlannedTime(november(16))).isEqualTo(WorkTime.ofHours(4));
        assertThat(plan.getPlannedTime(november(21))).isEqualTo(WorkTime.ZERO);
    }

    @Test
    void holidayCalendarOverridesTheWeek() {
        User user = createUser();
        assign(user, sevenHours().schedule(), LocalDate.of(2026, 10, 1));

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(4))).isEqualTo(WorkTime.ZERO);
        assertThat(plan.isWorkday(november(4))).isFalse();
        assertThat(plan.getPlannedTime(november(7))).isEqualTo(WorkTime.ofHours(7));
        assertThat(plan.isWorkday(november(7))).isTrue();
        assertThat(plan.getPlannedTime(november(5))).isEqualTo(WorkTime.ofHours(7));
    }

    @Test
    void holidayCalendarDaysCarryTheirNames() {
        User user = createUser();
        assign(user, sevenHours().schedule(), LocalDate.of(2026, 10, 1));

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.findHolidayDay(november(4)))
                .contains(new WorkPlan.HolidayDay("Unity Day", HolidayCalendarDayType.DAY_OFF));
        assertThat(plan.findHolidayDay(november(7)))
                .contains(new WorkPlan.HolidayDay("Working Saturday", HolidayCalendarDayType.WORKDAY));
        assertThat(plan.findHolidayDay(november(5))).isEmpty();
    }

    @Test
    void holidayNamesFollowTheScheduleInForce() {
        User user = createUser();
        assign(user, sevenHours().schedule(), LocalDate.of(2026, 10, 1));
        HolidayCalendar otherCalendar = createCalendar("Other calendar");
        addDay(otherCalendar, november(3), HolidayCalendarDayType.DAY_OFF, "Before the switch");
        addDay(otherCalendar, november(5), HolidayCalendarDayType.DAY_OFF, "Other holiday");
        assign(user, createSchedule("Other schedule", WorkTime.ofHours(8), otherCalendar, MONDAY_TO_FRIDAY), november(5));

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.findHolidayDay(november(3))).isEmpty();
        assertThat(plan.findHolidayDay(november(4))).map(WorkPlan.HolidayDay::name).contains("Unity Day");
        assertThat(plan.findHolidayDay(november(5))).map(WorkPlan.HolidayDay::name).contains("Other holiday");
        assertThat(plan.getPlannedTime(november(5))).isEqualTo(WorkTime.ZERO);
        // The seven-hour calendar's working Saturday no longer applies after the switch
        assertThat(plan.findHolidayDay(november(7))).isEmpty();
        assertThat(plan.getPlannedTime(november(7))).isEqualTo(WorkTime.ZERO);
    }

    @Test
    void daysOfAnotherCalendarDoNotCount() {
        User user = createUser();
        assign(user, sevenHours().schedule(), LocalDate.of(2026, 10, 1));
        HolidayCalendar otherCalendar = createCalendar("Other calendar");
        addDay(otherCalendar, november(5), HolidayCalendarDayType.DAY_OFF, "Other holiday");

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(5))).isEqualTo(WorkTime.ofHours(7));
        assertThat(plan.findHolidayDay(november(5))).isEmpty();
    }

    @Test
    void deletedScheduleAndCalendarStillCount() {
        User user = createUser();
        SevenHours sevenHours = sevenHours();
        assign(user, sevenHours.schedule(), LocalDate.of(2026, 10, 1));
        remove(sevenHours.schedule());
        remove(sevenHours.calendar());

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(4))).isEqualTo(WorkTime.ZERO);
        assertThat(plan.getPlannedTime(november(7))).isEqualTo(WorkTime.ofHours(7));
        // From the deleted schedule, not the 8-hour default
        assertThat(plan.getPlannedTime(november(9))).isEqualTo(WorkTime.ofHours(7));
        assertThat(plan.findHolidayDay(november(4))).map(WorkPlan.HolidayDay::name).contains("Unity Day");
    }

    @Test
    void deletedAssignmentAndCalendarDayDoNotCount() {
        User user = createUser();
        SevenHours sevenHours = sevenHours();
        assign(user, sevenHours.schedule(), LocalDate.of(2026, 10, 1));
        WorkScheduleAssignment halfTimeAssignment = assign(user, halfTime(), november(9));
        remove(halfTimeAssignment);
        remove(sevenHours.unityDay());

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(9))).isEqualTo(WorkTime.ofHours(7));
        assertThat(plan.getPlannedTime(november(4))).isEqualTo(WorkTime.ofHours(7));
        assertThat(plan.findHolidayDay(november(4))).isEmpty();
    }

    @Test
    void planIgnoresCurrentUserPermissions() {
        // A user with no role assignments, so no permission on schedules, calendars, or assignments
        User user = createUser();
        assign(user, sevenHours().schedule(), LocalDate.of(2026, 10, 1));
        assign(user, halfTime(), november(9));

        WorkPlan plan = systemAuthenticator.withUser(user.getUsername(), () -> novemberPlan(user));

        assertThat(plan.getPlannedTime(november(4))).isEqualTo(WorkTime.ZERO);
        assertThat(plan.getPlannedTime(november(5))).isEqualTo(WorkTime.ofHours(7));
        assertThat(plan.getPlannedTime(november(9))).isEqualTo(WorkTime.ofHours(4));
    }

    @Test
    void scheduleWithoutWorkdaysPlansNothing() {
        User user = createUser();
        WorkSchedule noDays = createSchedule("No days", WorkTime.ofHours(8), null, EnumSet.noneOf(DayOfWeek.class));
        assign(user, noDays, november(9));

        WorkPlan plan = novemberPlan(user);

        assertThat(plan.getPlannedTime(november(6))).isEqualTo(WorkTime.ofHours(8));
        assertThat(plan.getPlannedTime(november(9))).isEqualTo(WorkTime.ZERO);
        assertThat(plan.getPlannedTime(november(10))).isEqualTo(WorkTime.ZERO);
    }

    @AfterEach
    void tearDown() {
        // Reload by id right before removal: soft deletes in the tests changed rows after they were saved
        for (Id<?> id : Lists.reverse(createdIds)) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    private WorkPlan novemberPlan(User user) {
        return workPlanSupport.loadWorkPlan(user, NOVEMBER_START, NOVEMBER_END);
    }

    private static LocalDate november(int day) {
        return LocalDate.of(2026, 11, day);
    }

    private User createUser() {
        User user = dataManager.create(User.class);
        user.setUsername("work-plan-test-" + System.nanoTime());
        return save(user);
    }

    private HolidayCalendar createCalendar(String name) {
        HolidayCalendar calendar = dataManager.create(HolidayCalendar.class);
        calendar.setName(name);
        return save(calendar);
    }

    private HolidayCalendarDay addDay(HolidayCalendar calendar, LocalDate date, HolidayCalendarDayType type,
                                      String name) {
        HolidayCalendarDay day = dataManager.create(HolidayCalendarDay.class);
        day.setCalendar(calendar);
        day.setDate(date);
        day.setType(type);
        day.setName(name);
        return save(day);
    }

    private WorkSchedule createSchedule(String name, WorkTime workdayTime, HolidayCalendar calendar,
                                        Set<DayOfWeek> workdays) {
        WorkSchedule schedule = dataManager.create(WorkSchedule.class);
        schedule.setName(name);
        schedule.setWorkdayTime(workdayTime);
        schedule.setHolidayCalendar(calendar);
        schedule.setMonday(workdays.contains(MONDAY));
        schedule.setTuesday(workdays.contains(TUESDAY));
        schedule.setWednesday(workdays.contains(WEDNESDAY));
        schedule.setThursday(workdays.contains(THURSDAY));
        schedule.setFriday(workdays.contains(FRIDAY));
        schedule.setSaturday(workdays.contains(SATURDAY));
        schedule.setSunday(workdays.contains(DayOfWeek.SUNDAY));
        return save(schedule);
    }

    private WorkSchedule halfTime() {
        return createSchedule("Half time", WorkTime.ofHours(4), null, MONDAY_TO_FRIDAY);
    }

    // 7 hours, Monday to Friday, with a calendar where Wednesday 11-04 is off and Saturday 11-07 is worked
    private SevenHours sevenHours() {
        HolidayCalendar calendar = createCalendar("Seven hours calendar");
        HolidayCalendarDay unityDay = addDay(calendar, november(4), HolidayCalendarDayType.DAY_OFF, "Unity Day");
        addDay(calendar, november(7), HolidayCalendarDayType.WORKDAY, "Working Saturday");
        WorkSchedule schedule = createSchedule("Seven hours", WorkTime.ofHours(7), calendar, MONDAY_TO_FRIDAY);
        return new SevenHours(schedule, calendar, unityDay);
    }

    private WorkScheduleAssignment assign(User user, WorkSchedule schedule, LocalDate startDate) {
        WorkScheduleAssignment assignment = dataManager.create(WorkScheduleAssignment.class);
        assignment.setUser(user);
        assignment.setWorkSchedule(schedule);
        assignment.setStartDate(startDate);
        return save(assignment);
    }

    private <T> T save(T entity) {
        T saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void remove(Object entity) {
        dataManager.remove(dataManager.load(Id.of(entity)).one());
    }

    private record SevenHours(WorkSchedule schedule, HolidayCalendar calendar, HolidayCalendarDay unityDay) {
    }
}
