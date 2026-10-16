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