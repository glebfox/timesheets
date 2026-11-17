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

package com.glebfox.timesheets.app;

import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.EntityStates;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.metamodel.datatype.EnumClass;
import io.jmix.core.usersubstitution.CurrentUserSubstitution;
import io.jmix.data.AttributeChangesProvider;
import io.jmix.data.ReferenceIdProvider;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.APPROVED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.CLOSED;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.NEW;
import static com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.REJECTED;

/**
 * Judges whether the current user may create, change, or remove a time entry: the write rules of time entries, which
 * the predicates of the row-level role Time entry rules call.
 * <p>
 * The data store asks after the {@code EntitySavingEvent} listeners and the merge, about the merged instance, so the
 * time entry listener has already sent a changed Approved or Rejected entry back to New. The rules therefore judge an
 * update by the entry's stored values, from its change tracking: the old value of an attribute the save changes, the
 * current value of one it does not. A screen asks about an entry it has loaded, a detached instance, which reports no
 * change, so the screen's question is judged on the entry's current values.
 * <p>
 * The rules never load an entity and never flush: an entity query inside a save refreshes the rows the save holds,
 * and a flush clears the change tracking the rules read. The project of a task and the user's part in it come from
 * value queries. A reference is read only where it is loaded, or from the row of a managed instance; a detached
 * instance whose reference is not loaded is refused, as its value is unknown.
 * <p>
 * The user is always the effective user, so under user substitution it is the substituted user.
 */
@Component("ts_TimeEntryAccessSupport")
public class TimeEntryAccessSupport {

    // What the entry says, as opposed to its status and rejection reason, which a reviewer sets
    private static final List<String> CONTENT = List.of("date", "task", "user", "spentTime", "description", "tags");

    // The statuses in which the author changes or removes the entry
    private static final Set<TimeEntryStatus> OPEN_TO_AUTHOR = EnumSet.of(NEW, REJECTED);
    // The statuses from which a reviewer moves the entry, and those a reviewer sets
    private static final Set<TimeEntryStatus> REVIEWABLE = EnumSet.of(NEW, APPROVED, REJECTED);
    private static final Set<TimeEntryStatus> REVIEW_RESULTS = EnumSet.of(APPROVED, REJECTED);

    private final ProjectAccessSupport projectAccessSupport;
    private final AttributeChangesProvider attributeChangesProvider;
    private final ReferenceIdProvider referenceIdProvider;
    private final EntityStates entityStates;
    private final UnconstrainedDataManager unconstrainedDataManager;
    private final CurrentUserSubstitution currentUserSubstitution;

    public TimeEntryAccessSupport(ProjectAccessSupport projectAccessSupport,
                                  AttributeChangesProvider attributeChangesProvider,
                                  ReferenceIdProvider referenceIdProvider,
                                  EntityStates entityStates,
                                  UnconstrainedDataManager unconstrainedDataManager,
                                  CurrentUserSubstitution currentUserSubstitution) {
        this.projectAccessSupport = projectAccessSupport;
        this.attributeChangesProvider = attributeChangesProvider;
        this.referenceIdProvider = referenceIdProvider;
        this.entityStates = entityStates;
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.currentUserSubstitution = currentUserSubstitution;
    }

    /**
     * Tells whether the current user may create the entry: their own, New, without a rejection reason, on a task of
     * a project where they take part in a role other than Observer.
     *
     * @param entry the entry to be created, or the entry a screen asks about
     * @return {@code true} if the current user may create the entry
     */
    public boolean mayCreate(TimeEntry entry) {
        return isCurrentUser(currentReferenceId(entry, "user"))
                && currentStatus(entry) == NEW
                && entityStates.isLoaded(entry, "rejectionReason")
                && StringUtils.isBlank(entry.getRejectionReason())
                && projectAccessSupport.worksOn(currentProjectId(entry));
    }

    /**
     * Tells whether the current user may save the entry as it is, against its stored values.
     * <p>
     * A Closed entry is changed by no one. A change of the status or the rejection reason alone is a review: the user
     * must be someone the approval rule lets review the entry's author on its project, the entry stored New,
     * Approved, or Rejected, and its new status Approved or Rejected. Any other save, one that changes nothing
     * included, is the author's: the entry is theirs and stored New or Rejected, its user and rejection reason stay as
     * they are, its status stays or goes back to New, and a new task must be on a project where they take part in a
     * role other than Observer.
     *
     * @param entry the entry being saved, or an unchanged entry a screen asks about
     * @return {@code true} if the current user may save the entry
     */
    public boolean mayUpdate(TimeEntry entry) {
        TimeEntryStatus storedStatus = storedStatus(entry);
        if (storedStatus == null || storedStatus == CLOSED) {
            return false;
        }

        boolean statusChanged = changed(entry, "status");
        boolean reviewed = statusChanged || changed(entry, "rejectionReason");
        if (reviewed && CONTENT.stream().noneMatch(attribute -> changed(entry, attribute))) {
            // The task is not changed here, so its current project is the stored one
            return REVIEWABLE.contains(storedStatus)
                    && REVIEW_RESULTS.contains(currentStatus(entry))
                    && projectAccessSupport.mayReview(currentProjectId(entry), toUuid(storedReferenceId(entry, "user")));
        }

        return OPEN_TO_AUTHOR.contains(storedStatus)
                && isCurrentUser(storedReferenceId(entry, "user"))
                && !changed(entry, "user")
                && !changed(entry, "rejectionReason")
                && (!statusChanged || currentStatus(entry) == NEW)
                && (!changed(entry, "task") || projectAccessSupport.worksOn(currentProjectId(entry)));
    }

    /**
     * Tells whether the current user may remove the entry: their own, stored New or Rejected.
     *
     * @param entry the entry being removed, or the entry a screen asks about
     * @return {@code true} if the current user may remove the entry
     */
    public boolean mayDelete(TimeEntry entry) {
        TimeEntryStatus storedStatus = storedStatus(entry);
        return storedStatus != null
                && OPEN_TO_AUTHOR.contains(storedStatus)
                && isCurrentUser(storedReferenceId(entry, "user"));
    }

    // A value the change tracking reports as changed may still be the stored one, as when a save sets the same
    // tags again, so the stored and the current values are compared. A detached or new instance reports no change
    private boolean changed(TimeEntry entry, String attribute) {
        if (!attributeChangesProvider.isChanged(entry, attribute)) {
            return false;
        }
        Object stored = attributeChangesProvider.getOldValue(entry, attribute);
        Object current = EntityValues.getValue(entry, attribute);
        return !Objects.equals(comparable(stored), comparable(current));
    }

    // References compare by id, collections of references by their sets of ids, enumerations by their ids, and a
    // blank text as no text
    @Nullable
    private static Object comparable(@Nullable Object value) {
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .map(EntityValues::getId)
                    .collect(Collectors.toSet());
        }
        if (EntityValues.isEntity(value)) {
            return EntityValues.getId(value);
        }
        if (value instanceof EnumClass<?> enumValue) {
            return enumValue.getId();
        }
        if (value instanceof String text) {
            return StringUtils.defaultIfBlank(text, null);
        }
        return value;
    }

    @Nullable
    private TimeEntryStatus storedStatus(TimeEntry entry) {
        if (attributeChangesProvider.isChanged(entry, "status")) {
            Object stored = attributeChangesProvider.getOldValue(entry, "status");
            if (stored instanceof TimeEntryStatus status) {
                return status;
            }
            return stored instanceof String id ? TimeEntryStatus.fromId(id) : null;
        }
        return currentStatus(entry);
    }

    @Nullable
    private TimeEntryStatus currentStatus(TimeEntry entry) {
        return entityStates.isLoaded(entry, "status") ? entry.getStatus() : null;
    }

    @Nullable
    private Object storedReferenceId(TimeEntry entry, String attribute) {
        if (attributeChangesProvider.isChanged(entry, attribute)) {
            Object stored = attributeChangesProvider.getOldValue(entry, attribute);
            return stored == null ? null : EntityValues.getId(stored);
        }
        return currentReferenceId(entry, attribute);
    }

    // A reference that is not loaded is read from the row of a managed instance, which the data store gives the
    // rules; a detached instance does not keep it, and reading it there would load the referenced entity
    @Nullable
    private Object currentReferenceId(Object entity, String attribute) {
        if (entityStates.isLoaded(entity, attribute)) {
            Object reference = EntityValues.getValue(entity, attribute);
            return reference == null ? null : EntityValues.getId(reference);
        }
        if (entityStates.isManaged(entity)) {
            ReferenceIdProvider.RefId refId = referenceIdProvider.getReferenceId(entity, attribute);
            return refId.isLoaded() ? refId.getValue() : null;
        }
        return null;
    }

    @Nullable
    private UUID currentProjectId(TimeEntry entry) {
        if (entityStates.isLoaded(entry, "task")) {
            Task task = entry.getTask();
            if (task == null) {
                return null;
            }
            UUID projectId = toUuid(currentReferenceId(task, "project"));
            return projectId != null ? projectId : loadProjectId(task.getId());
        }
        UUID taskId = toUuid(currentReferenceId(entry, "task"));
        return taskId == null ? null : loadProjectId(taskId);
    }

    // A value query, as in ProjectAccessSupport: it neither flushes the rows the save holds nor refreshes them
    @Nullable
    private UUID loadProjectId(@Nullable UUID taskId) {
        if (taskId == null) {
            return null;
        }
        return unconstrainedDataManager.loadValue("select t.project.id from ts_Task t where t.id = :taskId",
                        UUID.class)
                .parameter("taskId", taskId)
                .optional()
                .orElse(null);
    }

    private boolean isCurrentUser(@Nullable Object userId) {
        return userId != null
                && currentUserSubstitution.getEffectiveUser() instanceof User user
                && userId.equals(user.getId());
    }

    @Nullable
    private static UUID toUuid(@Nullable Object id) {
        return id instanceof UUID uuid ? uuid : null;
    }
}
