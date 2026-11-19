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

import com.glebfox.timesheets.app.TimeEntryAccessSupport;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import io.jmix.security.model.RowLevelBiPredicate;
import io.jmix.security.model.RowLevelPolicyAction;
import io.jmix.security.role.annotation.PredicateRowLevelPolicy;
import io.jmix.security.role.annotation.RowLevelRole;
import org.springframework.context.ApplicationContext;

/**
 * The write rules of time entries, on every save whatever the channel and on every question a screen asks: who
 * creates, changes, and removes an entry, in which status, and which of its attributes. The rules themselves are in
 * {@link TimeEntryAccessSupport}.
 */
@RowLevelRole(name = "Time entry rules", code = TimeEntryRulesRole.CODE)
public interface TimeEntryRulesRole {

    String CODE = "time-entry-rules";

    @PredicateRowLevelPolicy(entityClass = TimeEntry.class, actions = RowLevelPolicyAction.CREATE)
    default RowLevelBiPredicate<TimeEntry, ApplicationContext> timeEntryCreate() {
        return (entry, applicationContext) ->
                applicationContext.getBean(TimeEntryAccessSupport.class).mayCreate(entry);
    }

    @PredicateRowLevelPolicy(entityClass = TimeEntry.class, actions = RowLevelPolicyAction.UPDATE)
    default RowLevelBiPredicate<TimeEntry, ApplicationContext> timeEntryUpdate() {
        return (entry, applicationContext) ->
                applicationContext.getBean(TimeEntryAccessSupport.class).mayUpdate(entry);
    }

    @PredicateRowLevelPolicy(entityClass = TimeEntry.class, actions = RowLevelPolicyAction.DELETE)
    default RowLevelBiPredicate<TimeEntry, ApplicationContext> timeEntryDelete() {
        return (entry, applicationContext) ->
                applicationContext.getBean(TimeEntryAccessSupport.class).mayDelete(entry);
    }
}
