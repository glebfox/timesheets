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

package com.glebfox.timesheets.entity.timeentry;

import com.glebfox.timesheets.datatype.SpentTime;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.MetadataTools;
import io.jmix.core.entity.annotation.JmixGeneratedValue;
import io.jmix.core.entity.annotation.JmixId;
import io.jmix.core.metamodel.annotation.InstanceName;
import io.jmix.core.metamodel.annotation.JmixEntity;
import io.jmix.core.metamodel.annotation.JmixProperty;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;

import java.time.LocalDate;
import java.util.UUID;

@JmixEntity(name = "ts_TimeEntryCalendarWrapper", annotatedPropertiesOnly = true)
public class TimeEntryCalendarWrapper {

    @JmixProperty
    @JmixGeneratedValue
    @JmixId
    private UUID id;

    private TimeEntry timeEntry;

    private LocalDate bufferedDate;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TimeEntry getTimeEntry() {
        return timeEntry;
    }

    public void setTimeEntry(TimeEntry timeEntry) {
        this.timeEntry = timeEntry;
    }

    @JmixProperty
    public LocalDate getDate() {
        return timeEntry.getDate();
    }

    public void setDate(LocalDate date) {
        // When moving events in the FullCalendar, it sets a new date itself ;-(
        // To prevent setting the modified state in the wrapped entity, in case
        // we have no permissions to update the entity, we buffer the date and
        // apply it after the permissions check.
        bufferedDate = date;
    }

    public boolean dateChanged() {
        return bufferedDate != null;
    }

    public void applyDate() {
        timeEntry.setDate(bufferedDate);
        bufferedDate = null;
    }

    public void discardDate() {
        bufferedDate = null;
    }

    @JmixProperty
    public Task getTask() {
        return timeEntry.getTask();
    }

    @JmixProperty
    public User getUser() {
        return timeEntry.getUser();
    }

    @JmixProperty
    public SpentTime getSpentTime() {
        return timeEntry.getSpentTime();
    }

    @JmixProperty
    public TimeEntryStatus getStatus() {
        return timeEntry.getStatus();
    }

    @JmixProperty
    public String getDescription() {
        return timeEntry.getDescription();
    }

    @JmixProperty
    public String getRejectionReason() {
        return timeEntry.getRejectionReason();
    }

    @JmixProperty
    public String getTitle() {
        StringBuilder sb = new StringBuilder();

        SpentTime spentTime = timeEntry.getSpentTime();
        if (spentTime != null) {
            sb.append(spentTime);
        }

        Task task = timeEntry.getTask();
        if (task != null) {
            if (task.getProject() != null) {
                sb.append(" [").append(task.getProject().getName()).append("]");
            }

            sb.append(" ").append(task.getName());
        }

        return sb.toString();
    }

    @JmixProperty
    public String getStyleName() {
        TimeEntryStatus status = timeEntry.getStatus();
        return status == null ? "time-entry" : "time-entry time-entry-" + status.getId();
    }

    @InstanceName
    public String getInstanceName(MetadataTools metadataTools,
                                  DatatypeFormatter datatypeFormatter) {
        return timeEntry.getInstanceName(metadataTools, datatypeFormatter);
    }
}