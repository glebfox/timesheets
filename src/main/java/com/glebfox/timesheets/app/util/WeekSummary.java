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

import com.google.common.base.Preconditions;

import java.time.LocalDate;

/**
 * The time spent in a calendar week of a month against the time planned for it. The week is the part of
 * a calendar row that falls within the displayed month, so the first and the last weeks of a month
 * may be shorter than seven days.
 *
 * @param firstDay the first day of the week within the displayed month
 * @param lastDay  the last day of the week within the displayed month, not before the first day
 * @param total    the time spent and planned in the days from the first day to the last day
 */
public record WeekSummary(LocalDate firstDay, LocalDate lastDay, FactAndPlan total) {

    public WeekSummary {
        Preconditions.checkNotNull(firstDay);
        Preconditions.checkNotNull(lastDay);
        Preconditions.checkNotNull(total);
        Preconditions.checkArgument(!firstDay.isAfter(lastDay), "firstDay must not be after lastDay");
    }

    /**
     * Determines whether the specified day falls within the week.
     *
     * @param today the day to check, usually the current date
     * @return {@code true} if the day is from the first day to the last day of the week, inclusive
     */
    public boolean isCurrent(LocalDate today) {
        return !today.isBefore(firstDay) && !today.isAfter(lastDay);
    }

    /**
     * Classifies the week against its plan, as of the specified day. The first matching rule wins:
     * <ol>
     *     <li>nothing planned and nothing spent is {@link WeekStatus#NO_WORKDAYS};</li>
     *     <li>more than planned is {@link WeekStatus#OVER}, even in the current week;</li>
     *     <li>exactly as planned is {@link WeekStatus#ON_PLAN}, even in the current week;</li>
     *     <li>less than planned in the current week is {@link WeekStatus#IN_PROGRESS};</li>
     *     <li>less than planned in a week that has not started is {@link WeekStatus#UPCOMING};</li>
     *     <li>less than planned in a week that is over is {@link WeekStatus#SHORT}.</li>
     * </ol>
     *
     * @param today the day to classify the week as of, usually the current date
     * @return the status of the week
     */
    public WeekStatus getStatus(LocalDate today) {
        if (total.equals(FactAndPlan.ZERO)) {
            return WeekStatus.NO_WORKDAYS;
        }
        if (total.isExcess()) {
            return WeekStatus.OVER;
        }
        if (total.isMatch()) {
            return WeekStatus.ON_PLAN;
        }
        if (isCurrent(today)) {
            return WeekStatus.IN_PROGRESS;
        }
        if (firstDay.isAfter(today)) {
            return WeekStatus.UPCOMING;
        }
        return WeekStatus.SHORT;
    }
}
