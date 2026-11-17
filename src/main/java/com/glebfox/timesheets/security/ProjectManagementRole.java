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

import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Managing projects: changing a project, and creating, changing, and removing its tasks, participants, tag groups, and
 * tags, in the project editor and its dialogs, with the lists their fields pick from.
 * <p>
 * Creating and removing projects is not granted: {@link ProjectCreationRole} grants it. The pickers' lists open without
 * menu items, and their entities stay read only. Which projects a holder changes is decided by a row-level role.
 */
@ResourceRole(name = "Projects: management", code = ProjectManagementRole.CODE)
public interface ProjectManagementRole extends ProjectReadOnlyRole {

    String CODE = "project-management";

    @EntityAttributePolicy(entityClass = Project.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Project.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    void projectChange();

    @EntityAttributePolicy(entityClass = Task.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Task.class, actions = EntityPolicyAction.ALL)
    void taskChange();

    @EntityAttributePolicy(entityClass = ProjectParticipant.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = ProjectParticipant.class, actions = EntityPolicyAction.ALL)
    void projectParticipantChange();

    @EntityAttributePolicy(entityClass = TagGroup.class, attributes = "*",
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = TagGroup.class, actions = EntityPolicyAction.ALL)
    void tagGroupChange();

    @EntityAttributePolicy(entityClass = Tag.class, attributes = "*", action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = Tag.class, actions = EntityPolicyAction.ALL)
    void tagChange();

    // The lists that the editors' fields pick from: a task's required tag groups, a participant's user and project
    // role, a task's type, and the project's client. No menu items, though tabbed mode opens them by their URLs too
    @ViewPolicy(viewIds = {"ts_TagGroup.lookup", "ts_User.list", "ts_ProjectRole.list", "ts_TaskType.list",
            "ts_Client.list"})
    void pickerScreens();
}
