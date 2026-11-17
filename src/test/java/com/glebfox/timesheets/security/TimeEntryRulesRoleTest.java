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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.app.TimeEntryApprovalSupport;
import com.glebfox.timesheets.app.TimeEntrySupport;
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.accesscontext.InMemoryCrudEntityContext;
import io.jmix.core.security.AccessDeniedException;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.APPROVED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.CLOSED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.REJECTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Each acting user holds Full access and Time entry rules, so only the rules narrow what they do. The fixture is
 * project P, where {@code worker} is a Member, {@code reviewer} an Approver, {@code manager} a Manager, and
 * {@code observer} an Observer, and project Q, where {@code worker} is an Observer. The worker has an entry on P in
 * each status, and the manager an Approved one. Saves go through the constrained {@code DataManager}, the approval
 * operations through {@link TimeEntryApprovalSupport}, which saves with it too.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TimeEntryRulesRoleTest {

    static final String USERNAME_PREFIX = "time-entry-rules-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    AccessManager accessManager;
    @Autowired
    Metadata metadata;
    @Autowired
    ApplicationContext applicationContext;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    TimeEntryApprovalSupport approvalSupport;
    @Autowired
    TimeEntrySupport timeEntrySupport;

    ApprovalTestData data;
    // Entries that a test expects to be refused, removed should a save let one through
    List<Id<?>> attemptedIds = new ArrayList<>();
    LocalDate today;

    User worker;
    User reviewer;
    User manager;
    User observer;

    Task taskP;
    Task otherTaskP;
    Task taskQ;
    Tag tagA;
    Tag tagB;
    ProjectParticipant workerOnP;

    TimeEntry newEntry;
    TimeEntry rejectedEntry;
    TimeEntry approvedEntry;
    TimeEntry closedEntry;
    TimeEntry managerApprovedEntry;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));
        today = LocalDate.now();

        worker = saveRuleHolder("worker");
        reviewer = saveRuleHolder("reviewer");
        manager = saveRuleHolder("manager");
        observer = saveRuleHolder("observer");

        Client client = data.saveClient();
        TaskType taskType = data.saveTaskType();
        Project p = data.saveProject("P", client);
        Project q = data.saveProject("Q", client);
        taskP = data.saveTask(p, taskType);
        otherTaskP = data.saveTask(p, taskType);
        taskQ = data.saveTask(q, taskType);

        TagGroup tagGroup = data.save(dataManager.create(TagGroup.class), g -> {
            g.setName("tag group");
            g.setProject(p);
        });
        tagA = data.save(dataManager.create(Tag.class), t -> {
            t.setName("A");
            t.setTagGroup(tagGroup);
        });
        tagB = data.save(dataManager.create(Tag.class), t -> {
            t.setName("B");
            t.setTagGroup(tagGroup);
        });

        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);
        ProjectRole observerRole = data.saveRole(ProjectRoleType.OBSERVER);
        workerOnP = data.saveParticipant(p, worker, memberRole);
        data.saveParticipant(p, reviewer, data.saveRole(ProjectRoleType.APPROVER));
        data.saveParticipant(p, manager, data.saveRole(ProjectRoleType.MANAGER));
        data.saveParticipant(p, observer, observerRole);
        data.saveParticipant(q, worker, observerRole);

        newEntry = saveEntry(worker, NEW, null);
        rejectedEntry = saveEntry(worker, REJECTED, "Too vague");
        approvedEntry = saveEntry(worker, APPROVED, null);
        closedEntry = saveEntry(worker, CLOSED, null);
        managerApprovedEntry = saveEntry(manager, APPROVED, null);
    }

    @Test
    void createsOnlyOwnNewEntriesOnProjectsWorkedOn() {
        as(worker, () -> {
            TimeEntry own = data.save(entryOf(taskP, worker), e -> {
            });
            assertThat(own.getStatus()).isEqualTo(NEW);

            refusedToSave(entryOf(taskP, reviewer));

            TimeEntry approved = entryOf(taskP, worker);
            approved.setStatus(APPROVED);
            refusedToSave(approved);

            TimeEntry withReason = entryOf(taskP, worker);
            withReason.setRejectionReason("Rejected in advance");
            refusedToSave(withReason);

            // The worker is an Observer of Q
            refusedToSave(entryOf(taskQ, worker));
        });
    }

    @Test
    void authorChangesNewAndRejectedEntries() {
        as(worker, () -> {
            TimeEntry entry = load(newEntry);
            entry.setDescription("Fixed");
            dataManager.save(entry);

            TimeEntry rejected = load(rejectedEntry);
            rejected.setDescription("More detail");
            dataManager.save(rejected);
        });

        assertThat(stored(newEntry).getDescription()).isEqualTo("Fixed");
        assertThat(stored(newEntry).getStatus()).isEqualTo(NEW);
        assertThat(stored(rejectedEntry).getDescription()).isEqualTo("More detail");
        assertThat(stored(rejectedEntry).getStatus()).isEqualTo(NEW);
    }

    @Test
    void authorChangesOnlyTagsOfARejectedEntry() {
        // A change of the tags alone is a change of the content: were it not seen as one, the save would look like
        // a change of the status alone, Rejected to New, which only a reviewer may make
        as(worker, () -> {
            TimeEntry rejected = load(rejectedEntry);
            rejected.setTags(new ArrayList<>(List.of(tagA, tagB)));
            dataManager.save(rejected);
        });

        TimeEntry stored = stored(rejectedEntry);
        assertThat(stored.getStatus()).isEqualTo(NEW);
        assertThat(stored.getTags()).extracting(Tag::getId).containsExactlyInAnyOrder(tagA.getId(), tagB.getId());
    }

    @Test
    void authorNeverChangesUserReasonOrStatus() {
        as(worker, () -> {
            TimeEntry toReviewer = load(newEntry);
            toReviewer.setUser(reviewer);
            refusedToSave(toReviewer);

            TimeEntry reason = load(rejectedEntry);
            reason.setRejectionReason("Not my fault");
            refusedToSave(reason);

            TimeEntry reasonWithContent = load(rejectedEntry);
            reasonWithContent.setDescription("More detail");
            reasonWithContent.setRejectionReason(null);
            refusedToSave(reasonWithContent);

            TimeEntry approved = load(newEntry);
            approved.setStatus(APPROVED);
            refusedToSave(approved);

            TimeEntry approvedWithContent = load(newEntry);
            approvedWithContent.setDescription("Done");
            approvedWithContent.setStatus(APPROVED);
            refusedToSave(approvedWithContent);
        });

        assertThat(stored(newEntry).getUser().getId()).isEqualTo(worker.getId());
        assertThat(stored(newEntry).getStatus()).isEqualTo(NEW);
        assertThat(stored(newEntry).getDescription()).isNull();
        assertThat(stored(rejectedEntry).getStatus()).isEqualTo(REJECTED);
        assertThat(stored(rejectedEntry).getRejectionReason()).isEqualTo("Too vague");
        assertThat(stored(rejectedEntry).getDescription()).isNull();
    }

    @Test
    void approvedAndClosedAreLockedForTheAuthor() {
        as(worker, () -> {
            TimeEntry approved = load(approvedEntry);
            approved.setDescription("Changed after approval");
            refusedToSave(approved);
            refusedToRemove(load(approvedEntry));

            TimeEntry closed = load(closedEntry);
            closed.setDescription("Changed after closing");
            refusedToSave(closed);
            refusedToRemove(load(closedEntry));
        });

        assertThat(stored(approvedEntry).getStatus()).isEqualTo(APPROVED);
        assertThat(stored(approvedEntry).getDescription()).isNull();
        assertThat(stored(closedEntry).getStatus()).isEqualTo(CLOSED);
        assertThat(stored(closedEntry).getDescription()).isNull();
    }

    @Test
    void managersOwnApprovedEntryIsLockedButRejectable() {
        as(manager, () -> {
            TimeEntry own = load(managerApprovedEntry);
            own.setDescription("Changed after approval");
            refusedToSave(own);

            List<TimeEntry> rejected = approvalSupport.reject(List.of(load(managerApprovedEntry)), "Wrong task");
            assertThat(rejected).extracting(TimeEntry::getStatus).containsExactly(REJECTED);
        });

        TimeEntry stored = stored(managerApprovedEntry);
        assertThat(stored.getStatus()).isEqualTo(REJECTED);
        assertThat(stored.getRejectionReason()).isEqualTo("Wrong task");
        assertThat(stored.getDescription()).isNull();
    }

    @Test
    void reviewerSetsOnlyApprovedOrRejected() {
        as(reviewer, () -> {
            List<TimeEntry> approved = approvalSupport.approve(List.of(load(newEntry)));
            assertThat(approved).extracting(TimeEntry::getStatus).containsExactly(APPROVED);

            TimeEntry backToNew = load(rejectedEntry);
            backToNew.setStatus(NEW);
            refusedToSave(backToNew);

            TimeEntry closed = load(approvedEntry);
            closed.setStatus(CLOSED);
            refusedToSave(closed);

            TimeEntry content = load(rejectedEntry);
            content.setDescription("Reviewer's words");
            refusedToSave(content);
        });

        assertThat(stored(newEntry).getStatus()).isEqualTo(APPROVED);
        assertThat(stored(rejectedEntry).getStatus()).isEqualTo(REJECTED);
        assertThat(stored(rejectedEntry).getDescription()).isNull();
        assertThat(stored(approvedEntry).getStatus()).isEqualTo(APPROVED);
    }

    @Test
    void onlyThoseTheApprovalRuleNamesReview() {
        TimeEntry managerNew = saveEntry(manager, NEW, null);
        TimeEntry reviewerNew = saveEntry(reviewer, NEW, null);

        // A plain save of the status, which the approval operations' own check of the rule does not guard
        as(reviewer, () -> {
            refusedToSave(withStatus(load(managerNew), APPROVED));
            refusedToSave(withStatus(load(reviewerNew), APPROVED));
        });
        as(observer, () -> refusedToSave(withStatus(load(newEntry), APPROVED)));
        as(manager, () -> dataManager.save(withStatus(load(newEntry), APPROVED)));

        assertThat(stored(managerNew).getStatus()).isEqualTo(NEW);
        assertThat(stored(reviewerNew).getStatus()).isEqualTo(NEW);
        assertThat(stored(newEntry).getStatus()).isEqualTo(APPROVED);
    }

    @Test
    void reviewerWhoChangesTagsWithTheStatusIsRefused() {
        as(reviewer, () -> {
            TimeEntry entry = load(newEntry);
            entry.setTags(new ArrayList<>(List.of(tagB)));
            entry.setStatus(APPROVED);
            refusedToSave(entry);
        });
        TimeEntry stored = stored(newEntry);
        assertThat(stored.getStatus()).isEqualTo(NEW);
        assertThat(stored.getTags()).extracting(Tag::getId).containsExactly(tagA.getId());

        // The same save with the tags as they are: the loaded tags, saved back unchanged, are no change
        as(reviewer, () -> dataManager.save(withStatus(load(newEntry), APPROVED)));
        assertThat(stored(newEntry).getStatus()).isEqualTo(APPROVED);
    }

    @Test
    void reviewerWhoDeletesWithTheStatusIsRefused() {
        // A save that also marks the entry deleted is no review, so it never bypasses the rule of removal
        as(reviewer, () -> {
            TimeEntry entry = withStatus(load(newEntry), APPROVED);
            entry.setDeletedDate(OffsetDateTime.now());
            entry.setDeletedBy(reviewer.getUsername());
            refusedToSave(entry);
        });

        assertThat(dataManager.load(Id.of(newEntry)).optional()).isPresent();
        assertThat(stored(newEntry).getStatus()).isEqualTo(NEW);
    }

    @Test
    void twoEntriesApprovedInOneSave() {
        TimeEntry second = saveEntry(worker, NEW, null);

        as(reviewer, () -> {
            List<TimeEntry> approved = approvalSupport.approve(List.of(load(newEntry), load(second)));
            assertThat(approved).extracting(TimeEntry::getStatus).containsExactly(APPROVED, APPROVED);
        });

        assertThat(stored(newEntry).getStatus()).isEqualTo(APPROVED);
        assertThat(stored(second).getStatus()).isEqualTo(APPROVED);
    }

    @Test
    void closedIsChangedByNobodyHoldingTheRules() {
        // A plain save: the approval operations refuse a Closed entry themselves before they save
        as(reviewer, () -> {
            TimeEntry rejected = load(closedEntry);
            rejected.setStatus(REJECTED);
            rejected.setRejectionReason("Paid twice");
            refusedToSave(rejected);
        });
        as(manager, () -> refusedToSave(withStatus(load(closedEntry), REJECTED)));
        as(worker, () -> {
            TimeEntry changed = load(closedEntry);
            changed.setDescription("Changed after closing");
            refusedToSave(changed);
        });
        assertThat(stored(closedEntry).getStatus()).isEqualTo(CLOSED);
        assertThat(stored(closedEntry).getDescription()).isNull();

        // Full access alone holds no row-level role
        TimeEntry corrected = load(closedEntry);
        corrected.setDescription("Corrected by the administrator");
        dataManager.save(corrected);
        assertThat(stored(closedEntry).getDescription()).isEqualTo("Corrected by the administrator");
        assertThat(stored(closedEntry).getStatus()).isEqualTo(CLOSED);
    }

    @Test
    void authorWhoLeftTheProjectFixesButDoesNotMoveOrCopy() {
        dataManager.remove(workerOnP);

        as(worker, () -> {
            TimeEntry moved = load(rejectedEntry);
            moved.setTask(otherTaskP);
            refusedToSave(moved);

            refusedToSave(timeEntrySupport.copy(load(rejectedEntry)));

            TimeEntry fixed = load(rejectedEntry);
            fixed.setDescription("Fixed after leaving");
            dataManager.save(fixed);
        });

        TimeEntry stored = stored(rejectedEntry);
        assertThat(stored.getTask().getId()).isEqualTo(taskP.getId());
        assertThat(stored.getDescription()).isEqualTo("Fixed after leaving");
        assertThat(stored.getStatus()).isEqualTo(NEW);
        assertThat(stored.getTags()).extracting(Tag::getId).containsExactly(tagA.getId());
    }

    @Test
    void screensProbeJudgedByTheAuthorRule() {
        // A screen asks about an entry it has loaded, unchanged: the editor whether it opens read-only, the calendar
        // whether it moves the entry
        as(worker, () -> {
            assertThat(updatePermitted(load(newEntry))).as("own New").isTrue();
            assertThat(updatePermitted(load(rejectedEntry))).as("own Rejected").isTrue();
            assertThat(updatePermitted(load(approvedEntry))).as("own Approved").isFalse();
            assertThat(updatePermitted(load(closedEntry))).as("own Closed").isFalse();
        });
        as(reviewer, () -> assertThat(updatePermitted(load(newEntry))).as("the worker's, for the reviewer").isFalse());
        as(manager, () -> {
            assertThat(updatePermitted(load(newEntry))).as("the worker's, for the manager").isFalse();
            assertThat(updatePermitted(load(managerApprovedEntry))).as("the manager's own Approved").isFalse();
        });
    }

    @Test
    void deletesOnlyOwnNewOrRejected() {
        as(reviewer, () -> refusedToRemove(load(newEntry)));
        as(manager, () -> refusedToRemove(load(rejectedEntry)));
        as(worker, () -> {
            refusedToRemove(load(approvedEntry));
            refusedToRemove(load(closedEntry));

            dataManager.remove(load(newEntry));
            dataManager.remove(load(rejectedEntry));
        });

        assertThat(dataManager.load(Id.of(newEntry)).optional()).isEmpty();
        assertThat(dataManager.load(Id.of(rejectedEntry)).optional()).isEmpty();
        assertThat(dataManager.load(Id.of(approvedEntry)).optional()).isPresent();
        assertThat(dataManager.load(Id.of(closedEntry)).optional()).isPresent();
    }

    @AfterEach
    void tearDown() {
        unlinkTags();
        // Entries come before the tasks, users, and tags they refer to
        for (Id<?> id : attemptedIds) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        attemptedIds.clear();
        data.removeAll();
    }

    // Removing an entry removes its tags as well (#8), and the entries share theirs, so the entries let go of their
    // tags before any of them is removed
    private void unlinkTags() {
        List<UUID> userIds = Stream.of(worker, reviewer, manager, observer)
                .filter(Objects::nonNull)
                .map(User::getId)
                .toList();
        if (userIds.isEmpty()) {
            return;
        }
        List<TimeEntry> entries = dataManager.load(TimeEntry.class)
                .query("e.user.id in ?1", userIds)
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.LOCAL).add("tags", FetchPlan.LOCAL))
                .hint(PersistenceHints.SOFT_DELETION, false)
                .list();
        SaveContext saveContext = new SaveContext().setHint(PersistenceHints.SOFT_DELETION, false);
        for (TimeEntry entry : entries) {
            entry.setTags(new ArrayList<>());
            saveContext.saving(entry);
        }
        dataManager.save(saveContext);
    }

    private void as(User user, Runnable action) {
        systemAuthenticator.runWithUser(user.getUsername(), action);
    }

    private void refusedToSave(TimeEntry entry) {
        attemptedIds.add(Id.of(entry));
        assertRefused(() -> dataManager.save(entry));
    }

    private void refusedToRemove(TimeEntry entry) {
        assertRefused(() -> dataManager.remove(entry));
    }

    private static void assertRefused(ThrowingCallable call) {
        assertThatThrownBy(call).isInstanceOf(AccessDeniedException.class);
    }

    private boolean updatePermitted(TimeEntry entry) {
        InMemoryCrudEntityContext context =
                new InMemoryCrudEntityContext(metadata.getClass(TimeEntry.class), applicationContext);
        accessManager.applyRegisteredConstraints(context);
        return context.isUpdatePermitted(entry);
    }

    private static TimeEntry withStatus(TimeEntry entry, TimeEntryStatus status) {
        entry.setStatus(status);
        return entry;
    }

    /**
     * Loads the entry as the time entry editor does.
     */
    private TimeEntry load(TimeEntry entry) {
        return dataManager.load(Id.of(entry))
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE))
                .one();
    }

    /**
     * Reads the entry as stored, as the administrator, outside the acting user's block.
     */
    private TimeEntry stored(TimeEntry entry) {
        return dataManager.load(Id.of(entry))
                .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE)
                        .add("user", FetchPlan.BASE)
                        .add("task", FetchPlan.BASE)
                        .add("tags", FetchPlan.BASE))
                .one();
    }

    private TimeEntry entryOf(Task task, User user) {
        TimeEntry entry = dataManager.create(TimeEntry.class);
        entry.setTask(task);
        entry.setUser(user);
        entry.setDate(today);
        entry.setSpentTime(WorkTime.ofHours(1));
        return entry;
    }

    private TimeEntry saveEntry(User user, TimeEntryStatus status, @Nullable String rejectionReason) {
        return data.save(entryOf(taskP, user), entry -> {
            entry.setStatus(status);
            entry.setRejectionReason(rejectionReason);
            entry.setTags(new ArrayList<>(List.of(tagA)));
        });
    }

    private User saveRuleHolder(String name) {
        User user = data.saveUser(name, name, name);
        assignRole(user, FullAccessRole.CODE, RoleAssignmentRoleType.RESOURCE);
        assignRole(user, TimeEntryRulesRole.CODE, RoleAssignmentRoleType.ROW_LEVEL);
        return user;
    }

    private void assignRole(User user, String roleCode, String roleType) {
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(roleType);
        });
    }
}
