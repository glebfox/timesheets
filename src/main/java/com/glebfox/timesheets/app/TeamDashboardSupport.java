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

import com.glebfox.timesheets.app.util.PersonAbsence;
import com.glebfox.timesheets.app.util.PersonWeek;
import com.glebfox.timesheets.app.util.WaitingApproval;
import com.glebfox.timesheets.app.util.WorkPlan;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.FetchMode;
import io.jmix.core.FetchPlan;
import io.jmix.core.UnconstrainedDataManager;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Computes what the dashboard shows of the current user's team: what waits for their approval, the team's week,
 * and who is away. The current user is the effective user, the substituted one when a substitution is active.
 * <p>
 * The team is the people whose entries the user approves by participation, see
 * {@link TimeEntryApprovalSupport#loadApprovedPeople}.
 */
@Component("ts_TeamDashboardSupport")
public class TeamDashboardSupport {

    private final TimeEntryApprovalSupport approvalSupport;
    private final WorkPlanSupport workPlanSupport;
    private final UnconstrainedDataManager unconstrainedDataManager;

    public TeamDashboardSupport(TimeEntryApprovalSupport approvalSupport,
                                WorkPlanSupport workPlanSupport,
                                UnconstrainedDataManager unconstrainedDataManager) {
        this.approvalSupport = approvalSupport;
        this.workPlanSupport = workPlanSupport;
        this.unconstrainedDataManager = unconstrainedDataManager;
    }

    /**
     * Tells whether the current user approves on at least one project.
     *
     * @return {@code true} if the user is a participant with a Manager or Approver role on a project
     */
    public boolean approvesAnywhere() {
        return !approvalSupport.loadProjectsToApprove().isEmpty();
    }

    /**
     * Loads what waits for the current user's approval: every New entry the approval rule lets the user approve,
     * of any date. The entries of deleted users, inactive users, former participants, and Observers count, so the
     * number matches the approval screen's, and such a user may be one of the people.
     *
     * @return the number of entries, their total time, the date of the oldest, and the people with the most entries
     * first, then by name
     */
    public WaitingApproval loadWaiting() {
        List<TimeEntry> entries = approvalSupport.loadWaitingEntries();

        WorkTime total = WorkTime.ZERO;
        LocalDate oldest = null;
        Map<UUID, List<TimeEntry>> byUser = new LinkedHashMap<>();
        for (TimeEntry entry : entries) {
            if (entry.getSpentTime() != null) {
                total = total.plus(entry.getSpentTime());
            }
            if (oldest == null || entry.getDate().isBefore(oldest)) {
                oldest = entry.getDate();
            }
            byUser.computeIfAbsent(entry.getUser().getId(), id -> new ArrayList<>()).add(entry);
        }

        List<WaitingApproval.Person> people = byUser.values().stream()
                .map(userEntries -> new WaitingApproval.Person(
                        userEntries.getFirst().getUser(),
                        userEntries.size(),
                        userEntries.stream().map(TimeEntry::getDate).min(Comparator.naturalOrder()).orElseThrow()))
                .sorted(Comparator.comparingInt(WaitingApproval.Person::count).reversed()
                        .thenComparing(person -> person.user().getDisplayName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
        return new WaitingApproval(entries.size(), total, oldest, people);
    }

    /**
     * Loads the week of every person of the team.
     * <p>
     * The time on projects where the current user does not approve the person is read without the user's own
     * permissions, and so are the plan and the absences: a deliberate exposure, so the dashboard shows whether the
     * person logged their week.
     *
     * @param firstDay the first day of the week
     * @param lastDay  the last day of the week, inclusive
     * @return one week for each person, in the order of {@link TimeEntryApprovalSupport#loadApprovedPeople}
     */
    public List<PersonWeek> loadTeamWeek(LocalDate firstDay, LocalDate lastDay) {
        List<User> people = approvalSupport.loadApprovedPeople();
        if (people.isEmpty()) {
            return List.of();
        }

        List<TimeEntry> approvableEntries = approvalSupport.loadEntriesToApprove(null, firstDay, lastDay);
        Set<UUID> approvableIds = new HashSet<>();
        Map<UUID, Map<TimeEntryStatus, WorkTime>> approvableByUser = new HashMap<>();
        for (TimeEntry entry : approvableEntries) {
            approvableIds.add(entry.getId());
            approvableByUser
                    .computeIfAbsent(entry.getUser().getId(), id -> new EnumMap<>(TimeEntryStatus.class))
                    .merge(entry.getStatus(), spentTime(entry), WorkTime::plus);
        }

        Map<UUID, WorkTime> otherByUser = new HashMap<>();
        unconstrainedDataManager.load(TimeEntry.class)
                .query("""
                        select e from ts_TimeEntry e
                        where e.user.id in :userIds and e.date >= :dateFrom and e.date <= :dateTo
                        and exists (select pr from ts_Project pr where pr = e.task.project)
                        """)
                .parameter("userIds", people.stream().map(User::getId).toList())
                .parameter("dateFrom", firstDay)
                .parameter("dateTo", lastDay)
                .fetchPlan(fetchPlan -> fetchPlan
                        .addFetchPlan(FetchPlan.BASE)
                        .add("user", user -> {
                        }, FetchMode.BATCH))
                .list().stream()
                .filter(entry -> !approvableIds.contains(entry.getId()))
                .forEach(entry -> otherByUser.merge(entry.getUser().getId(), spentTime(entry), WorkTime::plus));

        return people.stream()
                .map(person -> {
                    WorkPlan plan = workPlanSupport.loadWorkPlan(person, firstDay, lastDay.plusDays(1));
                    WorkTime planned = plan.plannedTimeByDay().values().stream()
                            .reduce(WorkTime.ZERO, WorkTime::plus);
                    return new PersonWeek(person,
                            approvableByUser.getOrDefault(person.getId(), Map.of()),
                            otherByUser.getOrDefault(person.getId(), WorkTime.ZERO),
                            planned,
                            plan.absences());
                })
                .toList();
    }

    /**
     * Loads the absences of the team that share a day with the range, read without the current user's own
     * permissions and without the comment.
     *
     * @param from the first day of the range
     * @param to   the last day of the range, inclusive
     * @return the absences, whole, the one starting first first, then the one ending first, then by the person's name
     */
    public List<PersonAbsence> loadTeamAbsences(LocalDate from, LocalDate to) {
        List<User> people = approvalSupport.loadApprovedPeople();
        if (people.isEmpty()) {
            return List.of();
        }
        Map<UUID, User> peopleById = new HashMap<>();
        people.forEach(person -> peopleById.put(person.getId(), person));

        // An absence of a deleted type stays in with its type's name, as in WorkPlanSupport
        return unconstrainedDataManager.load(Absence.class)
                .query("select a from ts_Absence a where a.user.id in :userIds " +
                        "and a.firstDay <= :to and a.lastDay >= :from")
                .parameter("userIds", peopleById.keySet())
                .parameter("from", from)
                .parameter("to", to)
                .fetchPlan(fetchPlan -> fetchPlan
                        .addFetchPlan(FetchPlan.BASE)
                        .add("user", user -> {
                        }, FetchMode.BATCH)
                        .add("type", FetchPlan.INSTANCE_NAME, FetchMode.BATCH))
                .list().stream()
                .map(absence -> new PersonAbsence(peopleById.get(absence.getUser().getId()),
                        new WorkPlan.AbsencePeriod(absence.getId(), absence.getType().getName(),
                                absence.getFirstDay(), absence.getLastDay())))
                .sorted(Comparator.comparing((PersonAbsence a) -> a.absence().firstDay())
                        .thenComparing(a -> a.absence().lastDay())
                        .thenComparing(a -> a.user().getDisplayName(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static WorkTime spentTime(TimeEntry entry) {
        return entry.getSpentTime() != null ? entry.getSpentTime() : WorkTime.ZERO;
    }
}
