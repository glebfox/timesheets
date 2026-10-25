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

import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import io.jmix.core.Id;
import io.jmix.core.UnconstrainedDataManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Keeps exactly one default {@link WorkSchedule}. Works through {@link UnconstrainedDataManager},
 * so the rule holds whatever the saving user may read.
 */
@Component("ts_WorkScheduleSupport")
public class WorkScheduleSupport {

    private final UnconstrainedDataManager dataManager;

    public WorkScheduleSupport(UnconstrainedDataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Unmarks every non-deleted default work schedule other than the given one,
     * in the caller's transaction.
     *
     * @param scheduleId the schedule that stays the default
     */
    @Transactional
    public void unmarkDefaultsExcept(Id<WorkSchedule> scheduleId) {
        List<WorkSchedule> otherDefaults = loadDefaultsExcept(scheduleId, 0);
        if (otherDefaults.isEmpty()) {
            return;
        }

        otherDefaults.forEach(schedule -> schedule.setIsDefault(false));
        dataManager.saveWithoutReload(otherDefaults.toArray());
    }

    /**
     * Checks whether a non-deleted work schedule other than the given one is the default.
     *
     * @param scheduleId the schedule to leave out
     * @return {@code true} if another schedule is the default
     */
    public boolean hasDefaultExcept(Id<WorkSchedule> scheduleId) {
        return !loadDefaultsExcept(scheduleId, 1).isEmpty();
    }

    private List<WorkSchedule> loadDefaultsExcept(Id<WorkSchedule> scheduleId, int maxResults) {
        return dataManager.load(WorkSchedule.class)
                .query("e.isDefault = true and e.id <> :scheduleId")
                .parameter("scheduleId", scheduleId.getValue())
                .maxResults(maxResults)
                .list();
    }
}
