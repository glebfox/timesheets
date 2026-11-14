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

import java.time.LocalDate;
import java.util.List;

/**
 * What is ahead of a user: the absences that have not ended and the holidays that are days off.
 *
 * @param absences the absences that end on or after the day, nearest first
 * @param holidays the days off of the user's holiday calendar on or after the day, nearest first, consecutive days
 *                 of one name as one holiday
 */
public record ComingUp(List<WorkPlan.AbsencePeriod> absences, List<Holiday> holidays) {

    public ComingUp {
        absences = List.copyOf(absences);
        holidays = List.copyOf(holidays);
    }

    /**
     * A holiday that is a day off, or consecutive days off of one name.
     *
     * @param firstDay the first day
     * @param lastDay  the last day, the same as the first one for one day
     * @param name     the name of the holiday
     */
    public record Holiday(LocalDate firstDay, LocalDate lastDay, String name) {
    }
}
