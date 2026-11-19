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
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.MenuPolicy;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Logging one's own time: the calendar, My time entries, My tasks, the time entry editor, and the task picker and
 * editor they open, with reading what these screens show.
 * <p>
 * An entry's user, status, and rejection reason stay view-only, on create too: every editor that creates an own entry
 * sets the user itself, and the status and the reason are not the author's to set. The role does not decide whose
 * entries a holder writes, which tasks they log time on, or what they read: a row-level role narrows that. The
 * attribute policies bind the screens, not the data layer.
 */
@ResourceRole(name = "Time logging", code = TimeLoggingRole.CODE)
public interface TimeLoggingRole {

    String CODE = "time-logging";

    // Attribute policies only add, so the view-only attributes are those left out of the MODIFY policy
    @EntityAttributePolicy(entityClass = TimeEntry.class, attributes = "*",
            action = EntityAttributePolicyAction.VIEW)
    @EntityAttributePolicy(entityClass = TimeEntry.class,
            attributes = {"date", "task", "spentTime", "description", "tags"},
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = TimeEntry.class, actions = EntityPolicyAction.ALL)
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

    @EntityAttributePolicy(entityClass = TagGroup.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = TagGroup.class, actions = EntityPolicyAction.READ)
    void tagGroup();

    @EntityAttributePolicy(entityClass = Tag.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = Tag.class, actions = EntityPolicyAction.READ)
    void tag();

    @EntityAttributePolicy(entityClass = TaskType.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = TaskType.class, actions = EntityPolicyAction.READ)
    void taskType();

    @EntityAttributePolicy(entityClass = User.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityPolicy(entityClass = User.class, actions = EntityPolicyAction.READ)
    void user();

    // The task picker and the task editor have no menu item: the time entry editor and My tasks open them
    @ViewPolicy(viewIds = {CalendarView.ID, MyTimeEntryListView.ID, "ts_Task.my", TimeEntryDetailView.ID,
            "ts_Task.lookup", "ts_Task.detail"})
    @MenuPolicy(menuIds = {CalendarView.ID, MyTimeEntryListView.ID, "ts_Task.my"})
    void timesheetsScreens();
}
