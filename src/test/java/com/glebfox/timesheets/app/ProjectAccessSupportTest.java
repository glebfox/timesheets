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

import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.core.usersubstitution.UserSubstitutionManager;
import io.jmix.securitydata.entity.UserSubstitutionEntity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fixture is five projects: {@code user} is a Manager of P1, a Member of P2, an Approver of P3, and an Observer
 * of P4, and has no part in P5; {@code manager} is a Manager of P3, and {@code author} a Member of P3.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class ProjectAccessSupportTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    UserSubstitutionManager userSubstitutionManager;
    @Autowired
    ProjectAccessSupport accessSupport;

    ApprovalTestData data;

    Project p1;
    Project p2;
    Project p3;
    Project p4;
    Project p5;

    User user;
    User manager;
    User author;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "access-" + UUID.randomUUID().toString().substring(0, 8));

        Client client = data.saveClient();
        ProjectRole managerRole = data.saveRole(ProjectRoleType.MANAGER);
        ProjectRole approverRole = data.saveRole(ProjectRoleType.APPROVER);
        ProjectRole memberRole = data.saveRole(ProjectRoleType.MEMBER);
        ProjectRole observerRole = data.saveRole(ProjectRoleType.OBSERVER);
        p1 = data.saveProject("P1", client);
        p2 = data.saveProject("P2", client);
        p3 = data.saveProject("P3", client);
        p4 = data.saveProject("P4", client);
        p5 = data.saveProject("P5", client);

        user = data.saveUser("user", "Ann", "Zimmer");
        manager = data.saveUser("manager", "Bob", "Zimmer");
        author = data.saveUser("author", "Cid", "Zimmer");

        data.saveParticipant(p1, user, managerRole);
        data.saveParticipant(p2, user, memberRole);
        data.saveParticipant(p3, user, approverRole);
        data.saveParticipant(p4, user, observerRole);
        data.saveParticipant(p3, manager, managerRole);
        data.saveParticipant(p3, author, memberRole);
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    @Test
    void managesOnlyWhereManager() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            assertThat(accessSupport.managesProject(p1.getId())).isTrue();
            assertThat(accessSupport.managesProject(p2.getId())).isFalse();
            assertThat(accessSupport.managesProject(p3.getId())).isFalse();
            assertThat(accessSupport.managesProject(p4.getId())).isFalse();
        });
    }

    @Test
    void worksOnEveryProjectButObserved() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            assertThat(accessSupport.worksOn(p1.getId())).isTrue();
            assertThat(accessSupport.worksOn(p2.getId())).isTrue();
            assertThat(accessSupport.worksOn(p3.getId())).isTrue();
            assertThat(accessSupport.worksOn(p4.getId())).isFalse();
            assertThat(accessSupport.worksOn(p5.getId())).isFalse();
        });
    }

    @Test
    void reviewsByTheApprovalRule() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            // A Manager reviews everyone's entries on the project, their own included
            assertThat(accessSupport.mayReview(p1.getId(), author.getId())).isTrue();
            assertThat(accessSupport.mayReview(p1.getId(), manager.getId())).isTrue();
            assertThat(accessSupport.mayReview(p1.getId(), user.getId())).isTrue();
            // An Approver reviews everyone's but their own and the project's Managers'
            assertThat(accessSupport.mayReview(p3.getId(), author.getId())).isTrue();
            assertThat(accessSupport.mayReview(p3.getId(), user.getId())).isFalse();
            assertThat(accessSupport.mayReview(p3.getId(), manager.getId())).isFalse();
            // A Member or an Observer reviews nobody's
            assertThat(accessSupport.mayReview(p2.getId(), author.getId())).isFalse();
            assertThat(accessSupport.mayReview(p4.getId(), author.getId())).isFalse();
        });
    }

    @Test
    void answersNoForAnUnknownProjectOrAuthor() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            assertThat(accessSupport.managesProject(null)).isFalse();
            assertThat(accessSupport.worksOn(null)).isFalse();
            assertThat(accessSupport.mayReview(null, author.getId())).isFalse();
            // The unknown author could be the Approver themselves or a Manager, and even a Manager is not told yes
            assertThat(accessSupport.mayReview(p3.getId(), null)).isFalse();
            assertThat(accessSupport.mayReview(p1.getId(), null)).isFalse();
        });
    }

    @Test
    void judgesTheEffectiveUserOfASubstitution() {
        data.save(dataManager.create(UserSubstitutionEntity.class), substitution -> {
            substitution.setUsername(author.getUsername());
            substitution.setSubstitutedUsername(user.getUsername());
        });

        systemAuthenticator.runWithUser(author.getUsername(), () -> {
            // The substitute alone: a Member of P3 only
            assertThat(accessSupport.managesProject(p1.getId())).isFalse();
            assertThat(accessSupport.worksOn(p2.getId())).isFalse();
            assertThat(accessSupport.mayReview(p3.getId(), author.getId())).isFalse();

            userSubstitutionManager.substituteUser(user.getUsername());

            assertThat(accessSupport.managesProject(p1.getId())).isTrue();
            assertThat(accessSupport.worksOn(p2.getId())).isTrue();
            assertThat(accessSupport.worksOn(p4.getId())).isFalse();
            assertThat(accessSupport.mayReview(p3.getId(), author.getId())).isTrue();
            assertThat(accessSupport.mayReview(p3.getId(), user.getId())).isFalse();
        });
    }
}
