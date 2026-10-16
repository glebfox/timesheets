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

import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.DataManager;
import io.jmix.core.querycondition.JpqlCondition;
import io.jmix.core.querycondition.LogicalCondition;
import io.jmix.core.querycondition.PropertyCondition;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Utility class for managing and manipulating {@link Task} entities.
 */
@Component("ts_TaskSupport")
public class TaskSupport {

    private final DataManager dataManager;

    public TaskSupport(DataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Retrieves a list of active tasks assigned to a specific user.
     *
     * @param user the user whose active tasks are to be retrieved
     * @return a list of active tasks assigned to the specified user
     */
    public List<Task> getUserActiveTasks(User user) {
        return getActiveTasksInternal(0, 0, user, null);
    }

    /**
     * Retrieves a paginated list of active tasks assigned to a specific user.
     *
     * @param user        the user whose active tasks are to be retrieved
     * @param firstResult the starting index of the first result in the paginated list
     * @param maxResult   the maximum number of tasks to return in the result
     * @return a list of active tasks assigned to the specified user, limited by the given
     * pagination parameters
     */
    public List<Task> getUserActiveTasks(User user, int firstResult, int maxResult) {
        return getActiveTasksInternal(firstResult, maxResult, user, null);
    }

    /**
     * Retrieves a stream of active tasks assigned to a specific user.
     *
     * @param user        the user whose active tasks are to be retrieved
     * @param firstResult the starting index of the first result in the paginated list
     * @param maxResult   the maximum number of tasks to return in the result
     * @param filter      an optional string filter to apply to the tasks
     * @return a stream of active tasks assigned to the specified user, considering the given
     * pagination and filter parameters
     */
    public Stream<Task> getUserActiveTasks(User user, int firstResult, int maxResult, @Nullable String filter) {
        return getActiveTasksInternal(firstResult, maxResult, user, filter).stream();
    }

    /**
     * Retrieves a paginated list of active tasks.
     *
     * @param firstResult the starting index of the first result in the paginated list
     * @param maxResult   the maximum number of tasks to return in the result
     * @return a list of active tasks limited by the given pagination parameters
     */
    public List<Task> getActiveTasks(int firstResult, int maxResult) {
        return getActiveTasksInternal(firstResult, maxResult, null, null);
    }

    /**
     * Retrieves a stream of active tasks with optional filtering and pagination.
     *
     * @param firstResult the starting index of the first result in the paginated list
     * @param maxResult   the maximum number of tasks to return in the result
     * @param filter      an optional string filter to apply to the tasks; may be null
     * @return a stream of active tasks, considering the given pagination and filter parameters
     */
    public Stream<Task> getActiveTasks(int firstResult, int maxResult, @Nullable String filter) {
        return getActiveTasksInternal(firstResult, maxResult, null, filter).stream();
    }

    private List<Task> getActiveTasksInternal(int firstResult, int maxResult,
                                              @Nullable User user, @Nullable String filter) {
        LogicalCondition condition = createCondition(user, filter);

        return dataManager.load(Task.class)
                .query("select distinct e from ts_Task e " +
                        "where e.project.status = @enum(com.glebfox.timesheets.entity.project.ProjectStatus.OPEN) " +
                        "and e.status = @enum(com.glebfox.timesheets.entity.task.TaskStatus.ACTIVE) " +
                        "order by e.project.name asc, e.name asc")
                .firstResult(firstResult)
                .condition(condition)
                .maxResults(maxResult)
                .list();
    }

    private LogicalCondition createCondition(@Nullable User user, @Nullable String filter) {
        PropertyCondition nameCondition = PropertyCondition.contains("name", filter);  // parameter value can be 'null'
        nameCondition.setSkipNullOrEmpty(true);

        JpqlCondition participantCondition = JpqlCondition.createWithParameters(
                "(select pp from ts_ProjectParticipant pp where pp.user = :user) MEMBER OF e.project.participants",
                null, Collections.singletonMap("user", user)
        );
        participantCondition.setSkipNullOrEmpty(true);

        return LogicalCondition.and(
                nameCondition,
                participantCondition
        );
    }
}