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

import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.EntitySet;
import io.jmix.core.FetchMode;
import io.jmix.core.FetchPlan;
import io.jmix.core.FetchPlanBuilder;
import io.jmix.core.FetchPlans;
import io.jmix.core.SaveContext;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import jakarta.persistence.OptimisticLockException;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Decides which time entries the current user may approve and loads them.
 * <p>
 * The approver is always the effective user, so under user substitution it is the substituted user.
 * The rule is written once, as the constant {@code APPROVABLE_ENTRY}, and every query of this bean that applies
 * it embeds that constant. The operations, {@link #approve} and {@link #reject}, check it again before they save.
 */
@Component("ts_TimeEntryApprovalSupport")
public class TimeEntryApprovalSupport {

    private static final String MANAGER =
            "@enum(com.glebfox.timesheets.entity.project.ProjectRoleType.MANAGER)";
    private static final String APPROVER =
            "@enum(com.glebfox.timesheets.entity.project.ProjectRoleType.APPROVER)";

    /**
     * Who approves whose entries. A JPQL condition on a time entry with the alias {@code e}, with the parameter
     * {@code :approverId}, the id of the approving user.
     * <p>
     * The entry is approvable when the approver is a participant of the entry's project whose role type is
     * <ul>
     *     <li>Manager: every entry of the project, the manager's own included; or</li>
     *     <li>Approver: every entry of the project except the approver's own and the entries of users who are
     *     participants of that project with a Manager role. An entry of a user who is no longer a participant
     *     counts as a member's.</li>
     * </ul>
     * A role of type Member or Observer grants nothing. Soft deletion applies to the participants, the roles,
     * the task, and the project: a deleted one grants nothing and has no entries to approve.
     * <p>
     * An entry's user is compared by the foreign key, never through the user's own attributes: a condition
     * that navigates {@code e.user}, such as {@code m.user = e.user}, or {@code e.user.id} inside a subquery, joins
     * the user table with the soft deletion filter, and the entries of a deleted user drop out of the result. They
     * still wait for approval. So {@code e.user.id} stands outside the subqueries, and the managers are left out
     * with {@code e.user not in (select m.user ...)}.
     * <p>
     * Soft deletion stays on for the whole query, as it cannot be switched off for part of one: with the
     * {@code PersistenceHints.SOFT_DELETION} hint off, a written {@code x.deletedDate is null} is printed as
     * {@code (0=0)}, so the deleted participants, roles, tasks, and projects would no longer be dropped.
     * <p>
     * {@link #loadProjectsToApprove} repeats this rule's Manager-or-Approver role-type fragment, so a change to the
     * rule touches both.
     */
    private static final String APPROVABLE_ENTRY = """
            (exists (select p from ts_ProjectParticipant p
                     where p.project = e.task.project
                     and p.user.id = :approverId
                     and p.role.type = %1$s)
             or (e.user.id <> :approverId
                 and exists (select p from ts_ProjectParticipant p
                             where p.project = e.task.project
                             and p.user.id = :approverId
                             and p.role.type = %2$s
                             and e.user not in (select m.user from ts_ProjectParticipant m
                                                where m.project = p.project
                                                and m.role.type = %1$s))))
            """.formatted(MANAGER, APPROVER);

    private final DataManager dataManager;
    private final FetchPlans fetchPlans;
    private final CurrentUserSubstitution currentUserSubstitution;

    public TimeEntryApprovalSupport(DataManager dataManager,
                                    FetchPlans fetchPlans,
                                    CurrentUserSubstitution currentUserSubstitution) {
        this.dataManager = dataManager;
        this.fetchPlans = fetchPlans;
        this.currentUserSubstitution = currentUserSubstitution;
    }

    /**
     * Loads the projects where the current user is a participant whose role type is Manager or Approver, open and
     * closed, sorted by name.
     *
     * @return the projects whose entries the current user may approve, at least in part
     */
    public List<Project> loadProjectsToApprove() {
        return dataManager.load(Project.class)
                .query("select pr from ts_Project pr " +
                        "where exists (select p from ts_ProjectParticipant p " +
                        "where p.project = pr and p.user.id = :approverId " +
                        "and p.role.type in (" + MANAGER + ", " + APPROVER + ")) " +
                        "order by pr.name")
                .parameter("approverId", approverId())
                .list();
    }

    /**
     * Loads the entries the current user may approve, dated from {@code from} to {@code to} inclusive.
     * <p>
     * The user is read in a query of its own, not joined to the entries: a join to the user table would drop the
     * entries of a deleted user.
     *
     * @param project the project to narrow the entries to, or {@code null} for every project to approve
     * @param from    the first day of the period
     * @param to      the last day of the period
     * @return the entries with their user, task, project, and tags, in date order
     */
    public List<TimeEntry> loadEntriesToApprove(@Nullable Project project, LocalDate from, LocalDate to) {
        String projectCondition = project != null ? " and e.task.project.id = :projectId" : "";
        var loader = dataManager.load(TimeEntry.class)
                .query("select e from ts_TimeEntry e where " + APPROVABLE_ENTRY +
                        " and e.date >= :dateFrom and e.date <= :dateTo" + projectCondition +
                        " order by e.date")
                .parameter("approverId", approverId())
                .parameter("dateFrom", from)
                .parameter("dateTo", to)
                .fetchPlan(TimeEntryApprovalSupport::addEntryAttributes);
        if (project != null) {
            loader.parameter("projectId", project.getId());
        }
        return loader.list();
    }

    /**
     * Loads the participants of the projects, with the role each has on its project. The participants of deleted
     * users and of deleted roles are loaded too: deleting a user or a role leaves its participants in place, and
     * the entries of a deleted user are shown with the person's role.
     *
     * @param projects the projects whose participants to load
     * @return the non-deleted participants, each with the id of their user, their project, and their role
     */
    public List<ProjectParticipant> loadParticipants(Collection<Project> projects) {
        if (projects.isEmpty()) {
            return List.of();
        }

        return dataManager.load(ProjectParticipant.class)
                .query("select p from ts_ProjectParticipant p where p.project.id in :projectIds")
                .parameter("projectIds", projects.stream().map(Project::getId).toList())
                // The query names only the project by its id, so the participant of a deleted user or role stays in
                // whatever the fetch mode, as a fetched to-one is a left outer join with no soft deletion filter.
                // In batch, it stays in even if a condition naming the user or the role by its id, which turns the
                // fetch into a filtered inner join, is added later (docs/guidelines/database.md)
                .fetchPlan(fetchPlan -> fetchPlan
                        // Only the user's id: a roster matches a participant to a person by it
                        .add("user", user -> {
                        }, FetchMode.BATCH)
                        .add("project", FetchPlan.INSTANCE_NAME)
                        .add("role", FetchPlan.BASE, FetchMode.BATCH))
                .list();
    }

    /**
     * Tells whether the current user may approve the entry. The entry is read from the database by its id, so
     * the answer follows the entry's current user and project, not the state of the object passed in.
     *
     * @param entry the entry to check
     * @return {@code true} if the rule lets the current user approve the entry
     */
    public boolean canApprove(TimeEntry entry) {
        Long count = dataManager
                .loadValue("select count(e) from ts_TimeEntry e where e.id = :entryId and " + APPROVABLE_ENTRY,
                        Long.class)
                .parameter("entryId", entry.getId())
                .parameter("approverId", approverId())
                .one();
        return count > 0;
    }

    /**
     * Sets the entries to Approved. An entry that was rejected keeps its rejection reason.
     * <p>
     * The entries are read again by id, so a stale instance never decides what is saved. The operation either
     * changes every entry or none: all of them are saved in one {@link SaveContext}, which also lets the time
     * entry listener tell each entry's user.
     *
     * @param entries the entries to approve, as the screen loaded them
     * @return the saved entries, one for each distinct entry given and in that order, loaded as
     * {@link #loadEntriesToApprove} loads them
     * @throws IllegalArgumentException if an entry is outside what the current user may approve, is Closed, or is
     *                                  already Approved
     * @throws OptimisticLockException  if an entry has changed or vanished since it was loaded: it was changed,
     *                                  deleted, or moved out of reach, or its task or project was deleted
     */
    public List<TimeEntry> approve(Collection<TimeEntry> entries) {
        return changeStatus(entries, entry -> {
            if (entry.getStatus() == TimeEntryStatus.APPROVED) {
                throw new IllegalArgumentException("Time entry %s is already approved".formatted(entry.getId()));
            }
            entry.setStatus(TimeEntryStatus.APPROVED);
        });
    }

    /**
     * Sets the entries to Rejected with the reason, which replaces an earlier one.
     * <p>
     * The entries are read again by id, so a stale instance never decides what is saved. The operation either
     * changes every entry or none: all of them are saved in one {@link SaveContext}, which also lets the time
     * entry listener tell each entry's user.
     *
     * @param entries the entries to reject, as the screen loaded them
     * @param reason  why the entries are rejected, stored as given
     * @return the saved entries, one for each distinct entry given and in that order, loaded as
     * {@link #loadEntriesToApprove} loads them
     * @throws IllegalArgumentException if the reason is blank, or an entry is outside what the current user may
     *                                  approve, or is Closed
     * @throws OptimisticLockException  if an entry has changed or vanished since it was loaded: it was changed,
     *                                  deleted, or moved out of reach, or its task or project was deleted
     */
    public List<TimeEntry> reject(Collection<TimeEntry> entries, String reason) {
        if (entries.isEmpty()) {
            return List.of();
        }
        if (reason.isBlank()) {
            throw new IllegalArgumentException("The rejection reason is required");
        }
        return changeStatus(entries, entry -> {
            entry.setStatus(TimeEntryStatus.REJECTED);
            entry.setRejectionReason(reason);
        });
    }

    /**
     * Applies the edit to the current state of the entries and saves them all or none.
     * <p>
     * The checks run in this order. First, an entry that is stale: it was changed, or it is gone, which includes an
     * entry of a deleted task or project and one the user cannot reach any more, such as an entry moved to a task of
     * another project. The screen reloads on that, so it comes before any refusal, whether or not the entry is
     * still in the rule. Next, an entry that is current but outside the rule is refused. Last, the status: an entry
     * that was approved or closed meanwhile has a new version, so it surfaces as stale, not as "already approved".
     */
    private List<TimeEntry> changeStatus(Collection<TimeEntry> entries, Consumer<TimeEntry> edit) {
        if (entries.isEmpty()) {
            return List.of();
        }

        List<UUID> ids = entries.stream().map(TimeEntry::getId).distinct().toList();
        // Own attributes only: saving them leaves the entry's references and tags as they are
        Map<UUID, TimeEntry> current = byId(dataManager.load(TimeEntry.class)
                .query("select e from ts_TimeEntry e where e.id in :ids and " + APPROVABLE_ENTRY)
                .parameter("ids", ids)
                .parameter("approverId", approverId())
                .fetchPlan(FetchPlan.LOCAL)
                .list());
        List<UUID> outsideTheRule = ids.stream().filter(id -> !current.containsKey(id)).toList();
        // Only a refusal needs to know what became of the entries outside the rule, so this runs on that path alone.
        // An entry is there while its task and its project are: they are not part of the entry's own version.
        Map<UUID, TimeEntry> outside = outsideTheRule.isEmpty() ? Map.of() : byId(dataManager.load(TimeEntry.class)
                .query("select e from ts_TimeEntry e where e.id in :ids "
                        + "and exists (select pr from ts_Project pr where pr = e.task.project)")
                .parameter("ids", outsideTheRule)
                .fetchPlan(FetchPlan.LOCAL)
                .list());

        for (TimeEntry given : entries) {
            TimeEntry now = current.getOrDefault(given.getId(), outside.get(given.getId()));
            if (now == null || !Objects.equals(now.getVersion(), given.getVersion())) {
                throw new OptimisticLockException(
                        "Time entry %s has changed since it was loaded".formatted(given.getId()));
            }
        }
        if (!outsideTheRule.isEmpty()) {
            throw new IllegalArgumentException("Time entry %s is not one the current user may approve or reject"
                    .formatted(outsideTheRule.get(0)));
        }
        for (TimeEntry entry : current.values()) {
            if (entry.getStatus() == TimeEntryStatus.CLOSED) {
                throw new IllegalArgumentException("Time entry %s is closed".formatted(entry.getId()));
            }
            edit.accept(entry);
        }

        FetchPlanBuilder builder = fetchPlans.builder(TimeEntry.class);
        addEntryAttributes(builder);
        FetchPlan fetchPlan = builder.build();
        SaveContext saveContext = new SaveContext();
        current.values().forEach(entry -> saveContext.saving(entry, fetchPlan));
        EntitySet saved = dataManager.save(saveContext);
        return ids.stream().map(id -> saved.get(TimeEntry.class, id)).toList();
    }

    private static Map<UUID, TimeEntry> byId(List<TimeEntry> entries) {
        return entries.stream().collect(Collectors.toMap(TimeEntry::getId, Function.identity()));
    }

    /**
     * What the approval screen shows of an entry. The user is read in a query of its own, not joined to the
     * entry: a join to the user table would drop the entry of a deleted user.
     */
    private static void addEntryAttributes(FetchPlanBuilder builder) {
        builder.addFetchPlan(FetchPlan.BASE)
                .add("user", FetchPlan.BASE, FetchMode.BATCH)
                .add("task", task -> task.addFetchPlan(FetchPlan.BASE)
                        .add("project", FetchPlan.INSTANCE_NAME))
                .add("tags", FetchPlan.INSTANCE_NAME);
    }

    private UUID approverId() {
        return ((User) currentUserSubstitution.getEffectiveUser()).getId();
    }
}
