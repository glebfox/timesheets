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

package com.glebfox.timesheets.app.util;

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.google.common.base.Preconditions;

import java.util.Comparator;
import java.util.List;

/**
 * A person with the time entries to approve and their totals.
 *
 * @param user    the person who logged the time
 * @param entries the entries of the person, sorted by date, then by task name ignoring case
 * @param totals  the totals of the entries
 */
public record ApprovalPerson(User user, List<TimeEntry> entries, ApprovalTotals totals) {

    private static final Comparator<TimeEntry> ENTRY_ORDER = Comparator
            .comparing(TimeEntry::getDate)
            .thenComparing(entry -> entry.getTask().getName(), String.CASE_INSENSITIVE_ORDER);

    public ApprovalPerson {
        Preconditions.checkNotNull(user);
        Preconditions.checkNotNull(totals);
        entries = entries.stream().sorted(ENTRY_ORDER).toList();
    }
}
