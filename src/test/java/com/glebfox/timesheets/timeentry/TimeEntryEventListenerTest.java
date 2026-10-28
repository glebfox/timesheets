/*
 * Copyright 2024 Gleb Gorelov
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

package com.glebfox.timesheets.timeentry;

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.event.TimeEntryStatusChangedEvent;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.UiEventPublisher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@ExtendWith({AuthenticatedAsAdmin.class, OutputCaptureExtension.class})
public class TimeEntryEventListenerTest {

    @Autowired
    DataManager dataManager;
    @MockitoSpyBean
    UiEventPublisher uiEventPublisher;

    User user;
    TimeEntry timeEntry;

    List<Object> entities;

    @BeforeEach
    void setUp() {
        entities = new ArrayList<>();

        user = dataManager.create(User.class);
        user.setUsername("time-entry-listener-test-" + UUID.randomUUID());
        entities.add(user);

        Client client = dataManager.create(Client.class);
        client.setName("Listener Test Client");
        entities.add(client);

        Project project = dataManager.create(Project.class);
        project.setName("Listener Test Project");
        project.setClient(client);
        project.setStatus(ProjectStatus.OPEN);
        entities.add(project);

        TaskType taskType = dataManager.create(TaskType.class);
        taskType.setName("Listener Test Task Type");
        entities.add(taskType);

        Task task = dataManager.create(Task.class);
        task.setName("Listener Test Task");
        task.setProject(project);
        task.setType(taskType);
        task.setStatus(TaskStatus.ACTIVE);
        entities.add(task);

        timeEntry = dataManager.create(TimeEntry.class);
        timeEntry.setDate(LocalDate.now());
        timeEntry.setUser(user);
        timeEntry.setTask(task);
        timeEntry.setSpentTime(WorkTime.ofHours(1));
        timeEntry.setStatus(TimeEntryStatus.NEW);
        entities.add(timeEntry);

        dataManager.saveAll(entities);
    }

    @Test
    void removingEntryOfDeletedUserNotifiesUser() {
        /* Given */
        dataManager.remove(Id.of(user));
        clearInvocations(uiEventPublisher);

        /* When */
        dataManager.remove(Id.of(timeEntry));

        /* Then */
        verify(uiEventPublisher).publishEventForUsers(
                any(TimeEntryStatusChangedEvent.class), eq(Set.of(user.getUsername())));
    }

    @Test
    void changingStatusOfEntryOfDeletedUserNotifiesUser() {
        /* Given */
        dataManager.remove(Id.of(user));
        clearInvocations(uiEventPublisher);

        TimeEntry loaded = dataManager.load(Id.of(timeEntry)).one();
        loaded.setStatus(TimeEntryStatus.REJECTED);

        /* When */
        dataManager.save(loaded);

        /* Then */
        verify(uiEventPublisher).publishEventForUsers(
                any(TimeEntryStatusChangedEvent.class), eq(Set.of(user.getUsername())));
    }

    @Test
    void removingEntryTogetherWithItsUserFailsNothingAfterCommit(CapturedOutput output) {
        /* Given */
        clearInvocations(uiEventPublisher);

        /* When */
        dataManager.save(new SaveContext()
                .removing(dataManager.load(Id.of(timeEntry)).one(), dataManager.load(Id.of(user)).one())
                .setHint(PersistenceHints.SOFT_DELETION, false));

        /* Then */
        verify(uiEventPublisher, never()).publishEventForUsers(any(), any());
        // Spring logs an after-commit failure instead of throwing it to the caller
        assertThat(output.toString().contains("afterCompletion threw exception"))
                .as("an after-commit listener failed; see the test output")
                .isFalse();
    }

    @AfterEach
    void tearDown() {
        // Reload by id right before removal: the tests delete rows after they were saved
        for (Object entity : Lists.reverse(entities)) {
            dataManager.load(Id.of(entity))
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(loaded -> dataManager.save(new SaveContext()
                            .removing(loaded)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
    }
}
