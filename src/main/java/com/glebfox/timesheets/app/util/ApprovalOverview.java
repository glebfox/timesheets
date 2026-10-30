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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;

/**
 * The time entries of a period, grouped by the people who logged them.
 *
 * @param people the people with their entries, those with entries waiting for approval first, then by name
 * @param totals the totals of the entries of all the people
 */
public record ApprovalOverview(List<ApprovalPerson> people, ApprovalTotals totals) {

    public ApprovalOverview {
        people = List.copyOf(people);
        Preconditions.checkNotNull(totals);
    }

    /**
     * Groups the entries by their users. The entries of equal users, by id, make up one person.
     *
     * @param entries     the entries of a period
     * @param displayName gives the name of a user to order the people by
     * @return the people with waiting entries first, then by name ignoring case
     */
    public static ApprovalOverview of(Collection<TimeEntry> entries, Function<User, String> displayName) {
        Map<UUID, List<TimeEntry>> entriesByUser = new LinkedHashMap<>();
        for (TimeEntry entry : entries) {
            entriesByUser.computeIfAbsent(entry.getUser().getId(), id -> new ArrayList<>()).add(entry);
        }

        List<NamedPerson> named = entriesByUser.values().stream()
                .map(userEntries -> {
                    User user = userEntries.getFirst().getUser();
                    return new NamedPerson(displayName.apply(user),
                            new ApprovalPerson(user, userEntries, ApprovalTotals.of(userEntries)));
                })
                .sorted(Comparator
                        .comparing((NamedPerson person) -> !person.person().totals().hasWaiting())
                        .thenComparing(NamedPerson::name, String.CASE_INSENSITIVE_ORDER))
                .toList();

        return new ApprovalOverview(
                named.stream().map(NamedPerson::person).toList(), ApprovalTotals.of(entries));
    }

    /**
     * Counts the people whose entries are all accepted.
     *
     * @return the number of people whose totals are {@linkplain ApprovalTotals#isFullyApproved() fully approved}
     */
    public int fullyApprovedPeople() {
        return (int) people.stream().filter(person -> person.totals().isFullyApproved()).count();
    }

    /**
     * Finds a person by the id of their user.
     *
     * @param userId the id of the user
     * @return the person, empty if the user has no entries in the period
     */
    public Optional<ApprovalPerson> find(UUID userId) {
        return people.stream().filter(person -> userId.equals(person.user().getId())).findFirst();
    }

    private record NamedPerson(String name, ApprovalPerson person) {
    }
}
