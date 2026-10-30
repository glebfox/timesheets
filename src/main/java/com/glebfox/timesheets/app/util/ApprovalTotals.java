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
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.google.common.base.Preconditions;

import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * The number and the time of time entries, in total and for each status.
 *
 * @param counts the number of entries of each status; a status with no entries may be absent
 * @param times  the time of the entries of each status; a status with no entries may be absent
 */
public record ApprovalTotals(Map<TimeEntryStatus, Integer> counts, Map<TimeEntryStatus, WorkTime> times) {

    private static final List<TimeEntryStatus> DAY_STATUS_ORDER = List.of(
            TimeEntryStatus.REJECTED, TimeEntryStatus.NEW, TimeEntryStatus.APPROVED, TimeEntryStatus.CLOSED);

    public ApprovalTotals {
        counts = Map.copyOf(counts);
        times = Map.copyOf(times);
    }

    /**
     * Totals the specified entries.
     *
     * @param entries the entries to total, each with a status and a spent time
     * @return the totals of the entries, all zero for none
     */
    public static ApprovalTotals of(Collection<TimeEntry> entries) {
        Map<TimeEntryStatus, Integer> counts = new EnumMap<>(TimeEntryStatus.class);
        Map<TimeEntryStatus, WorkTime> times = new EnumMap<>(TimeEntryStatus.class);
        for (TimeEntry entry : entries) {
            TimeEntryStatus status = entry.getStatus();
            Preconditions.checkNotNull(status, "a time entry must have a status");
            counts.merge(status, 1, Integer::sum);
            times.merge(status, entry.getSpentTime(), WorkTime::plus);
        }
        return new ApprovalTotals(counts, times);
    }

    /**
     * Determines the status that stands for all the entries of a day. The first matching rule wins:
     * <ol>
     *     <li>any rejected entry makes the day {@link TimeEntryStatus#REJECTED};</li>
     *     <li>any new entry makes it {@link TimeEntryStatus#NEW}, as it still waits for approval;</li>
     *     <li>any approved entry makes it {@link TimeEntryStatus#APPROVED};</li>
     *     <li>otherwise, with entries all closed, it is {@link TimeEntryStatus#CLOSED}.</li>
     * </ol>
     *
     * @param entries the entries of the day
     * @return the status of the day, empty if there are no entries
     */
    public static Optional<TimeEntryStatus> dayStatus(Collection<TimeEntry> entries) {
        ApprovalTotals totals = of(entries);
        return DAY_STATUS_ORDER.stream().filter(status -> totals.count(status) > 0).findFirst();
    }

    /**
     * Returns the number of all the entries.
     *
     * @return the number of entries of every status
     */
    public int count() {
        return counts.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Returns the time of all the entries.
     *
     * @return the sum of the time of entries of every status, zero for none
     */
    public WorkTime time() {
        return times.values().stream().reduce(WorkTime.ZERO, WorkTime::plus);
    }

    /**
     * Returns the number of the entries of the specified status.
     *
     * @param status the status of the entries
     * @return the number of entries, zero for none
     */
    public int count(TimeEntryStatus status) {
        return counts.getOrDefault(status, 0);
    }

    /**
     * Returns the time of the entries of the specified status.
     *
     * @param status the status of the entries
     * @return the sum of their time, zero for none
     */
    public WorkTime time(TimeEntryStatus status) {
        return times.getOrDefault(status, WorkTime.ZERO);
    }

    /**
     * Determines whether any entry waits for approval.
     *
     * @return {@code true} if there is a new entry
     */
    public boolean hasWaiting() {
        return count(TimeEntryStatus.NEW) > 0;
    }

    /**
     * Determines whether all the entries are accepted. A rejected entry is not: it is not final until it is
     * corrected and approved. No entries are not accepted either, as there is nothing to approve.
     *
     * @return {@code true} if there is an entry, and none is new or rejected
     */
    public boolean isFullyApproved() {
        return count() > 0 && count(TimeEntryStatus.NEW) == 0 && count(TimeEntryStatus.REJECTED) == 0;
    }
}
