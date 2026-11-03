/*
 * Copyright 2024 Gleb Gorelov
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

package com.glebfox.timesheets.listener;

import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.event.TimeEntryStatusChangedEvent;
import io.jmix.core.DataManager;
import io.jmix.core.EntityStates;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.event.AttributeChanges;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.core.event.EntitySavingEvent;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.UiEventPublisher;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Sends a changed {@link TimeEntry} back to review on every save path, and tells the entry's user to refresh their
 * count of rejected entries after its status changes or it is deleted.
 */
@Component("ts_TimeEntryEventListener")
public class TimeEntryEventListener {

    // The statuses a reviewer has set, which a change of what the entry says sends back to New
    private static final Set<TimeEntryStatus> REVIEWED = EnumSet.of(TimeEntryStatus.APPROVED, TimeEntryStatus.REJECTED);

    private final DataManager dataManager;
    private final UnconstrainedDataManager unconstrainedDataManager;
    private final EntityStates entityStates;
    private final UiEventPublisher uiEventPublisher;

    public TimeEntryEventListener(DataManager dataManager,
                                  UnconstrainedDataManager unconstrainedDataManager,
                                  EntityStates entityStates,
                                  UiEventPublisher uiEventPublisher) {
        this.dataManager = dataManager;
        this.unconstrainedDataManager = unconstrainedDataManager;
        this.entityStates = entityStates;
        this.uiEventPublisher = uiEventPublisher;
    }

    // Runs before the entry is merged, when the change tracking does not see the change yet, so the entry is
    // compared with the stored one. The data store checks row-level update constraints after this handler, on the
    // entry as it is saved here
    @EventListener
    public void onTimeEntrySaving(final EntitySavingEvent<TimeEntry> event) {
        TimeEntry entry = event.getEntity();
        // A New or Closed entry keeps its status, and so does one this save sets to either. A status that is not
        // loaded cannot have been changed, but cannot be told from the stored one without it either
        if (event.isNewEntity()
                || !entityStates.isLoaded(entry, "status")
                || !REVIEWED.contains(entry.getStatus())) {
            return;
        }

        // Soft deletion stays on, so that the stored tags leave out a deleted tag, as the screens' loads do
        unconstrainedDataManager.load(Id.of(entry))
                .fetchPlan(fetchPlanBuilder -> fetchPlanBuilder.addFetchPlan(FetchPlan.LOCAL)
                        .add("user", FetchPlan.INSTANCE_NAME)
                        .add("task", FetchPlan.INSTANCE_NAME)
                        .add("tags", FetchPlan.INSTANCE_NAME))
                .optional()
                // A save that sets the status itself keeps it, as the approval screen's do
                .filter(stored -> stored.getStatus() == entry.getStatus())
                .filter(stored -> contentChanged(entry, stored))
                .ifPresent(stored -> entry.setStatus(TimeEntryStatus.NEW));
    }

    private boolean contentChanged(TimeEntry entry, TimeEntry stored) {
        return differs(entry, stored, "date", TimeEntry::getDate)
                || differs(entry, stored, "spentTime", TimeEntry::getSpentTime)
                || differs(entry, stored, "description",
                timeEntry -> StringUtils.defaultIfBlank(timeEntry.getDescription(), null))
                || differs(entry, stored, "user",
                timeEntry -> timeEntry.getUser() == null ? null : timeEntry.getUser().getId())
                || differs(entry, stored, "task",
                timeEntry -> timeEntry.getTask() == null ? null : timeEntry.getTask().getId())
                || differs(entry, stored, "tags", timeEntry -> tagIds(timeEntry.getTags()));
    }

    // An attribute that the saved entry does not have loaded has not been changed, and a reference that is not
    // loaded cannot be read here
    private boolean differs(TimeEntry entry, TimeEntry stored, String attribute, Function<TimeEntry, ?> value) {
        return entityStates.isLoaded(entry, attribute)
                && !Objects.equals(value.apply(entry), value.apply(stored));
    }

    private static Set<UUID> tagIds(Collection<Tag> tags) {
        return tags == null
                ? Set.of()
                : tags.stream().map(Tag::getId).collect(Collectors.toSet());
    }

    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onTimeEntryChangedAfterCommit(final EntityChangedEvent<TimeEntry> event) {
        AttributeChanges changes = event.getChanges();

        if (EntityChangedEvent.Type.DELETED != event.getType()
                && changes.isChanged("status")) {
            TimeEntry timeEntry = dataManager.load(event.getEntityId())
                    .fetchPlan(fetchPlanBuilder ->
                            fetchPlanBuilder.add("user", FetchPlan.INSTANCE_NAME))
                    .one();
            publishEvent(timeEntry.getUser().getUsername());
        }

        if (EntityChangedEvent.Type.DELETED == event.getType()) {
            Id<User> userId = changes.getOldValue("user");
            if (userId != null) {
                // The user may be soft-deleted, as the entry's reference still reaches them,
                // or removed in the same save, and then there is no one to tell
                dataManager.load(userId)
                        .hint(PersistenceHints.SOFT_DELETION, false)
                        .optional()
                        .ifPresent(user -> publishEvent(user.getUsername()));
            }
        }
    }

    private void publishEvent(String username) {
        uiEventPublisher.publishEventForUsers(
                new TimeEntryStatusChangedEvent(this),
                Collections.singleton(username)
        );
    }
}