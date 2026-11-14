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
import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.absence.AbsenceType;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.core.usersubstitution.UserSubstitutionManager;
import io.jmix.data.PersistenceHints;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.securitydata.entity.UserSubstitutionEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

/**
 * The fixture is project P and project R, and users who all hold Full access: on P, {@code manager} is a
 * Manager, {@code approver} an Approver, {@code member} a Member, and {@code observer} an Observer; {@code inactive}
 * and {@code deleted} are Members too. {@code former} has no part in P. On R, {@code member} is a Manager.
 * The first names sort in the order of the fixture: Ann, Bob, Cid, Dan, Eve, Fay, Gus.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TeamDashboardSupportTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserSubstitutionManager userSubstitutionManager;
    @Autowired
    TimeEntryApprovalSupport approvalSupport;
    @Autowired
    TeamDashboardSupport teamSupport;
    @Autowired
    EntityStates entityStates;

    ApprovalTestData data;
    List<Id<?>> assignmentIds = new ArrayList<>();
    LocalDate today;

    Client client;
    TaskType taskType;
    ProjectRole managerRole;
    ProjectRole approverRole;
    ProjectRole memberRole;
    ProjectRole observerRole;
    Project projectP;
    Project projectR;
    Task pTask;
    Task rTask;

    User manager;
    User approver;
    User member;
    User observer;
    User inactive;
    User deleted;
    User former;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "team-" + UUID.randomUUID().toString().substring(0, 8));
        today = LocalDate.now();

        client = data.saveClient();
        taskType = data.saveTaskType();
        managerRole = data.saveRole(ProjectRoleType.MANAGER);
        approverRole = data.saveRole(ProjectRoleType.APPROVER);
        memberRole = data.saveRole(ProjectRoleType.MEMBER);
        observerRole = data.saveRole(ProjectRoleType.OBSERVER);
        projectP = data.saveProject("P", client);
        projectR = data.saveProject("R", client);
        pTask = data.saveTask(projectP, taskType);
        rTask = data.saveTask(projectR, taskType);

        manager = saveUser("manager", "Ann", "Zimmer");
        approver = saveUser("approver", "Bob", "Zimmer");
        member = saveUser("member", "Cid", "Zimmer");
        observer = saveUser("observer", "Dan", "Zimmer");
        inactive = saveUser("inactive", "Eve", "Zimmer");
        inactive.setActive(false);
        inactive = dataManager.save(inactive);
        deleted = saveUser("deleted", "Fay", "Zimmer");
        former = saveUser("former", "Gus", "Zimmer");

        data.saveParticipant(projectP, manager, managerRole);
        data.saveParticipant(projectP, approver, approverRole);
        data.saveParticipant(projectP, member, memberRole);
        data.saveParticipant(projectP, observer, observerRole);
        data.saveParticipant(projectP, inactive, memberRole);
        data.saveParticipant(projectP, deleted, memberRole);
        data.saveParticipant(projectR, member, managerRole);
        dataManager.remove(deleted);
    }

    @AfterEach
    void tearDown() {
        // Role assignments and substitutions are removed for good, reloaded by id
        for (Id<?> id : assignmentIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        assignmentIds.clear();
        data.removeAll();
    }

    @Test
    void managerSeesEveryParticipantButSelfTest() {
        List<User> people = asUser(manager, () -> approvalSupport.loadApprovedPeople());

        assertThat(people).extracting(User::getUsername)
                .containsExactly(approver.getUsername(), member.getUsername(), observer.getUsername());
        assertThat(asUser(manager, () -> teamSupport.approvesAnywhere())).isTrue();
    }

    @Test
    void peopleLoadWithoutCredentialsTest() {
        List<User> people = asUser(manager, () -> approvalSupport.loadApprovedPeople());

        assertThat(people).isNotEmpty().allSatisfy(person -> {
            for (String attribute : List.of("username", "firstName", "lastName", "avatar", "active")) {
                assertThat(entityStates.isLoaded(person, attribute)).as(attribute).isTrue();
            }
            assertThat(entityStates.isLoaded(person, "password")).isFalse();
        });
    }

    @Test
    void approverDoesNotSeeManagersTest() {
        // On Q the manager of P is a plain Member, so the approver sees them there
        Project projectQ = data.saveProject("Q", client);
        List<User> beforeQ = asUser(approver, () -> approvalSupport.loadApprovedPeople());
        data.saveParticipant(projectQ, approver, approverRole);
        data.saveParticipant(projectQ, manager, memberRole);
        List<User> afterQ = asUser(approver, () -> approvalSupport.loadApprovedPeople());

        assertThat(beforeQ).extracting(User::getUsername)
                .containsExactly(member.getUsername(), observer.getUsername());
        assertThat(afterQ).extracting(User::getUsername)
                .containsExactly(manager.getUsername(), member.getUsername(), observer.getUsername());
    }

    @Test
    void memberAndObserverApproveNobodyTest() {
        for (User user : List.of(observer, former)) {
            asUser(user, () -> {
                assertThat(teamSupport.approvesAnywhere()).isFalse();
                assertThat(approvalSupport.loadApprovedPeople()).isEmpty();
                assertThat(approvalSupport.loadWaitingEntries()).isEmpty();
                assertThat(teamSupport.loadWaiting().count()).isZero();
                assertThat(teamSupport.loadTeamWeek(today, today)).isEmpty();
                assertThat(teamSupport.loadTeamAbsences(today, today)).isEmpty();
                return null;
            });
        }
        // A member on P who manages R approves on R only
        assertThat(asUser(member, () -> approvalSupport.loadApprovedPeople())).isEmpty();
    }

    @Test
    void inactiveAndDeletedUsersLeftOutTest() {
        List<User> people = asUser(manager, () -> approvalSupport.loadApprovedPeople());

        assertThat(people).extracting(User::getUsername)
                .doesNotContain(inactive.getUsername(), deleted.getUsername(), manager.getUsername());
    }

    @Test
    void deletedProjectOrRoleGrantsNothingTest() {
        dataManager.remove(approverRole);
        assertThat(asUser(approver, () -> approvalSupport.loadApprovedPeople())).isEmpty();
        assertThat(asUser(approver, () -> teamSupport.approvesAnywhere())).isFalse();

        assertThat(asUser(manager, () -> approvalSupport.loadApprovedPeople())).isNotEmpty();
        dataManager.remove(projectP);
        assertThat(asUser(manager, () -> approvalSupport.loadApprovedPeople())).isEmpty();
        assertThat(asUser(manager, () -> teamSupport.approvesAnywhere())).isFalse();
    }

    @Test
    void waitingCountsOnlyNewApprovableEntriesTest() {
        data.saveEntryOf(pTask, member, today.minusDays(3), TimeEntryStatus.NEW, WorkTime.ofHours(4));
        data.saveEntryOf(pTask, member, today.minusDays(10), TimeEntryStatus.NEW, WorkTime.ofHours(2));
        data.saveEntry(pTask, member, today.minusDays(30), TimeEntryStatus.REJECTED, "no");
        data.saveEntry(pTask, member, today.minusDays(31), TimeEntryStatus.APPROVED, (String) null);
        data.saveEntryOf(pTask, former, today.minusDays(20), TimeEntryStatus.NEW, WorkTime.ofHours(4));
        User gone = saveUser("gone", "Hal", "Zimmer");
        data.saveParticipant(projectP, gone, memberRole);
        data.saveEntryOf(pTask, gone, today.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));
        dataManager.remove(gone);
        // On R the manager of P is not a participant
        data.saveEntryOf(rTask, member, today.minusDays(40), TimeEntryStatus.NEW, WorkTime.ofHours(4));

        WaitingApproval waiting = asUser(manager, () -> teamSupport.loadWaiting());

        assertThat(waiting.count()).isEqualTo(4);
        assertThat(waiting.total()).isEqualTo(WorkTime.ofHours(11));
        assertThat(waiting.oldest()).isEqualTo(today.minusDays(20));
        assertThat(waiting.people())
                .extracting(p -> p.user().getUsername(), WaitingApproval.Person::count, WaitingApproval.Person::oldest)
                .containsExactly(
                        tuple(member.getUsername(), 2, today.minusDays(10)),
                        tuple(former.getUsername(), 1, today.minusDays(20)),
                        tuple(gone.getUsername(), 1, today.minusDays(1)));
        assertThat(asUser(manager, () -> approvalSupport.loadWaitingEntries())).hasSize(4);
    }

    @Test
    void nothingWaitingTest() {
        WaitingApproval waiting = asUser(manager, () -> teamSupport.loadWaiting());

        assertThat(waiting.count()).isZero();
        assertThat(waiting.total()).isEqualTo(WorkTime.ZERO);
        assertThat(waiting.oldest()).isNull();
        assertThat(waiting.people()).isEmpty();
    }

    @Test
    void teamWeekSplitsApprovableAndOtherProjectsTest() {
        // The test users are on the seeded default schedule, "Full time": 8 hours Monday to Friday
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = monday.plusDays(6);
        data.saveEntryOf(pTask, member, monday, TimeEntryStatus.APPROVED, WorkTime.ofHours(8));
        data.saveEntryOf(pTask, member, monday.plusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(4));
        data.saveEntryOf(rTask, member, monday.plusDays(1), TimeEntryStatus.APPROVED, WorkTime.ofHours(6));
        // Outside the week
        data.saveEntryOf(pTask, member, monday.minusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));
        data.saveEntryOf(rTask, member, sunday.plusDays(1), TimeEntryStatus.NEW, WorkTime.ofHours(1));

        List<PersonWeek> week = asUser(manager, () -> teamSupport.loadTeamWeek(monday, sunday));

        assertThat(week).extracting(w -> w.user().getUsername())
                .containsExactly(approver.getUsername(), member.getUsername(), observer.getUsername());
        PersonWeek memberWeek = week.get(1);
        assertThat(memberWeek.approvable()).containsOnly(
                entry(TimeEntryStatus.APPROVED, WorkTime.ofHours(8)),
                entry(TimeEntryStatus.NEW, WorkTime.ofHours(4)));
        assertThat(memberWeek.otherProjects()).isEqualTo(WorkTime.ofHours(6));
        assertThat(memberWeek.plan()).isEqualTo(WorkTime.ofHours(40));
        assertThat(memberWeek.absences()).isEmpty();
        assertThat(memberWeek.hasEntries()).isTrue();
    }

    @Test
    void teamWeekPersonWithoutEntriesTest() {
        LocalDate monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        data.saveEntryOf(rTask, observer, monday, TimeEntryStatus.NEW, WorkTime.ofHours(3));

        // The manager of P is no participant of R: the observer's time there is another project's
        List<PersonWeek> week = asUser(manager, () -> teamSupport.loadTeamWeek(monday, monday.plusDays(6)));

        assertThat(week).extracting(w -> w.user().getUsername())
                .containsExactly(approver.getUsername(), member.getUsername(), observer.getUsername());
        assertThat(week).allSatisfy(w -> assertThat(w.hasEntries()).isFalse());
        assertThat(week.get(0).otherProjects()).isEqualTo(WorkTime.ZERO);
        assertThat(week.get(2).otherProjects()).isEqualTo(WorkTime.ofHours(3));
        assertThat(week.get(2).approvable()).isEmpty();
    }

    @Test
    void teamAbsencesTouchingRangeTest() {
        AbsenceType vacation = data.save(dataManager.create(AbsenceType.class), t -> t.setName("vacation"));
        saveAbsence(member, vacation, today.minusDays(10), today.minusDays(5), "before");
        Absence across = saveAbsence(member, vacation, today.plusDays(12), today.plusDays(20), "across");
        Absence inside = saveAbsence(observer, vacation, today.plusDays(2), today.plusDays(3), "inside");
        saveAbsence(approver, vacation, today.plusDays(14), today.plusDays(15), "after");
        saveAbsence(former, vacation, today.plusDays(1), today.plusDays(2), "not on the team");

        List<PersonAbsence> absences = asUser(manager, () -> teamSupport.loadTeamAbsences(today, today.plusDays(13)));

        assertThat(absences)
                .extracting(a -> a.user().getUsername(), a -> a.absence().id(), a -> a.absence().typeName(),
                        a -> a.absence().firstDay(), a -> a.absence().lastDay())
                .containsExactly(
                        tuple(observer.getUsername(), inside.getId(), "vacation", today.plusDays(2),
                                today.plusDays(3)),
                        tuple(member.getUsername(), across.getId(), "vacation", today.plusDays(12),
                                today.plusDays(20)));
    }

    @Test
    void substitutedApproverTest() {
        UserSubstitutionEntity substitution = dataManager.create(UserSubstitutionEntity.class);
        substitution.setUsername("admin");
        substitution.setSubstitutedUsername(manager.getUsername());
        assignmentIds.add(Id.of(dataManager.save(substitution)));

        List<User> people = systemAuthenticator.withUser("admin", () -> {
            userSubstitutionManager.substituteUser(manager.getUsername());
            return approvalSupport.loadApprovedPeople();
        });

        assertThat(people).extracting(User::getUsername)
                .containsExactly(approver.getUsername(), member.getUsername(), observer.getUsername());
    }

    private Absence saveAbsence(User user, AbsenceType type, LocalDate firstDay, LocalDate lastDay, String comment) {
        return data.save(dataManager.create(Absence.class), a -> {
            a.setUser(user);
            a.setType(type);
            a.setFirstDay(firstDay);
            a.setLastDay(lastDay);
            a.setComment(comment);
        });
    }

    private User saveUser(String name, String firstName, String lastName) {
        User saved = data.saveUser(name, firstName, lastName);
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(saved.getUsername());
        assignment.setRoleCode(FullAccessRole.CODE);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        assignmentIds.add(Id.of(dataManager.save(assignment)));
        return saved;
    }

    private <T> T asUser(User user, Supplier<T> action) {
        return systemAuthenticator.withUser(user.getUsername(), action::get);
    }
}
