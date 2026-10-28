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

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.event.TimeEntryStatusChangedEvent;
import io.jmix.core.DataManager;
import io.jmix.core.FetchPlan;
import io.jmix.core.Id;
import io.jmix.core.event.AttributeChanges;
import io.jmix.core.event.EntityChangedEvent;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.UiEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collections;

@Component("ts_TimeEntryEventListener")
public class TimeEntryEventListener {

    private final DataManager dataManager;
    private final UiEventPublisher uiEventPublisher;

    public TimeEntryEventListener(DataManager dataManager,
                                  UiEventPublisher uiEventPublisher) {
        this.dataManager = dataManager;
        this.uiEventPublisher = uiEventPublisher;
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