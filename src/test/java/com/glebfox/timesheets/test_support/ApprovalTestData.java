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

package com.glebfox.timesheets.test_support;

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Creates the data of a test of time entry approval: clients, task types, project roles, projects, tasks, users,
 * participants, and time entries, each named after a prefix unique to the test, and removes them all for good
 * afterwards.
 * <p>
 * A test creates one instance before each test method and calls {@link #removeAll} from its {@code @AfterEach}. The
 * rows are removed with soft deletion off, children before parents, and each is read again by id first, so a row
 * that a listener or an action changed meanwhile is removed all the same.
 */
public final class ApprovalTestData {

    private final DataManager dataManager;
    private final String prefix;
    private final List<Id<?>> createdIds = new ArrayList<>();

    /**
     * Creates the helper of one test method.
     *
     * @param dataManager saves and removes the rows
     * @param prefix      starts the name of every row created, unique to the test
     */
    public ApprovalTestData(DataManager dataManager, String prefix) {
        this.dataManager = dataManager;
        this.prefix = prefix;
    }

    /**
     * Saves an entity, to be removed by {@link #removeAll}.
     *
     * @param entity a new entity, created by {@link DataManager#create}
     * @param init   sets the entity's attributes before it is saved
     * @param <E>    the type of the entity
     * @return the saved entity
     */
    public <E> E save(E entity, Consumer<E> init) {
        init.accept(entity);
        E saved = dataManager.save(entity);
        createdIds.add(Id.of(saved));
        return saved;
    }

    public Client saveClient() {
        return save(dataManager.create(Client.class), client -> client.setName(prefix + " client"));
    }

    public TaskType saveTaskType() {
        return save(dataManager.create(TaskType.class), type -> type.setName(prefix + " task type"));
    }

    public ProjectRole saveRole(ProjectRoleType type) {
        return save(dataManager.create(ProjectRole.class), role -> {
            role.setName(prefix + " " + type.getId() + " role");
            role.setType(type);
        });
    }

    /**
     * Saves a project whose name is the prefix followed by the given name.
     */
    public Project saveProject(String name, Client client) {
        return save(dataManager.create(Project.class), project -> {
            project.setName(prefix + " " + name);
            project.setClient(client);
        });
    }

    /**
     * Saves a task whose name is the prefix followed by "task".
     */
    public Task saveTask(Project project, TaskType type) {
        return save(dataManager.create(Task.class), task -> {
            task.setName(prefix + " task");
            task.setProject(project);
            task.setType(type);
        });
    }

    /**
     * Saves a user whose username is the prefix and the given name, joined by a hyphen.
     */
    public User saveUser(String name, String firstName, String lastName) {
        return save(dataManager.create(User.class), user -> {
            user.setUsername(prefix + "-" + name);
            user.setFirstName(firstName);
            user.setLastName(lastName);
        });
    }

    public ProjectParticipant saveParticipant(Project project, User user, ProjectRole role) {
        return save(dataManager.create(ProjectParticipant.class), participant -> {
            participant.setProject(project);
            participant.setUser(user);
            participant.setRole(role);
        });
    }

    /**
     * Saves an entry of four hours.
     */
    public TimeEntry saveEntry(Task task, User user, LocalDate date, TimeEntryStatus status,
                               @Nullable String rejectionReason) {
        return save(dataManager.create(TimeEntry.class), entry -> {
            entry.setTask(task);
            entry.setUser(user);
            entry.setDate(date);
            entry.setSpentTime(WorkTime.ofHours(4));
            entry.setStatus(status);
            entry.setRejectionReason(rejectionReason);
        });
    }

    /**
     * Loads a user that the test did not create, such as {@code admin}.
     */
    public User loadUser(String username) {
        return dataManager.load(User.class)
                .query("e.username = ?1", username)
                .one();
    }

    /**
     * Removes every row saved through this helper for good, the last saved first; a row already gone is skipped.
     */
    public void removeAll() {
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(current -> dataManager.save(new SaveContext()
                            .removing(current)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }
}
