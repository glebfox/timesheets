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

package com.glebfox.timesheets.view.task;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.FullAccessRole;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import io.jmix.core.DataManager;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The fixture is a user who holds Full access, so that only the screen's own query narrows the tasks, and who takes
 * part in project {@code worked} as a Member and in project {@code observed} as an Observer, each with one task.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class MyTaskListViewUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    SystemAuthenticator systemAuthenticator;
    @Autowired
    ViewBuilders viewBuilders;

    ApprovalTestData data;
    User user;
    Task workedTask;
    Task observedTask;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "my-tasks-" + UUID.randomUUID().toString().substring(0, 8));
        Client client = data.saveClient();
        TaskType taskType = data.saveTaskType();
        Project worked = data.saveProject("worked", client);
        Project observed = data.saveProject("observed", client);
        workedTask = data.saveTask(worked, taskType);
        observedTask = data.saveTask(observed, taskType);

        user = data.saveUser("user", "Chell", null);
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(FullAccessRole.CODE);
            assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        });
        data.saveParticipant(worked, user, data.saveRole(ProjectRoleType.MEMBER));
        data.saveParticipant(observed, user, data.saveRole(ProjectRoleType.OBSERVER));
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    @Test
    void tasksOfAnObservedProjectAreNotListed() {
        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), MyTaskListView.class).open();
            MyTaskListView view = UiTestUtils.getCurrentView();

            CollectionContainer<Task> tasksDc = ViewControllerUtils.getViewData(view).getContainer("tasksDc");
            assertThat(tasksDc.getItems()).extracting(Task::getId)
                    .containsExactly(workedTask.getId())
                    .doesNotContain(observedTask.getId());
        });
    }
}
