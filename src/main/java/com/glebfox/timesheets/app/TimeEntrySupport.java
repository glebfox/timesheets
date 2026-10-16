/*
 * Copyright 2023 Gleb Gorelov
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

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import io.jmix.core.*;
import io.jmix.core.common.util.Preconditions;
import org.springframework.stereotype.Component;

/**
 * Utility class for managing and manipulating {@link TimeEntry} entities.
 */
@Component("ts_TimeEntrySupport")
public class TimeEntrySupport {

    private final MetadataTools metadataTools;
    private final DataManager dataManager;
    private final Metadata metadata;

    public TimeEntrySupport(MetadataTools metadataTools,
                            DataManager dataManager,
                            Metadata metadata) {
        this.metadataTools = metadataTools;
        this.dataManager = dataManager;
        this.metadata = metadata;
    }

    /**
     * Creates a copy of the provided {@link TimeEntry} instance with a new unique identifier
     * and a status of {@link TimeEntryStatus#NEW}. The method reloads the full state of the
     * source {@link TimeEntry} object, including related entities like user, task, and tags,
     * before creating a copy.
     *
     * @param source the {@link TimeEntry} instance to be copied
     * @return a new {@link TimeEntry} instance that is a copy of the source with a new ID and
     * a status of {@link TimeEntryStatus#NEW}
     */
    public TimeEntry copy(TimeEntry source) {
        Preconditions.checkNotNullArgument(source);

        TimeEntry reloaded = dataManager.load(Id.of(source))
                .fetchPlan(planBuilder ->
                        planBuilder.addFetchPlan(FetchPlan.BASE)
                                .add("user", FetchPlan.BASE)
                                .add("task", FetchPlan.INSTANCE_NAME)
                                .add("tags", FetchPlan.INSTANCE_NAME))
                .one();

        TimeEntry copied = metadataTools.copy(reloaded);
        copied.setId(UuidProvider.createUuid());
        copied.setStatus(TimeEntryStatus.NEW);
        return copied;
    }

    /**
     * Retrieves the count of {@link TimeEntry} entities associated with the current user
     * that have a status of {@link TimeEntryStatus#REJECTED}.
     *
     * @return the number of rejected {@link TimeEntry} records for the current user
     */
    public long getRejectedCount() {
        LoadContext<TimeEntry> loadContext = new LoadContext<>(metadata.getClass(TimeEntry.class));
        loadContext.setQueryString("select e from ts_TimeEntry e " +
                "where e.user.username = :current_user_username " +
                "and e.status = @enum(com.glebfox.timesheets.entity.timeentry.TimeEntryStatus.REJECTED)");

        return dataManager.getCount(loadContext);
    }
}