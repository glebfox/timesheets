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

package com.glebfox.timesheets.component.emptystate;

import com.glebfox.timesheets.entity.task.TaskType;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Reading task types without any of their attributes, and opening {@link GridEmptyStateTestView}: a user for whom
 * security hides every property column of the test view's grids, for {@link GridEmptyStateUiTest}.
 */
@ResourceRole(name = "Test: task type without attributes", code = TaskTypeNoAttributesTestRole.CODE)
public interface TaskTypeNoAttributesTestRole {

    String CODE = "test-task-type-no-attributes";

    @EntityPolicy(entityClass = TaskType.class, actions = EntityPolicyAction.READ)
    void taskType();

    // Tabbed mode opens only the views the user is permitted
    @ViewPolicy(viewIds = "ts_GridEmptyStateTestView")
    void testView();
}
