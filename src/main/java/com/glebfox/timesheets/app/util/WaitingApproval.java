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

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.user.User;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.List;

/**
 * What waits for the current user's approval: the entries in status New that the approval rule lets the user approve,
 * of any date.
 *
 * @param count  the number of entries
 * @param total  the time spent in them
 * @param oldest the date of the oldest entry, or {@code null} if nothing waits
 * @param people the people the entries belong to, the one with the most entries first, then by name; a person may be
 *               a deleted or inactive user, or one who is no longer a participant
 */
public record WaitingApproval(int count, WorkTime total, @Nullable LocalDate oldest, List<Person> people) {

    public WaitingApproval {
        people = List.copyOf(people);
    }

    /**
     * A person with entries waiting.
     *
     * @param user   the person who logged the time
     * @param count  the number of the person's waiting entries
     * @param oldest the date of the oldest of them
     */
    public record Person(User user, int count, LocalDate oldest) {
    }
}
