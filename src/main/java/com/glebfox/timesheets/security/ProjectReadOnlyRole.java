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

import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Reading projects: the project list and the project editor with the dialogs of its tasks, participants, tag groups,
 * and tags, every attribute view-only, and what they show: the people taking part, their project roles, the project's
 * client, and the tasks' types.
 * <p>
 * Users and clients are read for what the project screens show of them: the role grants no user or client screen, and
 * the editors' pickers are {@link ProjectManagementRole}'s. Which projects a holder reads is decided by a row-level
 * role.
 */
@ResourceRole(name = "Projects: read only", code = ProjectReadOnlyRole.CODE)
public interface ProjectReadOnlyRole {

    String CODE = "project-read";

    @EntityAttributePolicy(entityClass = Project.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Project.class, actions = EntityPolicyAction.READ)
    void project();

    @EntityAttributePolicy(entityClass = Task.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Task.class, actions = EntityPolicyAction.READ)
    void task();

    @EntityAttributePolicy(entityClass = ProjectParticipant.class, attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = ProjectParticipant.class, actions = EntityPolicyAction.READ)
    void projectParticipant();

    @EntityAttributePolicy(entityClass = ProjectRole.class, attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = ProjectRole.class, actions = EntityPolicyAction.READ)
    void projectRole();

    @EntityAttributePolicy(entityClass = TagGroup.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = TagGroup.class, actions = EntityPolicyAction.READ)
    void tagGroup();

    @EntityAttributePolicy(entityClass = Tag.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Tag.class, actions = EntityPolicyAction.READ)
    void tag();

    @EntityAttributePolicy(entityClass = TaskType.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = TaskType.class, actions = EntityPolicyAction.READ)
    void taskType();

    @EntityAttributePolicy(entityClass = Client.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Client.class, actions = EntityPolicyAction.READ)
    void client();

    @EntityAttributePolicy(entityClass = User.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();

    // The dialogs of the editor's tasks, participants, tag groups, and tags have no menu item
    @ViewPolicy(viewIds = {"ts_Project.list", "ts_Project.detail", "ts_Task.detail", "ts_ProjectParticipant.detail",
            "ts_TagGroup.detail", "ts_Tag.detail"})
    @MenuPolicy(menuIds = "ts_Project.list")
    void projectScreens();
}
