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
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;

import java.util.List;
import java.util.Map;

/**
 * A person of the current user's team and their week.
 *
 * @param user          the person
 * @param approvable    the time spent on the projects where the current user approves the person, by status; a
 *                      status with no entries is absent
 * @param otherProjects the time spent on any other project, in total
 * @param plan          the time the person is planned to work in the week
 * @param absences      the person's absences that share a day with the week, each whole
 */
public record PersonWeek(User user, Map<TimeEntryStatus, WorkTime> approvable, WorkTime otherProjects, WorkTime plan,
                         List<WorkPlan.AbsencePeriod> absences) {

    public PersonWeek {
        approvable = Map.copyOf(approvable);
        absences = List.copyOf(absences);
    }

    /**
     * Tells whether the approval screen would show the person for the week. Time on other projects alone does not
     * count: the screen's roster lists only the people with entries to approve.
     *
     * @return {@code true} if the person has time on a project where the current user approves them
     */
    public boolean hasEntries() {
        return !approvable.isEmpty();
    }
}
