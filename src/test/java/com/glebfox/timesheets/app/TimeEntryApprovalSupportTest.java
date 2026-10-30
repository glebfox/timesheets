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

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.event.TimeEntryStatusChangedEvent;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.core.usersubstitution.UserSubstitutionManager;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.UiEventPublisher;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.securitydata.entity.UserSubstitutionEntity;
import jakarta.persistence.OptimisticLockException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * The fixture is two projects, P and Q:
 * <ul>
 *     <li>on P, {@code manager} is a Manager, {@code approver1} and {@code approver2} are Approvers,
 *     {@code member} is a Member, and {@code observer} is an Observer; {@code former} has no part in it;</li>
 *     <li>on Q, {@code member} is a Manager and {@code approver1} is a Member.</li>
 * </ul>
 * Every user holds Full access: the rule narrows even a user who may read everything.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TimeEntryApprovalSupportTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserSubstitutionManager userSubstitutionManager;
    @Autowired
    TimeEntryApprovalSupport approvalSupport;
    @MockitoSpyBean
    UiEventPublisher uiEventPublisher;

    String prefix;
    LocalDate today;

    Client client;
    ProjectRole managerRole;
    ProjectRole approverRole;
    ProjectRole memberRole;
    ProjectRole observerRole;

    TagGroup tagGroup;
    Tag tag;

    Project projectP;
    Project projectQ;
    Task pTask;
    Task pTaskDeleted;
    Task qTask;

    User manager;
    User approver1;
    User approver2;
    User member;
    User observer;
    User former;

    ProjectParticipant approver2OnP;

    TimeEntry managerEntry;
    TimeEntry approver1Entry;
    TimeEntry approver2Entry;
    TimeEntry memberEntry;
    TimeEntry formerEntry;
    TimeEntry approver1EntryOnQ;
    TimeEntry memberEntryOnDeletedTask;

    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        prefix = "approval-" + UUID.randomUUID().toString().substring(0, 8);
        today = LocalDate.now();

        client = save(dataManager.create(Client.class), c -> c.setName(prefix + " client"));
        TaskType taskType = save(dataManager.create(TaskType.class), t -> t.setName(prefix + " task type"));

        projectP = saveProject("P", client);
        projectQ = saveProject("Q", client);
        tagGroup = save(dataManager.create(TagGroup.class), g -> {
            g.setName(prefix + " tag group");
            g.setProject(projectP);
        });
        tag = save(dataManager.create(Tag.class), t -> {
            t.setName(prefix + " tag");
            t.setTagGroup(tagGroup);
        });
        pTask = saveTask("pTask", projectP, taskType);
        pTaskDeleted = saveTask("pTaskDeleted", projectP, taskType);
        qTask = saveTask("qTask", projectQ, taskType);

        managerRole = saveRole(ProjectRoleType.MANAGER);
        approverRole = saveRole(ProjectRoleType.APPROVER);
        memberRole = saveRole(ProjectRoleType.MEMBER);
        observerRole = saveRole(ProjectRoleType.OBSERVER);

        manager = saveUser("manager");
        approver1 = saveUser("approver1");
        approver2 = saveUser("approver2");
        member = saveUser("member");
        observer = saveUser("observer");
        former = saveUser("former");

        saveParticipant(projectP, manager, managerRole);
        saveParticipant(projectP, approver1, approverRole);
        approver2OnP = saveParticipant(projectP, approver2, approverRole);
        saveParticipant(projectP, member, memberRole);
        saveParticipant(projectP, observer, observerRole);
        saveParticipant(projectQ, member, managerRole);
        saveParticipant(projectQ, approver1, memberRole);

        managerEntry = saveEntry(pTask, manager, today);
        approver1Entry = saveEntry(pTask, approver1, today);
        approver2Entry = saveEntry(pTask, approver2, today);
        memberEntry = saveEntry(pTask, member, today);
        formerEntry = saveEntry(pTask, former, today);
        approver1EntryOnQ = saveEntry(qTask, approver1, today);
        memberEntryOnDeletedTask = saveEntry(pTaskDeleted, member, today);
        dataManager.remove(pTaskDeleted);
    }

    @Test
    void managerApprovesEveryEntryOfTheProject() {
        List<TimeEntry> entries = asUser(manager, () -> approvalSupport.loadEntriesToApprove(null, today, today));

        assertThat(usernamesOf(entries)).containsExactlyInAnyOrder(
                manager.getUsername(), approver1.getUsername(), approver2.getUsername(),
                member.getUsername(), former.getUsername());
    }

    @Test
    void approverApprovesAllButOwnAndManagers() {
        List<TimeEntry> entries = asUser(approver1, () -> approvalSupport.loadEntriesToApprove(null, today, today));

        assertThat(usernamesOf(entries)).containsExactlyInAnyOrder(
                approver2.getUsername(), member.getUsername(), former.getUsername());
    }

    @Test
    void memberOnOneProjectManagerOnAnother() {
        List<TimeEntry> entries = asUser(member, () -> approvalSupport.loadEntriesToApprove(null, today, today));
        List<Project> projects = asUser(member, () -> approvalSupport.loadProjectsToApprove());

        assertThat(entries).extracting(TimeEntry::getId).containsExactly(approver1EntryOnQ.getId());
        assertThat(projects).extracting(Project::getId).containsExactly(projectQ.getId());
    }

    @Test
    void observerApprovesNothing() {
        List<TimeEntry> entries = asUser(observer, () -> approvalSupport.loadEntriesToApprove(null, today, today));
        List<Project> projects = asUser(observer, () -> approvalSupport.loadProjectsToApprove());

        assertThat(entries).isEmpty();
        assertThat(projects).isEmpty();
    }

    @Test
    void deletedParticipationGrantsNothing() {
        dataManager.remove(approver2OnP);

        List<TimeEntry> entries = asUser(approver2, () -> approvalSupport.loadEntriesToApprove(null, today, today));
        List<Project> projects = asUser(approver2, () -> approvalSupport.loadProjectsToApprove());

        assertThat(entries).isEmpty();
        assertThat(projects).isEmpty();
    }

    @Test
    void projectsToApproveAreSortedByNameOpenAndClosed() {
        Project closedP = dataManager.load(Id.of(projectP)).one();
        closedP.setStatus(ProjectStatus.CLOSED);
        dataManager.saveWithoutReload(closedP);
        // A project whose name sorts before P, where the manager of P is an Approver
        Project projectA = saveProject("A", client);
        saveParticipant(projectA, manager, approverRole);

        List<Project> projects = asUser(manager, () -> approvalSupport.loadProjectsToApprove());

        assertThat(projects).extracting(Project::getName).containsExactly(projectA.getName(), projectP.getName());
        assertThat(projects).extracting(Project::getStatus).containsExactly(ProjectStatus.OPEN, ProjectStatus.CLOSED);
    }

    @Test
    void projectNarrowsToOneProject() {
        List<TimeEntry> onP = asUser(approver1, () -> approvalSupport.loadEntriesToApprove(projectP, today, today));
        List<TimeEntry> onQ = asUser(approver1, () -> approvalSupport.loadEntriesToApprove(projectQ, today, today));

        assertThat(usernamesOf(onP)).containsExactlyInAnyOrder(
                approver2.getUsername(), member.getUsername(), former.getUsername());
        assertThat(onQ).isEmpty();
    }

    @Test
    void datesAreInclusive() {
        TimeEntry yesterdayEntry = saveEntry(pTask, member, today.minusDays(1));

        List<TimeEntry> both = asUser(manager,
                () -> approvalSupport.loadEntriesToApprove(null, today.minusDays(1), today));
        List<TimeEntry> onlyToday = asUser(manager, () -> approvalSupport.loadEntriesToApprove(null, today, today));
        List<TimeEntry> onlyYesterday = asUser(manager,
                () -> approvalSupport.loadEntriesToApprove(null, today.minusDays(1), today.minusDays(1)));

        assertThat(both).extracting(TimeEntry::getId).contains(yesterdayEntry.getId(), memberEntry.getId());
        assertThat(onlyToday).extracting(TimeEntry::getId)
                .contains(memberEntry.getId())
                .doesNotContain(yesterdayEntry.getId());
        assertThat(onlyYesterday).extracting(TimeEntry::getId).containsExactly(yesterdayEntry.getId());
    }

    @Test
    void entriesOfDeletedTasksAreNotListed() {
        List<TimeEntry> entries = asUser(manager, () -> approvalSupport.loadEntriesToApprove(null, today, today));

        assertThat(entries).extracting(TimeEntry::getId).doesNotContain(memberEntryOnDeletedTask.getId());
    }

    @Test
    void entriesOfDeletedUsersAreListed() {
        dataManager.remove(former);

        List<TimeEntry> forManager = asUser(manager, () -> approvalSupport.loadEntriesToApprove(null, today, today));
        List<TimeEntry> forApprover = asUser(approver1, () -> approvalSupport.loadEntriesToApprove(null, today, today));

        assertThat(forManager).extracting(TimeEntry::getId).contains(formerEntry.getId());
        assertThat(usernamesOf(forManager)).contains(former.getUsername());
        assertThat(usernamesOf(forApprover)).contains(former.getUsername());
        assertThat(asUser(approver1, () -> approvalSupport.canApprove(formerEntry))).isTrue();
    }

    @Test
    void aDeletedManagersEntriesStayOffAnApproversList() {
        // Deleting a user leaves their participations in place, so the user still counts as the project's manager
        dataManager.remove(manager);

        List<TimeEntry> entries = asUser(approver1, () -> approvalSupport.loadEntriesToApprove(null, today, today));

        assertThat(usernamesOf(entries)).containsExactlyInAnyOrder(
                approver2.getUsername(), member.getUsername(), former.getUsername());
    }

    @Test
    void aDeletedRoleGrantsNothing() {
        dataManager.remove(approverRole);

        List<TimeEntry> entries = asUser(approver1, () -> approvalSupport.loadEntriesToApprove(null, today, today));
        List<Project> projects = asUser(approver1, () -> approvalSupport.loadProjectsToApprove());

        assertThat(entries).isEmpty();
        assertThat(projects).isEmpty();
    }

    @Test
    void deletedTagsAreNotLoaded() {
        TimeEntry tagged = dataManager.load(Id.of(memberEntry)).one();
        tagged.setTags(List.of(tag));
        dataManager.saveWithoutReload(tagged);
        dataManager.remove(tag);

        List<TimeEntry> entries = asUser(manager, () -> approvalSupport.loadEntriesToApprove(null, today, today));

        TimeEntry loaded = entries.stream()
                .filter(e -> e.getId().equals(memberEntry.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(loaded.getTags()).isEmpty();
    }

    @Test
    void entriesCarryWhatTheScreenShows() {
        TimeEntry tagged = dataManager.load(Id.of(memberEntry)).one();
        tagged.setTags(List.of(tag));
        dataManager.saveWithoutReload(tagged);

        List<TimeEntry> entries = asUser(manager, () -> approvalSupport.loadEntriesToApprove(null, today, today));

        TimeEntry loaded = entries.stream()
                .filter(e -> e.getId().equals(memberEntry.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(loaded.getSpentTime()).isEqualTo(WorkTime.ofHours(1));
        assertThat(loaded.getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(loaded.getUser().getUsername()).isEqualTo(member.getUsername());
        assertThat(loaded.getTask().getName()).isEqualTo(pTask.getName());
        assertThat(loaded.getTask().getProject().getName()).isEqualTo(projectP.getName());
        assertThat(loaded.getTags()).extracting(Tag::getName).containsExactly(tag.getName());
    }

    @Test
    void theApproverIsTheEffectiveUser() {
        UserSubstitutionEntity substitution = dataManager.create(UserSubstitutionEntity.class);
        substitution.setUsername(observer.getUsername());
        substitution.setSubstitutedUsername(manager.getUsername());
        createdIds.add(Id.of(dataManager.save(substitution)));

        List<TimeEntry> entries = asUser(observer, () -> {
            userSubstitutionManager.substituteUser(manager.getUsername());
            return approvalSupport.loadEntriesToApprove(null, today, today);
        });
        List<Project> projects = asUser(observer, () -> {
            userSubstitutionManager.substituteUser(manager.getUsername());
            return approvalSupport.loadProjectsToApprove();
        });
        boolean canApprove = asUser(observer, () -> {
            userSubstitutionManager.substituteUser(manager.getUsername());
            return approvalSupport.canApprove(managerEntry);
        });

        assertThat(usernamesOf(entries)).containsExactlyInAnyOrder(
                manager.getUsername(), approver1.getUsername(), approver2.getUsername(),
                member.getUsername(), former.getUsername());
        assertThat(projects).extracting(Project::getId).containsExactly(projectP.getId());
        assertThat(canApprove).isTrue();
    }

    @Test
    void canApproveFollowsTheRule() {
        asUser(approver1, () -> {
            assertThat(approvalSupport.canApprove(memberEntry)).isTrue();
            assertThat(approvalSupport.canApprove(approver2Entry)).isTrue();
            assertThat(approvalSupport.canApprove(formerEntry)).isTrue();
            assertThat(approvalSupport.canApprove(managerEntry)).isFalse();
            assertThat(approvalSupport.canApprove(approver1Entry)).isFalse();
            assertThat(approvalSupport.canApprove(approver1EntryOnQ)).isFalse();
            assertThat(approvalSupport.canApprove(memberEntryOnDeletedTask)).isFalse();
            return null;
        });
        asUser(manager, () -> {
            assertThat(approvalSupport.canApprove(managerEntry)).isTrue();
            assertThat(approvalSupport.canApprove(approver1Entry)).isTrue();
            assertThat(approvalSupport.canApprove(approver1EntryOnQ)).isFalse();
            return null;
        });
        asUser(observer, () -> {
            assertThat(approvalSupport.canApprove(memberEntry)).isFalse();
            return null;
        });
    }

    @Test
    void canApproveReadsTheCurrentState() {
        boolean before = asUser(approver2, () -> approvalSupport.canApprove(memberEntry));

        dataManager.remove(approver2OnP);

        boolean after = asUser(approver2, () -> approvalSupport.canApprove(memberEntry));
        assertThat(before).isTrue();
        assertThat(after).isFalse();
    }

    @Test
    void participantsCarryTheirRoles() {
        List<ProjectParticipant> participants = approvalSupport.loadParticipants(List.of(projectP));

        assertThat(participants)
                .extracting(p -> p.getUser().getId(), p -> p.getRole().getName(), p -> p.getProject().getName())
                .containsExactlyInAnyOrder(
                        tuple(manager.getId(), roleName(ProjectRoleType.MANAGER), projectP.getName()),
                        tuple(approver1.getId(), roleName(ProjectRoleType.APPROVER), projectP.getName()),
                        tuple(approver2.getId(), roleName(ProjectRoleType.APPROVER), projectP.getName()),
                        tuple(member.getId(), roleName(ProjectRoleType.MEMBER), projectP.getName()),
                        tuple(observer.getId(), roleName(ProjectRoleType.OBSERVER), projectP.getName()));
        assertThat(participants).extracting(p -> p.getRole().getType()).containsExactlyInAnyOrder(
                ProjectRoleType.MANAGER, ProjectRoleType.APPROVER, ProjectRoleType.APPROVER,
                ProjectRoleType.MEMBER, ProjectRoleType.OBSERVER);
    }

    @Test
    void deletedParticipantsAreNotLoaded() {
        dataManager.remove(approver2OnP);

        List<ProjectParticipant> participants = approvalSupport.loadParticipants(List.of(projectP, projectQ));

        assertThat(participants).hasSize(6);
        assertThat(participants).extracting(p -> p.getUser().getId()).doesNotContain(approver2.getId());
        assertThat(approvalSupport.loadParticipants(List.of())).isEmpty();
    }

    @Test
    void participantsOfDeletedUsersAndRolesAreLoaded() {
        dataManager.remove(observer);
        dataManager.remove(memberRole);

        List<ProjectParticipant> participants = approvalSupport.loadParticipants(List.of(projectP));

        assertThat(participants).extracting(p -> p.getUser().getId())
                .containsExactlyInAnyOrder(manager.getId(), approver1.getId(), approver2.getId(), member.getId(),
                        observer.getId());
        assertThat(participants).filteredOn(p -> p.getUser().getId().equals(member.getId()))
                .extracting(p -> p.getRole().getName())
                .containsExactly(roleName(ProjectRoleType.MEMBER));
    }

    @Test
    void approveSetsApprovedAndKeepsTheReason() {
        TimeEntry rejected = withStatus(memberEntry, TimeEntryStatus.REJECTED, "r");

        List<TimeEntry> approved = asUser(approver1, () -> approvalSupport.approve(List.of(rejected)));

        TimeEntry stored = reload(memberEntry);
        assertThat(stored.getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
        assertThat(stored.getRejectionReason()).isEqualTo("r");
        assertThat(approved).extracting(TimeEntry::getId).containsExactly(memberEntry.getId());
        assertThat(approved).extracting(TimeEntry::getStatus).containsExactly(TimeEntryStatus.APPROVED);
    }

    @Test
    void approveRefusesAnEntryOutsideTheRule() {
        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(memberEntry, managerEntry))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("may approve or reject");

        assertThat(reload(memberEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(managerEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
    }

    @Test
    void approveRefusesAnApproversOwnEntryButNotAManagers() {
        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(approver1Entry))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("may approve or reject");
        asUser(manager, () -> approvalSupport.approve(List.of(managerEntry)));

        assertThat(reload(approver1Entry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(managerEntry).getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
    }

    @Test
    void approveRefusesClosedAndApprovedEntries() {
        TimeEntry closed = withStatus(memberEntry, TimeEntryStatus.CLOSED, null);
        TimeEntry approved = withStatus(approver2Entry, TimeEntryStatus.APPROVED, null);

        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(closed))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is closed");
        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(approved))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is already approved");
        // An entry that could be approved beside a refused one stays as it is
        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(formerEntry, closed))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is closed");

        assertThat(reload(formerEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(memberEntry).getStatus()).isEqualTo(TimeEntryStatus.CLOSED);
        assertThat(reload(approver2Entry).getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
    }

    @Test
    void approveFailsWhenAnEntryChangedMeanwhile() {
        TimeEntry stale = staleCopyOf(memberEntry);

        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(stale, approver2Entry))))
                .isInstanceOf(OptimisticLockException.class);

        assertThat(reload(memberEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(approver2Entry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
    }

    @Test
    void approveFailsWhenAnEntryWasDeletedMeanwhile() {
        TimeEntry stale = reload(memberEntry);
        dataManager.remove(memberEntry);

        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(stale, approver2Entry))))
                .isInstanceOf(OptimisticLockException.class);

        assertThat(reload(approver2Entry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
    }

    @Test
    void rejectFailsWhenAnEntryMovedOutOfScopeMeanwhile() {
        TimeEntry stale = reload(memberEntry);
        TimeEntry moved = reload(memberEntry);
        // Project Q is one where approver1 is a plain member
        moved.setTask(qTask);
        dataManager.saveWithoutReload(moved);

        assertThatThrownBy(() -> asUser(approver1,
                () -> approvalSupport.reject(List.of(stale, approver2Entry), "reason")))
                .isInstanceOf(OptimisticLockException.class);

        assertThat(reload(memberEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(approver2Entry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
    }

    @Test
    void approveFailsWhenTheTaskOfAnEntryWasDeletedMeanwhile() {
        // The fixture deleted pTaskDeleted after the entry was saved, which leaves the entry's own version alone
        TimeEntry stale = reload(memberEntryOnDeletedTask);

        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(stale, approver2Entry))))
                .isInstanceOf(OptimisticLockException.class);

        assertThat(reload(approver2Entry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
    }

    @Test
    void approveFailsWhenTheProjectOfAnEntryWasDeletedMeanwhile() {
        // member manages Q, so the entry is theirs to approve until the project is deleted
        TimeEntry stale = reload(approver1EntryOnQ);
        dataManager.remove(projectQ);

        assertThatThrownBy(() -> asUser(member, () -> approvalSupport.approve(List.of(stale))))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void anEntryOutsideTheRuleThatChangedMeanwhileIsStaleNotRefused() {
        // approver1 may not approve managerEntry; once it has changed, it counts as stale before it is refused
        TimeEntry stale = staleCopyOf(managerEntry);

        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.approve(List.of(stale))))
                .isInstanceOf(OptimisticLockException.class);
    }

    @Test
    void rejectSetsRejectedWithTheReason() {
        TimeEntry approved = withStatus(approver2Entry, TimeEntryStatus.APPROVED, null);
        TimeEntry rejected = withStatus(formerEntry, TimeEntryStatus.REJECTED, "old reason");

        List<TimeEntry> result = asUser(approver1,
                () -> approvalSupport.reject(List.of(memberEntry, approved, rejected), "new reason"));

        assertThat(result).extracting(TimeEntry::getId)
                .containsExactly(memberEntry.getId(), approver2Entry.getId(), formerEntry.getId());
        for (TimeEntry entry : List.of(memberEntry, approver2Entry, formerEntry)) {
            TimeEntry stored = reload(entry);
            assertThat(stored.getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
            assertThat(stored.getRejectionReason()).isEqualTo("new reason");
        }
    }

    @Test
    void rejectRequiresAReason() {
        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.reject(List.of(memberEntry), " \n")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");
        assertThatThrownBy(() -> asUser(approver1, () -> approvalSupport.reject(List.of(memberEntry), "")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reason is required");

        TimeEntry stored = reload(memberEntry);
        assertThat(stored.getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(stored.getRejectionReason()).isNull();
    }

    @Test
    void rejectRefusesEntriesOutsideTheRuleAndClosedOnes() {
        TimeEntry closed = withStatus(approver2Entry, TimeEntryStatus.CLOSED, null);

        assertThatThrownBy(() -> asUser(approver1,
                () -> approvalSupport.reject(List.of(memberEntry, managerEntry), "reason")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("may approve or reject");
        assertThatThrownBy(() -> asUser(approver1,
                () -> approvalSupport.reject(List.of(memberEntry, closed), "reason")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("is closed");

        assertThat(reload(memberEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(managerEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(approver2Entry).getStatus()).isEqualTo(TimeEntryStatus.CLOSED);
    }

    @Test
    void rejectFailsWhenAnEntryChangedMeanwhile() {
        TimeEntry stale = staleCopyOf(memberEntry);

        assertThatThrownBy(() -> asUser(approver1,
                () -> approvalSupport.reject(List.of(approver2Entry, stale), "reason")))
                .isInstanceOf(OptimisticLockException.class);

        assertThat(reload(memberEntry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
        assertThat(reload(approver2Entry).getStatus()).isEqualTo(TimeEntryStatus.NEW);
    }

    @Test
    void anEmptyCollectionChangesNothing() {
        assertThat(asUser(approver1, () -> approvalSupport.approve(List.of()))).isEmpty();
        assertThat(asUser(approver1, () -> approvalSupport.reject(List.of(), "reason"))).isEmpty();
        assertThat(asUser(approver1, () -> approvalSupport.reject(List.of(), ""))).isEmpty();
    }

    @Test
    void entriesOfDeletedUsersCanBeApprovedAndRejected() {
        dataManager.remove(former);

        List<TimeEntry> approved = asUser(approver1, () -> approvalSupport.approve(List.of(formerEntry)));
        // The entry that came back is current, so it can be changed again
        List<TimeEntry> rejected = asUser(approver1, () -> approvalSupport.reject(approved, "reason"));

        assertThat(approved).extracting(TimeEntry::getStatus).containsExactly(TimeEntryStatus.APPROVED);
        assertThat(rejected).extracting(TimeEntry::getStatus).containsExactly(TimeEntryStatus.REJECTED);
        assertThat(reload(formerEntry).getStatus()).isEqualTo(TimeEntryStatus.REJECTED);
        assertThat(rejected.get(0).getUser().getUsername()).isEqualTo(former.getUsername());
    }

    @Test
    void savedEntriesCarryWhatTheScreenShowsAndKeepTheirTags() {
        TimeEntry tagged = reload(memberEntry);
        tagged.setTags(List.of(tag));
        dataManager.saveWithoutReload(tagged);

        TimeEntry current = reload(memberEntry);

        List<TimeEntry> approved = asUser(manager, () -> approvalSupport.approve(List.of(current)));

        TimeEntry saved = approved.get(0);
        assertThat(saved.getStatus()).isEqualTo(TimeEntryStatus.APPROVED);
        assertThat(saved.getSpentTime()).isEqualTo(WorkTime.ofHours(1));
        assertThat(saved.getUser().getUsername()).isEqualTo(member.getUsername());
        assertThat(saved.getTask().getName()).isEqualTo(pTask.getName());
        assertThat(saved.getTask().getProject().getName()).isEqualTo(projectP.getName());
        assertThat(saved.getTags()).extracting(Tag::getName).containsExactly(tag.getName());
        TimeEntry stored = dataManager.load(Id.of(memberEntry))
                .fetchPlan(plan -> plan.addFetchPlan(FetchPlan.BASE).add("tags", FetchPlan.INSTANCE_NAME))
                .one();
        assertThat(stored.getTags()).extracting(Tag::getName).containsExactly(tag.getName());
    }

    @Test
    void approveAndRejectTellTheEntryUser() {
        clearInvocations(uiEventPublisher);
        asUser(approver1, () -> approvalSupport.approve(List.of(memberEntry)));

        verify(uiEventPublisher).publishEventForUsers(
                any(TimeEntryStatusChangedEvent.class), eq(Set.of(member.getUsername())));

        TimeEntry current = reload(memberEntry);
        clearInvocations(uiEventPublisher);
        asUser(approver1, () -> approvalSupport.reject(List.of(current), "reason"));

        verify(uiEventPublisher).publishEventForUsers(
                any(TimeEntryStatusChangedEvent.class), eq(Set.of(member.getUsername())));
    }

    @AfterEach
    void tearDown() {
        // Soft-deleted rows and role assignments are removed for good, reloaded by id, children before parents
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    private TimeEntry reload(TimeEntry entry) {
        return dataManager.load(Id.of(entry)).one();
    }

    /**
     * Returns the entry as the screen loaded it, and then changes the entry in the database as its author would,
     * so that the returned instance is stale.
     */
    private TimeEntry staleCopyOf(TimeEntry entry) {
        TimeEntry stale = reload(entry);
        TimeEntry changed = reload(entry);
        changed.setDescription("edited meanwhile");
        dataManager.saveWithoutReload(changed);
        return stale;
    }

    private TimeEntry withStatus(TimeEntry entry, TimeEntryStatus status, String rejectionReason) {
        TimeEntry loaded = reload(entry);
        loaded.setStatus(status);
        loaded.setRejectionReason(rejectionReason);
        return dataManager.save(loaded);
    }

    private <T> T asUser(User user, Supplier<T> action) {
        return systemAuthenticator.withUser(user.getUsername(), action::get);
    }

    private <E> E save(E entity, Consumer<E> init) {
        init.accept(entity);
        E saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private Project saveProject(String name, Client client) {
        return save(dataManager.create(Project.class), p -> {
            p.setName(prefix + " " + name);
            p.setClient(client);
        });
    }

    private Task saveTask(String name, Project project, TaskType taskType) {
        return save(dataManager.create(Task.class), t -> {
            t.setName(prefix + " " + name);
            t.setProject(project);
            t.setType(taskType);
        });
    }

    private ProjectRole saveRole(ProjectRoleType type) {
        return save(dataManager.create(ProjectRole.class), r -> {
            r.setName(roleName(type));
            r.setType(type);
        });
    }

    private String roleName(ProjectRoleType type) {
        return prefix + " " + type.getId() + " role";
    }

    private User saveUser(String name) {
        User saved = save(dataManager.create(User.class), u -> u.setUsername(prefix + "-" + name));
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(saved.getUsername());
        assignment.setRoleCode(FullAccessRole.CODE);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
        return saved;
    }

    private ProjectParticipant saveParticipant(Project project, User user, ProjectRole role) {
        return save(dataManager.create(ProjectParticipant.class), p -> {
            p.setProject(project);
            p.setUser(user);
            p.setRole(role);
        });
    }

    private TimeEntry saveEntry(Task task, User user, LocalDate date) {
        return save(dataManager.create(TimeEntry.class), e -> {
            e.setTask(task);
            e.setUser(user);
            e.setDate(date);
            e.setSpentTime(WorkTime.ofHours(1));
            e.setStatus(TimeEntryStatus.NEW);
        });
    }

    private List<String> usernamesOf(List<TimeEntry> entries) {
        return entries.stream().map(e -> e.getUser().getUsername()).toList();
    }
}
