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

package com.glebfox.timesheets.timeentry;

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import io.jmix.security.model.EntityAttributePolicyAction;
import io.jmix.security.model.EntityPolicyAction;
import io.jmix.security.role.annotation.EntityAttributePolicy;
import io.jmix.security.role.annotation.EntityPolicy;
import io.jmix.security.role.annotation.ResourceRole;
import io.jmix.securityflowui.role.annotation.ViewPolicy;

/**
 * Every operation on time entries, with every attribute but the user editable, and opening the Time entries list and
 * the time entry editor: the grants a Manager has on time entries, who may not set an entry's user, for
 * {@link TimeEntryRulesUiTest}.
 */
@ResourceRole(name = "Test: time entries without their user", code = TimeEntryWithoutUserTestRole.CODE)
public interface TimeEntryWithoutUserTestRole {

    String CODE = "test-time-entry-without-user";

    @EntityAttributePolicy(entityClass = TimeEntry.class, attributes = "*", action = EntityAttributePolicyAction.VIEW)
    @EntityAttributePolicy(entityClass = TimeEntry.class,
            attributes = {"date", "task", "spentTime", "description", "tags", "status", "rejectionReason"},
            action = EntityAttributePolicyAction.MODIFY)
    @EntityPolicy(entityClass = TimeEntry.class, actions = EntityPolicyAction.ALL)
    void timeEntry();

    // Tabbed mode opens only the views the user is permitted
    @ViewPolicy(viewIds = {"ts_TimeEntry.list", TimeEntryDetailView.ID})
    void timeEntryViews();
}
