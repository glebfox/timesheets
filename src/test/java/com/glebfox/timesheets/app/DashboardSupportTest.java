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

import com.glebfox.timesheets.app.util.Attention;
import com.glebfox.timesheets.app.util.ComingUp;
import com.glebfox.timesheets.app.util.DaySummary;
import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.app.util.MyWeek;
import com.glebfox.timesheets.app.util.ProjectTime;
import com.glebfox.timesheets.app.util.ShortDay;
import com.glebfox.timesheets.app.util.WeekSummary;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDay;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.core.usersubstitution.UserSubstitutionManager;
import io.jmix.data.PersistenceHints;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.securitydata.entity.UserSubstitutionEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;

// The test user is on the seeded default schedule, "Full time": 8 hours Monday to Friday, no holiday calendar
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class DashboardSupportTest {

    static final DayOfWeek MONDAY = DayOfWeek.MONDAY;

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserSubstitutionManager userSubstitutionManager;
    @Autowired
    DashboardSupport dashboardSupport;

    String prefix;
    LocalDate today;
    Project project;
    Task task;
    User user;
    User other;

    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        prefix = "dashboard-" + UUID.randomUUID().toString().substring(0, 8);
        today = LocalDate.now();

        Client client = save(dataManager.create(Client.class), c -> c.setName(prefix + " client"));
        TaskType taskType = save(dataManager.create(TaskType.class), t -> t.setName(prefix + " task type"));
        project = save(dataManager.create(Project.class), p -> {
            p.setName(prefix + " project");
            p.setClient(client);
        });
        task = save(dataManager.create(Task.class), t -> {
            t.setName(prefix + " task");
            t.setProject(project);
            t.setType(taskType);
        });
        user = saveUser("user");
        other = saveUser("other");
    }

    @AfterEach
    void tearDown() {
        // Soft-deleted rows and role assignments are removed for good, reloaded by id, children before parents
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    @Test
    void myWeekSumsDaysAgainstPlanTest() {
        LocalDate monday = weekStart(today, MONDAY);
        saveEntry(user, monday, 8);
        saveEntry(user, monday.plusDays(1), 3);
        saveEntry(user, monday.plusDays(1), 1);

        MyWeek week = asUser(() -> dashboardSupport.loadMyWeek(today, MONDAY));

        assertThat(week.days()).hasSize(7);
        assertThat(week.days()).extracting(DaySummary::date)
                .containsExactlyElementsOf(monday.datesUntil(monday.plusDays(7)).toList());
        assertThat(week.days().get(0).total()).isEqualTo(new FactAndPlan(hours(8), hours(8)));
        assertThat(week.days().get(1).total()).isEqualTo(new FactAndPlan(hours(4), hours(8)));
        assertThat(week.days().get(2).total()).isEqualTo(new FactAndPlan(hours(0), hours(8)));
        assertThat(week.days().get(5).total()).isEqualTo(FactAndPlan.ZERO);
        assertThat(week.days()).extracting(DaySummary::offReason).containsOnlyNulls();
        assertThat(week.summary()).isEqualTo(new WeekSummary(monday, monday.plusDays(6),
                new FactAndPlan(hours(12), hours(40))));
    }

    @Test
    void myWeekNamesAbsenceDayTest() {
        LocalDate monday = weekStart(today, MONDAY);
        AbsenceType type = save(dataManager.create(AbsenceType.class), t -> t.setName(prefix + " vacation"));
        save(dataManager.create(Absence.class), a -> {
            a.setUser(user);
            a.setType(type);
            a.setFirstDay(monday.plusDays(2));
            a.setLastDay(monday.plusDays(2));
        });

        MyWeek week = asUser(() -> dashboardSupport.loadMyWeek(today, MONDAY));

        DaySummary wednesday = week.days().get(2);
        assertThat(wednesday.offReason()).isEqualTo(type.getName());
        assertThat(wednesday.total().plan()).isEqualTo(WorkTime.ZERO);
        assertThat(week.days().get(1).offReason()).isNull();
        assertThat(week.summary().total().plan()).isEqualTo(hours(32));
    }

    @Test
    void recentWeeksAcrossYearBoundaryTest() {
        LocalDate januaryDay = LocalDate.of(today.getYear() + 1, 1, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

        List<WeekSummary> weeks = asUser(() -> dashboardSupport.loadRecentWeeks(januaryDay, MONDAY, 8));

        assertWholeWeeks(weeks, 8, MONDAY);
        assertThat(weeks.getLast().isCurrent(januaryDay)).isTrue();
        LocalDate newYearsEve = LocalDate.of(today.getYear(), 12, 31);
        LocalDate newYearsDay = newYearsEve.plusDays(1);
        // Each of the two days sits in a week counted whole, whether or not it is the same week
        assertThat(weeks).filteredOn(w -> w.isCurrent(newYearsEve)).singleElement()
                .satisfies(w -> assertThat(ChronoUnit.DAYS.between(w.firstDay(), w.lastDay())).isEqualTo(6));
        assertThat(weeks).filteredOn(w -> w.isCurrent(newYearsDay)).singleElement()
                .satisfies(w -> assertThat(ChronoUnit.DAYS.between(w.firstDay(), w.lastDay())).isEqualTo(6));
        assertThat(weeks.getLast().total().plan()).isEqualTo(hours(40));
    }

    @Test
    void recentWeeksSundayStartTest() {
        LocalDate januaryDay = LocalDate.of(today.getYear() + 1, 1, 1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.WEDNESDAY));

        List<WeekSummary> weeks = asUser(() -> dashboardSupport.loadRecentWeeks(januaryDay, DayOfWeek.SUNDAY, 8));

        assertWholeWeeks(weeks, 8, DayOfWeek.SUNDAY);
        assertThat(weeks.getLast().isCurrent(januaryDay)).isTrue();
        MyWeek week = asUser(() -> dashboardSupport.loadMyWeek(januaryDay, DayOfWeek.SUNDAY));
        assertThat(week.summary().firstDay().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        assertThat(week.summary().lastDay().getDayOfWeek()).isEqualTo(DayOfWeek.SATURDAY);
    }

    @Test
    void attentionListsShortDaysAndWeeksTest() {
        LocalDate previousTuesday = weekStart(today, MONDAY).minusWeeks(1).plusDays(1);
        saveEntry(user, previousTuesday, 4);

        Attention attention = asUser(() -> dashboardSupport.loadAttention(today, MONDAY));

        assertThat(attention.shortDays()).contains(new ShortDay(previousTuesday, hours(4)));
        assertThat(attention.shortDays()).extracting(ShortDay::date)
                .doesNotContain(today)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(attention.shortWeeks()).extracting(WeekSummary::firstDay)
                .contains(weekStart(previousTuesday, MONDAY))
                .isSortedAccordingTo(java.util.Comparator.reverseOrder());
        assertThat(attention.shortWeeks()).noneMatch(w -> w.isCurrent(today));
    }

    @Test
    void freshUserHasPlanOnlyTest() {
        MyWeek week = asUser(() -> dashboardSupport.loadMyWeek(today, MONDAY));
        Attention attention = asUser(() -> dashboardSupport.loadAttention(today, MONDAY));

        assertThat(week.days()).allSatisfy(day -> {
            assertThat(day.total().fact()).isEqualTo(WorkTime.ZERO);
            boolean workday = day.date().getDayOfWeek().getValue() <= 5;
            assertThat(day.total().plan()).isEqualTo(workday ? hours(8) : WorkTime.ZERO);
        });
        assertThat(week.summary().total()).isEqualTo(new FactAndPlan(WorkTime.ZERO, hours(40)));
        assertThat(attention.rejectedCount()).isZero();
        LocalDate previousWeekStart = weekStart(today, MONDAY).minusWeeks(1);
        List<LocalDate> pastWorkdays = previousWeekStart.datesUntil(today)
                .filter(d -> d.getDayOfWeek().getValue() <= 5)
                .toList();
        assertThat(attention.shortDays()).extracting(ShortDay::date)
                .containsExactlyInAnyOrderElementsOf(pastWorkdays);
        assertThat(attention.shortDays()).allSatisfy(d -> assertThat(d.missing()).isEqualTo(hours(8)));
        // The seven previous weeks are over and short; the current one is not over
        assertThat(attention.shortWeeks()).hasSize(7);
    }

    @Test
    void onlyOwnEntriesTest() {
        LocalDate monday = weekStart(today, MONDAY);
        saveEntry(user, monday, 2);
        saveEntry(other, monday, 8);
        saveEntry(other, monday.plusDays(1), 8);

        MyWeek week = asUser(() -> dashboardSupport.loadMyWeek(today, MONDAY));

        assertThat(week.days().get(0).total().fact()).isEqualTo(hours(2));
        assertThat(week.days().get(1).total().fact()).isEqualTo(WorkTime.ZERO);
        assertThat(week.summary().total().fact()).isEqualTo(hours(2));
    }

    @Test
    void timeByProjectOrdersLargestFirstTest() {
        Project second = saveProject("second");
        Task secondTask = saveTask(second);
        LocalDate thisMonth = today.withDayOfMonth(1);
        LocalDate lastMonth = thisMonth.minusMonths(1);
        saveEntry(user, thisMonth, 6);
        saveEntry(user, secondTask, thisMonth.plusDays(1), 10);
        saveEntry(user, lastMonth, 3);
        saveEntry(other, thisMonth, 9);

        List<ProjectTime> times = asUser(() -> dashboardSupport.loadTimeByProject(YearMonth.from(today)));

        assertThat(times).extracting(t -> t.project().getId(), ProjectTime::time)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(second.getId(), hours(10)),
                        org.assertj.core.groups.Tuple.tuple(project.getId(), hours(6)));
    }

    @Test
    void timeByProjectBreaksTiesByNameTest() {
        Project alpha = saveProject("a");
        Project beta = saveProject("b");
        LocalDate day = today.withDayOfMonth(1);
        saveEntry(user, saveTask(beta), day, 2);
        saveEntry(user, saveTask(alpha), day, 2);

        List<ProjectTime> times = asUser(() -> dashboardSupport.loadTimeByProject(YearMonth.from(today)));

        assertThat(times).extracting(t -> t.project().getName())
                .containsExactly(alpha.getName(), beta.getName());
    }

    @Test
    void comingUpListsFutureAbsencesAndHolidaysTest() {
        AbsenceType type = save(dataManager.create(AbsenceType.class), t -> t.setName(prefix + " leave"));
        saveAbsence(type, today.minusDays(5), today.minusDays(1));
        Absence upcoming = saveAbsence(type, today.plusDays(7), today.plusDays(9));
        Absence current = saveAbsence(type, today.minusDays(1), today);
        HolidayCalendar calendar = assignCalendar();
        LocalDate dayOff = today.plusMonths(1);
        saveHoliday(calendar, dayOff, HolidayCalendarDayType.DAY_OFF, "Day off next month");
        saveHoliday(calendar, dayOff.plusDays(1), HolidayCalendarDayType.WORKDAY, "Workday next month");
        saveHoliday(calendar, today.plusMonths(7), HolidayCalendarDayType.DAY_OFF, "Beyond the horizon");

        ComingUp comingUp = asUser(() -> dashboardSupport.loadComingUp(today));

        assertThat(comingUp.absences()).extracting(WorkPlan.AbsencePeriod::id)
                .containsExactly(current.getId(), upcoming.getId());
        assertThat(comingUp.holidays()).containsExactly(new ComingUp.Holiday(dayOff, "Day off next month"));
    }

    @Test
    void comingUpHolidaysAreNearestFirstTest() {
        HolidayCalendar calendar = assignCalendar();
        saveHoliday(calendar, today.plusDays(40), HolidayCalendarDayType.DAY_OFF, "Later");
        saveHoliday(calendar, today, HolidayCalendarDayType.DAY_OFF, "Today");
        saveHoliday(calendar, today.plusDays(10), HolidayCalendarDayType.DAY_OFF, "Sooner");

        ComingUp comingUp = asUser(() -> dashboardSupport.loadComingUp(today));

        assertThat(comingUp.holidays()).extracting(ComingUp.Holiday::name)
                .containsExactly("Today", "Sooner", "Later");
        assertThat(comingUp.absences()).isEmpty();
    }

    @Test
    void myWeekNamesHolidayAndAbsenceTakesPrecedenceTest() {
        LocalDate monday = weekStart(today, MONDAY);
        HolidayCalendar calendar = assignCalendar();
        saveHoliday(calendar, monday.plusDays(1), HolidayCalendarDayType.DAY_OFF, "Tuesday holiday");
        saveHoliday(calendar, monday.plusDays(2), HolidayCalendarDayType.DAY_OFF, "Wednesday holiday");
        AbsenceType type = save(dataManager.create(AbsenceType.class), t -> t.setName(prefix + " leave"));
        saveAbsence(type, monday.plusDays(2), monday.plusDays(2));

        MyWeek week = asUser(() -> dashboardSupport.loadMyWeek(today, MONDAY));

        assertThat(week.days().get(1).offReason()).isEqualTo("Tuesday holiday");
        assertThat(week.days().get(1).total().plan()).isEqualTo(WorkTime.ZERO);
        assertThat(week.days().get(2).offReason()).isEqualTo(type.getName());
        assertThat(week.days().get(0).offReason()).isNull();
    }

    @Test
    void recentEntriesNewestDateFirstTest() {
        LocalDate day = today.minusDays(10);
        saveEntry(user, day, 1);
        saveEntry(user, day, 2);
        saveEntry(user, day.plusDays(1), 1);
        saveEntry(user, day.plusDays(1), 2);
        saveEntry(user, day.plusDays(2), 1);
        saveEntry(user, day.plusDays(3), 1);
        saveEntry(user, day.plusDays(4), 1);
        saveEntry(other, day.plusDays(5), 1);

        List<TimeEntry> entries = asUser(() -> dashboardSupport.loadRecentEntries(5));

        assertThat(entries).hasSize(5);
        assertThat(entries).extracting(TimeEntry::getDate)
                .isSortedAccordingTo(java.util.Comparator.reverseOrder())
                .startsWith(day.plusDays(4), day.plusDays(3), day.plusDays(2));
        assertThat(entries).allSatisfy(e -> {
            assertThat(e.getUser().getId()).isEqualTo(user.getId());
            assertThat(e.getTask().getProject().getName()).isEqualTo(project.getName());
            assertThat(e.getStatus()).isNotNull();
        });
    }

    @Test
    void substitutedUserDataTest() {
        LocalDate thisMonth = today.withDayOfMonth(1);
        saveEntry(user, thisMonth, 5);
        TimeEntry rejected = saveEntry(user, thisMonth.plusDays(1), 3);
        rejected.setStatus(TimeEntryStatus.REJECTED);
        dataManager.save(rejected);
        // Admin's own entry must not appear under the substitution
        saveEntry(admin(), thisMonth, 7);
        UserSubstitutionEntity substitution = dataManager.create(UserSubstitutionEntity.class);
        substitution.setUsername("admin");
        substitution.setSubstitutedUsername(user.getUsername());
        createdIds.add(Id.of(dataManager.save(substitution)));

        List<TimeEntry> entries = systemAuthenticator.withUser("admin", () -> {
            userSubstitutionManager.substituteUser(user.getUsername());
            return dashboardSupport.loadRecentEntries(5);
        });
        List<ProjectTime> times = systemAuthenticator.withUser("admin", () -> {
            userSubstitutionManager.substituteUser(user.getUsername());
            return dashboardSupport.loadTimeByProject(YearMonth.from(today));
        });
        Attention attention = systemAuthenticator.withUser("admin", () -> {
            userSubstitutionManager.substituteUser(user.getUsername());
            return dashboardSupport.loadAttention(today, MONDAY);
        });

        assertThat(entries).extracting(e -> e.getUser().getId()).containsOnly(user.getId());
        assertThat(entries).hasSize(2);
        assertThat(times).extracting(ProjectTime::time).containsExactly(hours(8));
        assertThat(attention.rejectedCount()).isEqualTo(1);
    }

    private static void assertWholeWeeks(List<WeekSummary> weeks, int count, DayOfWeek firstDayOfWeek) {
        assertThat(weeks).hasSize(count);
        for (int i = 0; i < weeks.size(); i++) {
            WeekSummary week = weeks.get(i);
            assertThat(week.firstDay().getDayOfWeek()).isEqualTo(firstDayOfWeek);
            assertThat(ChronoUnit.DAYS.between(week.firstDay(), week.lastDay())).isEqualTo(6);
            if (i > 0) {
                assertThat(week.firstDay()).isEqualTo(weeks.get(i - 1).lastDay().plusDays(1));
            }
        }
    }

    private static LocalDate weekStart(LocalDate date, DayOfWeek firstDayOfWeek) {
        return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
    }

    private static WorkTime hours(int hours) {
        return WorkTime.ofHours(hours);
    }

    private <T> T asUser(Supplier<T> action) {
        return systemAuthenticator.withUser(user.getUsername(), action::get);
    }

    private <E> E save(E entity, Consumer<E> init) {
        init.accept(entity);
        E saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private User saveUser(String name) {
        User saved = save(dataManager.create(User.class), u -> u.setUsername(prefix + "-" + name));
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(saved.getUsername());
        assignment.setRoleCode(FullAccessRole.CODE);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
        return saved;
    }

    private User admin() {
        return dataManager.load(User.class).query("e.username = :u").parameter("u", "admin").one();
    }

    private Project saveProject(String name) {
        return save(dataManager.create(Project.class), p -> {
            p.setName(prefix + " " + name);
            p.setClient(project.getClient());
        });
    }

    private Task saveTask(Project taskProject) {
        return save(dataManager.create(Task.class), t -> {
            t.setName(prefix + " task of " + taskProject.getName());
            t.setProject(taskProject);
            t.setType(task.getType());
        });
    }

    private Absence saveAbsence(AbsenceType type, LocalDate firstDay, LocalDate lastDay) {
        return save(dataManager.create(Absence.class), a -> {
            a.setUser(user);
            a.setType(type);
            a.setFirstDay(firstDay);
            a.setLastDay(lastDay);
        });
    }

    // Puts the test user on a Monday-to-Friday schedule with a new holiday calendar, from a year ago
    private HolidayCalendar assignCalendar() {
        HolidayCalendar calendar = save(dataManager.create(HolidayCalendar.class), c -> c.setName(prefix + " calendar"));
        WorkSchedule schedule = save(dataManager.create(WorkSchedule.class), s -> {
            s.setName(prefix + " schedule");
            s.setWorkdayTime(hours(8));
            s.setHolidayCalendar(calendar);
            s.setMonday(true);
            s.setTuesday(true);
            s.setWednesday(true);
            s.setThursday(true);
            s.setFriday(true);
            s.setSaturday(false);
            s.setSunday(false);
        });
        save(dataManager.create(WorkScheduleAssignment.class), a -> {
            a.setUser(user);
            a.setWorkSchedule(schedule);
            a.setStartDate(today.minusYears(1));
        });
        return calendar;
    }

    private void saveHoliday(HolidayCalendar calendar, LocalDate date, HolidayCalendarDayType type, String name) {
        save(dataManager.create(HolidayCalendarDay.class), d -> {
            d.setCalendar(calendar);
            d.setDate(date);
            d.setType(type);
            d.setName(name);
        });
    }

    private TimeEntry saveEntry(User entryUser, LocalDate date, int hours) {
        return saveEntry(entryUser, task, date, hours);
    }

    private TimeEntry saveEntry(User entryUser, Task entryTask, LocalDate date, int hours) {
        return save(dataManager.create(TimeEntry.class), e -> {
            e.setTask(entryTask);
            e.setUser(entryUser);
            e.setDate(date);
            e.setSpentTime(WorkTime.ofHours(hours));
            e.setStatus(TimeEntryStatus.NEW);
        });
    }
}
