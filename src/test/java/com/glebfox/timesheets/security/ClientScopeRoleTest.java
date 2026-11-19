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

import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * The acting user {@code c} holds Full access and Client scope, so only the row-level role narrows what they read.
 * {@code c} is an Observer of P1, a project of client X, where {@code colleague} is a Member and {@code leaver} logged
 * an entry and then left. P2, a project of client Y, has {@code stranger} as a Member and no part of {@code c}.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class ClientScopeRoleTest {

    static final String USERNAME_PREFIX = "client-scope-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    EntityStates entityStates;

    ApprovalTestData data;

    Client x;
    Client y;
    Project p1;
    Project p2;

    User c;
    User colleague;
    User leaver;
    User stranger;

    TimeEntry leaversEntry;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));
        LocalDate today = LocalDate.now();

        c = saveClientScopeHolder("c");
        colleague = data.saveUser("colleague", "Colleague", "Member");
        leaver = data.saveUser("leaver", "Leaver", "Member");
        stranger = data.saveUser("stranger", "Stranger", "Member");

        x = saveClient("X");
        y = saveClient("Y");
        TaskType taskType = data.saveTaskType();
        ProjectRole observerRole = data.saveRole(ProjectRoleType.OBSERVER);
        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);

        p1 = data.saveProject("P1", x);
        Task taskOfP1 = data.saveTask(p1, taskType);
        data.saveParticipant(p1, c, observerRole);
        data.saveParticipant(p1, colleague, memberRole);
        ProjectParticipant leaverOnP1 = data.saveParticipant(p1, leaver, memberRole);
        leaversEntry = data.saveEntry(taskOfP1, leaver, today, NEW, null);
        // Leaving a project removes the participant row; the teardown removes it for good
        dataManager.remove(leaverOnP1);

        p2 = data.saveProject("P2", y);
        data.saveParticipant(p2, stranger, memberRole);
    }

    @Test
    void readsOnlyThePeopleOfOwnProjects() {
        List<Object> everyone = idsOf(c, colleague, leaver, stranger);

        as(c, () -> assertThat(loadIds(User.class, everyone))
                .containsExactlyInAnyOrder(c.getId(), colleague.getId()));

        // A holder who takes part in no project reads only themselves
        User loner = saveClientScopeHolder("loner");
        as(loner, () -> assertThat(loadIds(User.class, idsOf(c, colleague, leaver, stranger, loner)))
                .containsExactly(loner.getId()));
    }

    @Test
    void readsOnlyTheClientsOfOwnProjects() {
        as(c, () -> assertThat(loadIds(Client.class, idsOf(x, y)))
                .containsExactly(x.getId()));
    }

    @Test
    void anEntrysUserShowsWhenFetched() {
        // As the Time entries list loads them, with their user
        as(c, () -> {
            TimeEntry entry = dataManager.load(Id.of(leaversEntry))
                    .fetchPlan(builder -> builder.addFetchPlan(FetchPlan.BASE).add("user", FetchPlan.BASE))
                    .one();
            assertThat(entry.getUser()).extracting(User::getId).isEqualTo(leaver.getId());
        });
    }

    @Test
    void anEntrysUserShowsWhenLoadedLazily() {
        // The base fetch plan of an entry holds its task, not its user, so reading the user loads it lazily: a lazy
        // load keeps only the in-memory constraints of the load it comes from, and no JPQL policy applies to it
        as(c, () -> {
            TimeEntry entry = dataManager.load(Id.of(leaversEntry)).fetchPlan(FetchPlan.BASE).one();
            assertThat(entityStates.isLoaded(entry, "user")).isFalse();
            assertThat(entry.getUser()).extracting(User::getId).isEqualTo(leaver.getId());
            assertThat(loadIds(User.class, idsOf(leaver))).isEmpty();
        });
    }

    @Test
    void includesProjectScope() {
        as(c, () -> assertThat(loadIds(Project.class, idsOf(p1, p2)))
                .containsExactly(p1.getId()));
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    private Client saveClient(String name) {
        return data.save(dataManager.create(Client.class),
                client -> client.setName(USERNAME_PREFIX + UUID.randomUUID() + " " + name));
    }

    private void as(User user, Runnable action) {
        systemAuthenticator.runWithUser(user.getUsername(), action);
    }

    private List<Object> loadIds(Class<?> entityClass, Collection<Object> ids) {
        return dataManager.load(entityClass)
                .query("e.id in :ids")
                .parameter("ids", ids)
                .list()
                .stream()
                .map(EntityValues::getId)
                .toList();
    }

    private static List<Object> idsOf(Object... entities) {
        return Stream.of(entities)
                .map(EntityValues::getId)
                .toList();
    }

    private User saveClientScopeHolder(String name) {
        User user = data.saveUser(name, name, name);
        assignRole(user, FullAccessRole.CODE, RoleAssignmentRoleType.RESOURCE);
        assignRole(user, ClientScopeRole.CODE, RoleAssignmentRoleType.ROW_LEVEL);
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
