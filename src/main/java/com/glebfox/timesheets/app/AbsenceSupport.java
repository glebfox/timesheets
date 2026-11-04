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

import com.glebfox.timesheets.entity.absence.Absence;
import com.glebfox.timesheets.entity.user.User;
import io.jmix.core.FetchMode;
import io.jmix.core.FetchPlan;
import io.jmix.core.FluentLoader;
import io.jmix.core.UnconstrainedDataManager;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Finds the absences that the absence screens check.
 */
@Component("ts_AbsenceSupport")
public class AbsenceSupport {

    // The rule that one user's absences do not overlap holds whatever the saving user may read
    private final UnconstrainedDataManager dataManager;

    public AbsenceSupport(UnconstrainedDataManager dataManager) {
        this.dataManager = dataManager;
    }

    /**
     * Finds the user's non-deleted absences that share at least one day with a range, whatever their type,
     * a deleted one included.
     *
     * @param user       the user whose absences to look at
     * @param firstDay   the first day of the range
     * @param lastDay    the last day of the range, the same as the first one for one day
     * @param excludedId the id of an absence to leave out, such as the one being edited, or {@code null}
     * @return the absences ordered by their first day, each with its type's name
     */
    public List<Absence> findOverlapping(User user, LocalDate firstDay, LocalDate lastDay, @Nullable UUID excludedId) {
        // The user is compared by reference, which joins no table
        String query = "select e from ts_Absence e"
                + " where e.user = :user and e.firstDay <= :lastDay and e.lastDay >= :firstDay"
                + (excludedId != null ? " and e.id <> :excludedId" : "")
                + " order by e.firstDay";
        FluentLoader.ByQuery<Absence> loader = dataManager.load(Absence.class)
                .query(query)
                .parameter("user", user)
                .parameter("firstDay", firstDay)
                .parameter("lastDay", lastDay)
                // The type is read in a query of its own, which leaves deleted types in, so an absence of a deleted
                // type is found and named (docs/guidelines/database.md)
                .fetchPlan(planBuilder -> planBuilder.addFetchPlan(FetchPlan.BASE)
                        .add("type", FetchPlan.INSTANCE_NAME, FetchMode.BATCH));
        if (excludedId != null) {
            loader.parameter("excludedId", excludedId);
        }
        return loader.list();
    }
}
