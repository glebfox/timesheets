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
import com.google.common.base.Preconditions;
import io.jmix.core.FetchPlan;
import io.jmix.core.UnconstrainedDataManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves the time a user is planned to work from their work schedule assignments,
 * the default work schedule, and the holiday calendars of those schedules.
 */
@Component("ts_WorkPlanSupport")
public class WorkPlanSupport {

    // Loads ignore the current user's permissions: every user gets their plan
    private final UnconstrainedDataManager dataManager;

    public WorkPlanSupport(UnconstrainedDataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Loads the plan of a user for a date range. Each day takes the schedule of the user's
     * latest assignment that starts on or before it, or the default schedule before the first
     * assignment. A day of the schedule's holiday calendar decides whether the day is worked;
     * otherwise the schedule's days of the week do. A deleted schedule or holiday calendar still
     * counts for the assignments that refer to it; a deleted assignment or calendar day does not.
     *
     * @param user      the user to load the plan for
     * @param startDate the first day of the range (inclusive)
     * @param endDate   the end of the range (exclusive)
     * @return the planned time of every day of the range
     */
    public WorkPlan loadWorkPlan(User user, LocalDate startDate, LocalDate endDate) {
        Preconditions.checkArgument(!endDate.isBefore(startDate), "The range ends before it starts");

        NavigableMap<LocalDate, WorkSchedule> scheduleByStartDate = loadAssignments(user, endDate).stream()
                .collect(Collectors.toMap(WorkScheduleAssignment::getStartDate,
                        WorkScheduleAssignment::getWorkSchedule,
                        (earlier, later) -> later,
                        TreeMap::new));
        WorkSchedule defaultSchedule = loadDefaultSchedule();
        Map<UUID, Map<LocalDate, HolidayCalendarDayType>> holidayDaysByCalendar = loadHolidayDays(
                Stream.concat(scheduleByStartDate.values().stream(), Stream.of(defaultSchedule)),
                startDate, endDate);

        Map<LocalDate, WorkTime> plannedTimeByDay = new HashMap<>();
        startDate.datesUntil(endDate).forEach(date -> {
            Map.Entry<LocalDate, WorkSchedule> assigned = scheduleByStartDate.floorEntry(date);
            WorkSchedule schedule = assigned != null ? assigned.getValue() : defaultSchedule;
            plannedTimeByDay.put(date, getPlannedTime(schedule, date, holidayDaysByCalendar));
        });
        return new WorkPlan(plannedTimeByDay);
    }

    private List<WorkScheduleAssignment> loadAssignments(User user, LocalDate endDate) {
        // No lower bound: an assignment that started before the range governs its first days
        return dataManager.load(WorkScheduleAssignment.class)
                .query("""
                        select e from ts_WorkScheduleAssignment e
                        where e.user = :user and e.startDate < :endDate
                        order by e.startDate
                        """)
                .parameter("user", user)
                .parameter("endDate", endDate)
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("workSchedule", scheduleBuilder -> scheduleBuilder.addFetchPlan(FetchPlan.BASE)
                                .add("holidayCalendar", FetchPlan.INSTANCE_NAME)))
                .list();
    }

    private WorkSchedule loadDefaultSchedule() {
        return dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true")
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("holidayCalendar", FetchPlan.INSTANCE_NAME))
                .one();
    }

    private Map<UUID, Map<LocalDate, HolidayCalendarDayType>> loadHolidayDays(Stream<WorkSchedule> schedules,
                                                                              LocalDate startDate,
                                                                              LocalDate endDate) {
        Set<UUID> calendarIds = schedules
                .map(WorkSchedule::getHolidayCalendar)
                .filter(Objects::nonNull)
                .map(HolidayCalendar::getId)
                .collect(Collectors.toSet());

        Map<UUID, Map<LocalDate, HolidayCalendarDayType>> holidayDaysByCalendar = new HashMap<>();
        for (UUID calendarId : calendarIds) {
            holidayDaysByCalendar.put(calendarId, loadHolidayDays(calendarId, startDate, endDate));
        }
        return holidayDaysByCalendar;
    }

    private Map<LocalDate, HolidayCalendarDayType> loadHolidayDays(UUID calendarId,
                                                                   LocalDate startDate,
                                                                   LocalDate endDate) {
        // The condition on calendar.id compares the foreign key, so the days of a deleted calendar come back.
        // The calendar stays out of the fetch plan: fetching it joins the calendar table with the
        // soft deletion filter, which drops the days of a deleted calendar.
        return dataManager.load(HolidayCalendarDay.class)
                .query("""
                        select e from ts_HolidayCalendarDay e
                        where e.calendar.id = :calendarId
                            and e.date >= :startDate and e.date < :endDate
                        """)
                .parameter("calendarId", calendarId)
                .parameter("startDate", startDate)
                .parameter("endDate", endDate)
                .fetchPlan(FetchPlan.BASE)
                .list().stream()
                .collect(Collectors.toMap(HolidayCalendarDay::getDate, HolidayCalendarDay::getType));
    }

    private WorkTime getPlannedTime(WorkSchedule schedule,
                                    LocalDate date,
                                    Map<UUID, Map<LocalDate, HolidayCalendarDayType>> holidayDaysByCalendar) {
        HolidayCalendar calendar = schedule.getHolidayCalendar();
        HolidayCalendarDayType holidayDayType = calendar != null
                ? holidayDaysByCalendar.getOrDefault(calendar.getId(), Map.of()).get(date)
                : null;

        boolean workday = holidayDayType != null
                ? holidayDayType == HolidayCalendarDayType.WORKDAY
                : isWorkdayOfWeek(schedule, date);
        return workday ? schedule.getWorkdayTime() : WorkTime.ZERO;
    }

    private boolean isWorkdayOfWeek(WorkSchedule schedule, LocalDate date) {
        Boolean workday = switch (date.getDayOfWeek()) {
            case MONDAY -> schedule.getMonday();
            case TUESDAY -> schedule.getTuesday();
            case WEDNESDAY -> schedule.getWednesday();
            case THURSDAY -> schedule.getThursday();
            case FRIDAY -> schedule.getFriday();
            case SATURDAY -> schedule.getSaturday();
            case SUNDAY -> schedule.getSunday();
        };
        return Boolean.TRUE.equals(workday);
    }
}
