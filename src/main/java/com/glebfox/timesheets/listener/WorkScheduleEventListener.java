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

package com.glebfox.timesheets.listener;

import com.glebfox.timesheets.app.WorkScheduleSupport;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.UnconstrainedDataManager;
import io.jmix.core.event.AttributeChanges;
import io.jmix.core.event.EntityChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Keeps exactly one default {@link WorkSchedule} on every save path: marking a schedule
 * as the default unmarks the previous one, and unmarking or deleting the only default is refused.
 */
@Component("ts_WorkScheduleEventListener")
public class WorkScheduleEventListener {

    private final UnconstrainedDataManager dataManager;
    private final WorkScheduleSupport workScheduleSupport;
    private final Messages messages;

    public WorkScheduleEventListener(UnconstrainedDataManager dataManager,
                                     WorkScheduleSupport workScheduleSupport,
                                     Messages messages) {
        this.dataManager = dataManager;
        this.workScheduleSupport = workScheduleSupport;
        this.messages = messages;
    }

    // Runs before commit, so a refusal rolls the save back. The schedules that unmarkDefaultsExcept
    // saves come back here as UPDATED with isDefault turned off, and pass: the new default is already flushed.
    @EventListener
    public void onWorkScheduleChanged(EntityChangedEvent<WorkSchedule> event) {
        Id<WorkSchedule> scheduleId = event.getEntityId();
        switch (event.getType()) {
            case CREATED -> {
                if (isDefault(scheduleId)) {
                    workScheduleSupport.unmarkDefaultsExcept(scheduleId);
                }
            }
            case UPDATED -> {
                AttributeChanges changes = event.getChanges();
                if (changes.isChanged("isDefault")) {
                    if (isDefault(scheduleId)) {
                        workScheduleSupport.unmarkDefaultsExcept(scheduleId);
                    } else if (!workScheduleSupport.hasDefaultExcept(scheduleId)) {
                        refuse();
                    }
                } else if (changes.isChanged("deletedDate")
                        && isDefault(scheduleId)
                        && workScheduleSupport.hasDefaultExcept(scheduleId)) {
                    // A schedule deleted as the default in the same save that marked another
                    // keeps its flag; restored, it comes back as not the default
                    workScheduleSupport.unmarkDefault(scheduleId);
                }
            }
            case DELETED -> {
                // A deleted schedule cannot be loaded; its last values come with the event
                Boolean wasDefault = event.getChanges().getOldValue("isDefault");
                if (Boolean.TRUE.equals(wasDefault) && !workScheduleSupport.hasDefaultExcept(scheduleId)) {
                    refuse();
                }
            }
        }
    }

    private boolean isDefault(Id<WorkSchedule> scheduleId) {
        // A schedule that is not found is a deleted one, which never counts as the default
        return dataManager.load(scheduleId)
                .optional()
                .map(schedule -> Boolean.TRUE.equals(schedule.getIsDefault()))
                .orElse(false);
    }

    private void refuse() {
        throw new IllegalStateException(
                messages.getMessage(WorkScheduleEventListener.class, "defaultWorkScheduleRequired"));
    }
}
