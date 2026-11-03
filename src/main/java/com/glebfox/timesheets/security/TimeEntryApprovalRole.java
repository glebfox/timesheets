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
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Approving time entries: the approval screen, reading time entries and what the screen shows of them, and changing
 * an entry's status and rejection reason.
 * <p>
 * The role does not decide whose entries a holder approves: the approval support bean does, by the holder's part in
 * the entry's project. So the project screens are not granted, and a holder who is no Manager or Approver anywhere
 * sees an empty screen. The attribute policies bind the screens, not the data layer: the support bean changes only the
 * status and the reason.
 */
@ResourceRole(name = "Time entries: approval", code = TimeEntryApprovalRole.CODE)
public interface TimeEntryApprovalRole extends UserReadOnlyRole {

    String CODE = "time-entry-approval";

    @EntityAttributePolicy(entityClass = TimeEntry.class, attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    @EntityAttributePolicy(entityClass = TimeEntry.class, attributes = {"status", "rejectionReason"},
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = TimeEntry.class, actions = {EntityPolicyAction.READ, EntityPolicyAction.UPDATE})
    void timeEntry();

    @EntityAttributePolicy(entityClass = Task.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Task.class, actions = EntityPolicyAction.READ)
    void task();

    @EntityAttributePolicy(entityClass = Project.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Project.class, actions = EntityPolicyAction.READ)
    void project();

    @EntityAttributePolicy(entityClass = ProjectParticipant.class, attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = ProjectParticipant.class, actions = EntityPolicyAction.READ)
    void projectParticipant();

    @EntityAttributePolicy(entityClass = ProjectRole.class, attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = ProjectRole.class, actions = EntityPolicyAction.READ)
    void projectRole();

    @EntityAttributePolicy(entityClass = Tag.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Tag.class, actions = EntityPolicyAction.READ)
    void tag();

    @EntityAttributePolicy(entityClass = TagGroup.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = TagGroup.class, actions = EntityPolicyAction.READ)
    void tagGroup();

    @ViewPolicy(viewIds = TimeEntryApprovalView.ID)
    @MenuPolicy(menuIds = TimeEntryApprovalView.ID)
    void timeEntryApprovalScreen();
}
