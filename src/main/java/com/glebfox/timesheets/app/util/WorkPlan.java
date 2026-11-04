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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * The time a user is planned to work on each day of a date range, the holiday calendar day of each date
 * that has one, and the user's absences that share a day with the range.
 *
 * @param plannedTimeByDay the planned time of every day of the range, {@link WorkTime#ZERO} for a day off
 * @param holidayDayByDate the holiday calendar day of each date of the range that has one, an absent date included
 * @param absences         the absences that share a day with the range, each whole, its days outside the range
 *                         included
 */
public record WorkPlan(Map<LocalDate, WorkTime> plannedTimeByDay, Map<LocalDate, HolidayDay> holidayDayByDate,
                       List<AbsencePeriod> absences) {

    public WorkPlan {
        plannedTimeByDay = Map.copyOf(plannedTimeByDay);
        holidayDayByDate = Map.copyOf(holidayDayByDate);
        absences = List.copyOf(absences);
        Preconditions.checkArgument(plannedTimeByDay.keySet().containsAll(holidayDayByDate.keySet()),
                "The plan has holiday calendar days outside its range");
        for (AbsencePeriod absence : absences) {
            Preconditions.checkArgument(!absence.lastDay().isBefore(absence.firstDay()),
                    "The absence %s ends before it starts", absence.id());
            Preconditions.checkArgument(plannedTimeByDay.keySet().stream().anyMatch(absence::includes),
                    "The absence %s has no day in the plan", absence.id());
        }
    }

    /**
     * Creates a plan without absences.
     *
     * @param plannedTimeByDay the planned time of every day of the range, {@link WorkTime#ZERO} for a day off
     * @param holidayDayByDate the holiday calendar day of each date of the range that has one, an absent date
     *                         included
     */
    public WorkPlan(Map<LocalDate, WorkTime> plannedTimeByDay, Map<LocalDate, HolidayDay> holidayDayByDate) {
        this(plannedTimeByDay, holidayDayByDate, List.of());
    }

    /**
     * Creates a plan without holiday calendar days or absences.
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
     * Returns the holiday calendar day of the specified date. An absent date has it too, although the absence
     * decided the date's planned time.
     *
     * @param date a date the plan covers
     * @return the holiday calendar day, or empty if the holiday calendar of the date's schedule has no day on it
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

    /**
     * An absence as a plan shows it: the whole absence, its days outside the plan's range included.
     *
     * @param id       the id of the absence
     * @param typeName the name of the absence's type, such as "Vacation"
     * @param firstDay the first day of the absence
     * @param lastDay  the last day of the absence, the same as the first for a one-day absence
     */
    public record AbsencePeriod(UUID id, String typeName, LocalDate firstDay, LocalDate lastDay) {

        /**
         * @param date the date to check
         * @return {@code true} if the date is one of the absence's days, its first and last included
         */
        public boolean includes(LocalDate date) {
            return !date.isBefore(firstDay) && !date.isAfter(lastDay);
        }
    }
}
