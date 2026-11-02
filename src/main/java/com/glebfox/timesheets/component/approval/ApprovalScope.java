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

package com.glebfox.timesheets.component.approval;

import com.google.common.base.Preconditions;

import java.time.LocalDate;

/**
 * A stretch of days of the selected person's period that a bulk approve covers: one day, the days of a week that
 * belong to the month shown, or the whole period.
 *
 * @param from the first day
 * @param to   the last day, not before the first
 */
public record ApprovalScope(LocalDate from, LocalDate to) {

    public ApprovalScope {
        Preconditions.checkNotNull(from);
        Preconditions.checkNotNull(to);
        Preconditions.checkArgument(!to.isBefore(from), "The last day must not be before the first");
    }

    /**
     * Returns the scope of a single day.
     *
     * @param day the day
     * @return the scope that holds only the day
     */
    static ApprovalScope ofDay(LocalDate day) {
        return new ApprovalScope(day, day);
    }

    /**
     * Determines whether a day is in the scope.
     *
     * @param day the day to check
     * @return {@code true} if the day is from the first to the last day of the scope, inclusive
     */
    public boolean contains(LocalDate day) {
        return !day.isBefore(from) && !day.isAfter(to);
    }
}
