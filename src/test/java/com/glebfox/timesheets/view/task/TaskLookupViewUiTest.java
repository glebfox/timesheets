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
import com.glebfox.timesheets.test_support.ApprovalTestData;
import io.jmix.core.DataManager;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.model.CollectionContainer;
import io.jmix.flowui.model.ViewData;
import io.jmix.flowui.view.DialogWindow;
import io.jmix.flowui.view.ViewControllerUtils;
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
 * The fixture is a user who takes part in project {@code worked} as a Member and in project {@code observed} as an
 * Observer, each with one task. The picker is opened as the time entry editor opens it, for that user.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TaskLookupViewUiTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    DialogWindows dialogWindows;

    ApprovalTestData data;
    User user;
    Project worked;
    Project observed;
    Task workedTask;
    Task observedTask;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, "task-lookup-" + UUID.randomUUID().toString().substring(0, 8));
        Client client = data.saveClient();
        TaskType taskType = data.saveTaskType();
        worked = data.saveProject("worked", client);
        observed = data.saveProject("observed", client);
        workedTask = data.saveTask(worked, taskType);
        observedTask = data.saveTask(observed, taskType);

        user = data.saveUser("user", "Chell", null);
        data.saveParticipant(worked, user, data.saveRole(ProjectRoleType.MEMBER));
        data.saveParticipant(observed, user, data.saveRole(ProjectRoleType.OBSERVER));
    }

    @AfterEach
    void tearDown() {
        data.removeAll();
    }

    @Test
    void tasksAndProjectsOfAnObservedProjectAreNotListed() {
        DialogWindow<TaskLookupView> dialog = dialogWindows.lookup(UiTestUtils.getCurrentView(), Task.class)
                .withViewClass(TaskLookupView.class)
                .build();
        dialog.getView().setUser(user);
        dialog.open();

        ViewData viewData = ViewControllerUtils.getViewData(dialog.getView());
        CollectionContainer<Task> tasksDc = viewData.getContainer("tasksDc");
        CollectionContainer<Project> projectsDc = viewData.getContainer("projectsDc");
        assertThat(tasksDc.getItems()).extracting(Task::getId)
                .containsExactly(workedTask.getId())
                .doesNotContain(observedTask.getId());
        // The project filter leaves out the observed project too
        assertThat(projectsDc.getItems()).extracting(Project::getId)
                .containsExactly(worked.getId())
                .doesNotContain(observed.getId());
    }
}
