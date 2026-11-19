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

import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.task.TaskType;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Managing the catalogs that projects use: task types and project roles, in their lists and editors.
 */
@ResourceRole(name = "Task types and project roles", code = ProjectCatalogsRole.CODE)
public interface ProjectCatalogsRole {

    String CODE = "project-catalogs";

    @EntityAttributePolicy(entityClass = TaskType.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = TaskType.class, actions = EntityPolicyAction.ALL)
    void taskType();

    @EntityAttributePolicy(entityClass = ProjectRole.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = ProjectRole.class, actions = EntityPolicyAction.ALL)
    void projectRole();

    @ViewPolicy(viewIds = {"ts_TaskType.list", "ts_TaskType.detail"})
    @MenuPolicy(menuIds = "ts_TaskType.list")
    void taskTypeScreens();

    @ViewPolicy(viewIds = {"ts_ProjectRole.list", "ts_ProjectRole.detail"})
    @MenuPolicy(menuIds = "ts_ProjectRole.list")
    void projectRoleScreens();
}
