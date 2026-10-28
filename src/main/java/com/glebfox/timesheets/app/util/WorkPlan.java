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

package com.glebfox.timesheets.app.util;

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import com.google.common.base.Preconditions;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

/**
 * The time a user is planned to work on each day of a date range, and the holiday calendar day
 * that decided a date, if any.
 *
 * @param plannedTimeByDay the planned time of every day of the range, {@link WorkTime#ZERO} for a day off
 * @param holidayDayByDate the holiday calendar day that decided each date of the range that has one
 */
public record WorkPlan(Map<LocalDate, WorkTime> plannedTimeByDay, Map<LocalDate, HolidayDay> holidayDayByDate) {

    public WorkPlan {
        plannedTimeByDay = Map.copyOf(plannedTimeByDay);
        holidayDayByDate = Map.copyOf(holidayDayByDate);
        Preconditions.checkArgument(plannedTimeByDay.keySet().containsAll(holidayDayByDate.keySet()),
                "The plan has holiday calendar days outside its range");
    }

    /**
     * Creates a plan without holiday calendar days.
     *
     * @param plannedTimeByDay the planned time of every day of the range, {@link WorkTime#ZERO} for a day off
     */
    public WorkPlan(Map<LocalDate, WorkTime> plannedTimeByDay) {
        this(plannedTimeByDay, Map.of());
    }

    /**
     * @param date the date to check
     * @return {@code true} if the plan has the date
     */
    public boolean covers(LocalDate date) {
        return plannedTimeByDay.containsKey(date);
    }

    /**
     * Returns the time planned for the specified date.
     *
     * @param date a date the plan covers
     * @return the planned time, {@link WorkTime#ZERO} for a day off
     * @throws IllegalArgumentException if the plan does not cover the date
     */
    public WorkTime getPlannedTime(LocalDate date) {
        WorkTime plannedTime = plannedTimeByDay.get(date);
        Preconditions.checkArgument(plannedTime != null, "The plan does not cover %s", date);
        return plannedTime;
    }

    /**
     * Checks whether the specified date is a workday, that is, plans time.
     *
     * @param date a date the plan covers
     * @return {@code true} if more than zero time is planned for the date
     * @throws IllegalArgumentException if the plan does not cover the date
     */
    public boolean isWorkday(LocalDate date) {
        return getPlannedTime(date).compareTo(WorkTime.ZERO) > 0;
    }

    /**
     * Returns the holiday calendar day that decided the specified date.
     *
     * @param date a date the plan covers
     * @return the holiday calendar day, or empty if the schedule's days of the week decided the date
     * @throws IllegalArgumentException if the plan does not cover the date
     */
    public Optional<HolidayDay> findHolidayDay(LocalDate date) {
        Preconditions.checkArgument(covers(date), "The plan does not cover %s", date);
        return Optional.ofNullable(holidayDayByDate.get(date));
    }

    /**
     * A day of a holiday calendar as a plan shows it.
     *
     * @param name the name of the day, such as "Unity Day"
     * @param type whether the day is a day off or a workday
     */
    public record HolidayDay(String name, HolidayCalendarDayType type) {
    }
}
