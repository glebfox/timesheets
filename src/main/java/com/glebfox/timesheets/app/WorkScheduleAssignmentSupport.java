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

import com.glebfox.timesheets.entity.schedule.WorkScheduleAssignment;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.FluentLoader;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Finds the work schedule assignments that the assignment screens show and check.
 */
@Component("ts_WorkScheduleAssignmentSupport")
public class WorkScheduleAssignmentSupport {

    private final DataManager dataManager;

    public WorkScheduleAssignmentSupport(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Finds the assignments in force on a day: for each user, the assignment with the latest start date
     * on or before the day. A user whose assignments all start later has none.
     *
     * @param assignments the assignments to choose from, each with its user and start date set
     * @param today       the day to choose for
     * @return at most one assignment per user
     */
    public Set<WorkScheduleAssignment> findCurrent(Collection<WorkScheduleAssignment> assignments, LocalDate today) {
        return new HashSet<>(assignments.stream()
                .filter(assignment -> !assignment.getStartDate().isAfter(today))
                .collect(Collectors.toMap(assignment -> assignment.getUser().getId(),
                        Function.identity(),
                        BinaryOperator.maxBy(Comparator.comparing(WorkScheduleAssignment::getStartDate))))
                .values());
    }

    /**
     * Loads the non-deleted assignments of users to any schedule, deleted schedules included.
     *
     * @param users the users whose assignments to load
     * @return the assignments with their user and work schedule
     */
    public List<WorkScheduleAssignment> loadAssignmentsOfUsers(Collection<User> users) {
        if (users.isEmpty()) {
            return List.of();
        }

        return loadAssignments(users, "e.user.id in :userIds").list();
    }

    /**
     * Loads the non-deleted assignments of users that start on a date, deleted schedules included.
     *
     * @param users     the users whose assignments to load
     * @param startDate the start date of the assignments
     * @return the assignments with their user and work schedule
     */
    public List<WorkScheduleAssignment> loadAssignmentsFrom(Collection<User> users, LocalDate startDate) {
        if (users.isEmpty()) {
            return List.of();
        }

        return loadAssignments(users, "e.user.id in :userIds and e.startDate = :startDate")
                .parameter("startDate", startDate)
                .list();
    }

    private FluentLoader.ByQuery<WorkScheduleAssignment> loadAssignments(Collection<User> users, String condition) {
        // The condition must not navigate workSchedule: that joins the schedule table with the soft deletion
        // filter and drops the assignments of a deleted schedule
        return dataManager.load(WorkScheduleAssignment.class)
                .query(condition)
                .parameter("userIds", users.stream().map(User::getId).toList())
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.INSTANCE_NAME)
                        .add("workSchedule", FetchPlan.INSTANCE_NAME));
    }
}
