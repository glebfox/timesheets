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
import com.glebfox.timesheets.app.util.DaySummary;
import com.glebfox.timesheets.app.util.FactAndPlan;
import com.glebfox.timesheets.app.util.MyWeek;
import com.glebfox.timesheets.app.util.ShortDay;
import com.glebfox.timesheets.app.util.WeekStatus;
import com.glebfox.timesheets.app.util.WeekSummary;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Computes what the dashboard shows of the current user's own time: this week, the recent weeks, and what needs
 * attention. The current user is the effective user, the substituted one when a substitution is active.
 * Weeks are whole, across month and year boundaries.
 */
@Component("ts_DashboardSupport")
public class DashboardSupport {

    /**
     * How many weeks, the current one included, the recent weeks and the short weeks cover.
     */
    public static final int RECENT_WEEKS = 8;

    private final DataManager dataManager;
    private final WorkPlanSupport workPlanSupport;
    private final TimeEntrySupport timeEntrySupport;
    private final CurrentUserSubstitution currentUserSubstitution;

    public DashboardSupport(DataManager dataManager,
                            WorkPlanSupport workPlanSupport,
                            TimeEntrySupport timeEntrySupport,
                            CurrentUserSubstitution currentUserSubstitution) {
        this.dataManager = dataManager;
        this.workPlanSupport = workPlanSupport;
        this.timeEntrySupport = timeEntrySupport;
        this.currentUserSubstitution = currentUserSubstitution;
    }

    /**
     * Loads the week that contains the day, with each of its seven days.
     *
     * @param today          the day to find the week of
     * @param firstDayOfWeek the day the weeks start on
     * @return the week
     */
    public MyWeek loadMyWeek(LocalDate today, DayOfWeek firstDayOfWeek) {
        LocalDate start = weekStart(today, firstDayOfWeek);
        List<DaySummary> days = loadDays(start, start.plusWeeks(1));
        return new MyWeek(toWeek(days.subList(0, 7)), days);
    }

    /**
     * Loads whole weeks, the last one containing the day.
     *
     * @param today          the day the last week contains
     * @param firstDayOfWeek the day the weeks start on
     * @param count          how many weeks to load
     * @return the weeks, oldest first, each of seven days whatever month or year the days belong to
     */
    public List<WeekSummary> loadRecentWeeks(LocalDate today, DayOfWeek firstDayOfWeek, int count) {
        LocalDate end = weekStart(today, firstDayOfWeek).plusWeeks(1);
        return toWeeks(loadDays(end.minusWeeks(count), end));
    }

    /**
     * Loads what needs attention: the rejected entries, the days of the current and the previous week
     * before the day that are short, and the recent weeks that are short.
     *
     * @param today          the day to look back from; it is never a short day
     * @param firstDayOfWeek the day the weeks start on
     * @return the figures, with no truncation
     */
    public Attention loadAttention(LocalDate today, DayOfWeek firstDayOfWeek) {
        LocalDate end = weekStart(today, firstDayOfWeek).plusWeeks(1);
        List<DaySummary> days = loadDays(end.minusWeeks(RECENT_WEEKS), end);

        LocalDate shortDaysStart = end.minusWeeks(2);
        List<ShortDay> shortDays = days.stream()
                .filter(day -> !day.date().isBefore(shortDaysStart) && day.date().isBefore(today))
                .filter(day -> day.total().isInsufficient())
                .map(day -> new ShortDay(day.date(), WorkTime.ofMinutes(day.total().difference().toMinutes())))
                .toList()
                .reversed();
        List<WeekSummary> shortWeeks = toWeeks(days).reversed().stream()
                .filter(week -> week.getStatus(today) == WeekStatus.SHORT)
                .toList();

        return new Attention(timeEntrySupport.getRejectedCount(), shortDays, shortWeeks);
    }

    private static LocalDate weekStart(LocalDate date, DayOfWeek firstDayOfWeek) {
        return date.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
    }

    private static List<WeekSummary> toWeeks(List<DaySummary> days) {
        List<WeekSummary> weeks = new ArrayList<>();
        for (int i = 0; i < days.size(); i += 7) {
            weeks.add(toWeek(days.subList(i, i + 7)));
        }
        return weeks;
    }

    private static WeekSummary toWeek(List<DaySummary> weekDays) {
        FactAndPlan total = weekDays.stream()
                .map(DaySummary::total)
                .reduce(FactAndPlan.ZERO, FactAndPlan::plus);
        return new WeekSummary(weekDays.getFirst().date(), weekDays.getLast().date(), total);
    }

    /**
     * Loads the days of the range with one plan load and one entries load.
     */
    private List<DaySummary> loadDays(LocalDate start, LocalDate endExclusive) {
        User user = (User) currentUserSubstitution.getEffectiveUser();
        WorkPlan plan = workPlanSupport.loadWorkPlan(user, start, endExclusive);

        // The constrained data manager: only the entries the user may read count
        Map<LocalDate, WorkTime> spentByDay = new HashMap<>();
        dataManager.load(TimeEntry.class)
                .query("select e from ts_TimeEntry e where e.user.id = :userId and e.date >= :start and e.date < :end")
                .parameter("userId", user.getId())
                .parameter("start", start)
                .parameter("end", endExclusive)
                .fetchPlan(FetchPlan.BASE)
                .list()
                .forEach(entry -> {
                    if (entry.getSpentTime() != null) {
                        spentByDay.merge(entry.getDate(), entry.getSpentTime(), WorkTime::plus);
                    }
                });

        return start.datesUntil(endExclusive)
                .map(date -> new DaySummary(date,
                        new FactAndPlan(spentByDay.getOrDefault(date, WorkTime.ZERO), plan.getPlannedTime(date)),
                        findOffReason(plan, date)))
                .toList();
    }

    @Nullable
    private static String findOffReason(WorkPlan plan, LocalDate date) {
        return plan.absences().stream()
                .filter(absence -> absence.includes(date))
                .findFirst()
                .map(WorkPlan.AbsencePeriod::typeName)
                .orElseGet(() -> plan.findHolidayDay(date)
                        .filter(holiday -> holiday.type() == HolidayCalendarDayType.DAY_OFF)
                        .map(WorkPlan.HolidayDay::name)
                        .orElse(null));
    }
}
