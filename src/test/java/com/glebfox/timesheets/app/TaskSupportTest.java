/*
 * Copyright 2023 Gleb Gorelov
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
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TaskSupportTest {

    @Autowired
    DataManager dataManager;

    @Autowired
    TaskSupport support;

    List<Object> entities;

    @Test
    void userActiveTasksTest() {
        /* Test data */

        entities = new ArrayList<>();

        User user1 = dataManager.create(User.class);
        user1.setUsername("test-user-" + RandomStringUtils.insecure().nextAlphabetic(5));
        entities.add(user1);

        User user2 = dataManager.create(User.class);
        user2.setUsername("test-user-" + RandomStringUtils.insecure().nextAlphabetic(5));
        entities.add(user2);

        Client client = dataManager.create(Client.class);
        client.setName("Test Client " + RandomStringUtils.insecure().nextAlphabetic(5));
        entities.add(client);

        Project project1 = dataManager.create(Project.class);
        project1.setName("Test Project " + RandomStringUtils.insecure().nextAlphabetic(5));
        project1.setClient(client);
        entities.add(project1);

        Project project2 = dataManager.create(Project.class);
        project2.setName("Test Project " + RandomStringUtils.insecure().nextAlphabetic(5));
        project2.setClient(client);
        entities.add(project2);

        TaskType taskType = dataManager.create(TaskType.class);
        taskType.setName("Test TaskType " + RandomStringUtils.insecure().nextAlphabetic(5));
        entities.add(taskType);

        Task task1 = dataManager.create(Task.class);
        task1.setName("Test Task " + RandomStringUtils.insecure().nextAlphabetic(5));
        task1.setProject(project1);
        task1.setType(taskType);
        entities.add(task1);

        Task task2 = dataManager.create(Task.class);
        task2.setName("Test Task " + RandomStringUtils.insecure().nextAlphabetic(5));
        task2.setProject(project2);
        task2.setType(taskType);
        entities.add(task2);

        ProjectRole projectRole = dataManager.create(ProjectRole.class);
        projectRole.setName("Test ProjectRole " + RandomStringUtils.insecure().nextAlphabetic(5));
        projectRole.setType(ProjectRoleType.MEMBER);
        entities.add(projectRole);

        ProjectParticipant projectParticipant1 = dataManager.create(ProjectParticipant.class);
        projectParticipant1.setUser(user1);
        projectParticipant1.setProject(project1);
        projectParticipant1.setRole(projectRole);
        entities.add(projectParticipant1);

        ProjectParticipant projectParticipant2 = dataManager.create(ProjectParticipant.class);
        projectParticipant2.setUser(user2);
        projectParticipant2.setProject(project2);
        projectParticipant2.setRole(projectRole);
        entities.add(projectParticipant2);

        dataManager.saveAll(entities);

        /* Test */
        List<Task> userActiveTasks = support.getUserActiveTasks(user1);
        assertThat(userActiveTasks).hasSize(1);
    }

    @AfterEach
    void tearDown() {
        if (CollectionUtils.isEmpty(entities)) {
            return;
        }

        dataManager.save(
                new SaveContext()
                        .removing(Lists.reverse(entities))
                        .setHint(PersistenceHints.SOFT_DELETION, false)
        );
    }
}
