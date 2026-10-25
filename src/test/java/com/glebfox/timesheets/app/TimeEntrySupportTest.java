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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectStatus;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.google.common.collect.Lists;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.SaveContext;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import io.jmix.data.PersistenceHints;
import org.apache.commons.collections4.CollectionUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TimeEntrySupportTest {

    @Autowired
    DataManager dataManager;
    @Autowired
    TimeSource timeSource;
    @Autowired
    EntityStates entityStates;
    @Autowired
    TimeEntrySupport timeEntrySupport;
    @Autowired
    CurrentAuthentication currentAuthentication;

    ZonedDateTime zonedDateTime;

    User user;
    Task task;
    Project project;

    List<Object> entities;

    @BeforeEach
    void tearUp() {
        entities = new ArrayList<>();

        user = ((User) currentAuthentication.getUser());
        zonedDateTime = timeSource.now();

        Client client = dataManager.create(Client.class);
        client.setName("Test Client");
        entities.add(client);

        project = dataManager.create(Project.class);
        project.setName("Test Project");
        project.setClient(client);
        project.setStatus(ProjectStatus.OPEN);
        entities.add(project);

        TaskType taskType = dataManager.create(TaskType.class);
        taskType.setName("Test Task Type");
        entities.add(taskType);

        task = dataManager.create(Task.class);
        task.setName("Test Task");
        task.setProject(project);
        task.setType(taskType);
        task.setStatus(TaskStatus.ACTIVE);
        entities.add(task);
    }

    @Test
    void copyTest() {
        /* Given */
        TagGroup tagGroup = dataManager.create(TagGroup.class);
        tagGroup.setName("Test TagGroup");
        tagGroup.setProject(project);
        entities.add(tagGroup);

        Tag tag1 = dataManager.create(Tag.class);
        tag1.setName("Test Tag 1");
        tag1.setTagGroup(tagGroup);
        entities.add(tag1);

        Tag tag2 = dataManager.create(Tag.class);
        tag2.setName("Test Tag 2");
        tag2.setTagGroup(tagGroup);
        entities.add(tag2);

        TimeEntry timeEntry = dataManager.create(TimeEntry.class);
        timeEntry.setDate(zonedDateTime.toLocalDate());
        timeEntry.setUser(user);
        timeEntry.setTask(task);
        timeEntry.setSpentTime(WorkTime.ofHours(1));
        timeEntry.setStatus(TimeEntryStatus.APPROVED);
        entities.add(timeEntry);

        timeEntry.setTags(List.of(tag1, tag2));

        dataManager.saveAll(entities);

        /* When */
        TimeEntry copied = timeEntrySupport.copy(timeEntry);

        /* Then */
        assertThat(copied.getDate()).isEqualTo(timeEntry.getDate());
        assertThat(copied.getUser()).isEqualTo(timeEntry.getUser());
        assertThat(copied.getTask()).isEqualTo(timeEntry.getTask());
        assertThat(copied.getSpentTime()).isEqualTo(timeEntry.getSpentTime());
        assertThat(copied.getStatus()).isEqualTo(TimeEntryStatus.NEW);

        List<Tag> tags = timeEntry.getTags();
        assertThat(copied.getTags())
                .isNotNull()
                .size()
                .isEqualTo(tags.size());
        for (Tag tag : copied.getTags()) {
            assertThat(tag)
                    .isIn(tags)
                    .matches(t -> !entityStates.isNew(t), "Is not new tag");
        }
    }

    @Test
    void rejectedCountTest() {
        /* Given */
        TimeEntry timeEntry1 = dataManager.create(TimeEntry.class);
        timeEntry1.setDate(zonedDateTime.toLocalDate());
        timeEntry1.setUser(user);
        timeEntry1.setTask(task);
        timeEntry1.setSpentTime(WorkTime.ofHours(1));
        timeEntry1.setStatus(TimeEntryStatus.NEW);
        entities.add(timeEntry1);

        TimeEntry timeEntry2 = dataManager.create(TimeEntry.class);
        timeEntry2.setDate(zonedDateTime.toLocalDate());
        timeEntry2.setUser(user);
        timeEntry2.setTask(task);
        timeEntry2.setSpentTime(WorkTime.ofHours(1));
        timeEntry2.setStatus(TimeEntryStatus.REJECTED);
        entities.add(timeEntry2);

        TimeEntry timeEntry3 = dataManager.create(TimeEntry.class);
        timeEntry3.setDate(zonedDateTime.toLocalDate());
        timeEntry3.setUser(user);
        timeEntry3.setTask(task);
        timeEntry3.setSpentTime(WorkTime.ofHours(1));
        timeEntry3.setStatus(TimeEntryStatus.REJECTED);
        entities.add(timeEntry3);

        TimeEntry timeEntry4 = dataManager.create(TimeEntry.class);
        timeEntry4.setDate(zonedDateTime.toLocalDate());
        timeEntry4.setUser(user);
        timeEntry4.setTask(task);
        timeEntry4.setSpentTime(WorkTime.ofHours(1));
        timeEntry4.setStatus(TimeEntryStatus.APPROVED);
        entities.add(timeEntry4);

        User user1 = dataManager.create(User.class);
        user1.setUsername("Test User");
        entities.add(user1);

        TimeEntry timeEntry5 = dataManager.create(TimeEntry.class);
        timeEntry5.setDate(zonedDateTime.toLocalDate());
        timeEntry5.setUser(user1);
        timeEntry5.setTask(task);
        timeEntry5.setSpentTime(WorkTime.ofHours(1));
        timeEntry5.setStatus(TimeEntryStatus.REJECTED);
        entities.add(timeEntry5);

        dataManager.saveAll(entities);

        /* When */
        long rejectedCount = timeEntrySupport.getRejectedCount();

        /* Then */
        assertThat(rejectedCount).isEqualTo(2);
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
