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
import com.google.common.base.Preconditions;

import java.time.LocalDate;
import java.util.Map;

/**
 * The time a user is planned to work on each day of a date range.
 *
 * @param plannedTimeByDay the planned time of every day of the range, {@link WorkTime#ZERO} for a day off
 */
public record WorkPlan(Map<LocalDate, WorkTime> plannedTimeByDay) {

    public WorkPlan {
        plannedTimeByDay = Map.copyOf(plannedTimeByDay);
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
}
