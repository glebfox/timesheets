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

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import io.jmix.security.model.RowLevelBiPredicate;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;
import org.springframework.context.ApplicationContext;

/**
 * The role that allows editing only active time entries, i.e., not approved or closed.
 */
@RowLevelRole(name = "EditActiveTimeEntries", code = EditActiveTimeEntriesRole.CODE)
public interface EditActiveTimeEntriesRole {

    String CODE = "edit-active-time-entries";

    @PredicateRowLevelPolicy(entityClass = TimeEntry.class, actions = RowLevelPolicyAction.UPDATE)
    default RowLevelBiPredicate<TimeEntry, ApplicationContext> timeEntryPredicate() {
        return (timeEntry, applicationContext) ->
                timeEntry.getStatus() != TimeEntryStatus.APPROVED
                        && timeEntry.getStatus() != TimeEntryStatus.CLOSED;
    }
}