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

import com.google.common.base.Preconditions;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A range of days as the user entered it: either end may be empty, but not both. What an empty end means is up
 * to the use: one day in an editor, no limit in a filter.
 *
 * @param start the first day, or {@code null} when not entered
 * @param end   the last day, or {@code null} when not entered
 */
public record DateRange(@Nullable LocalDate start, @Nullable LocalDate end) {

    public DateRange {
        Preconditions.checkArgument(start != null || end != null, "A date range needs at least one day");
    }

    /**
     * Creates a range of the given days, or returns {@code null} when both are empty.
     *
     * @param start the first day, or {@code null} when not entered
     * @param end   the last day, or {@code null} when not entered
     */
    public static @Nullable DateRange of(@Nullable LocalDate start, @Nullable LocalDate end) {
        return start == null && end == null ? null : new DateRange(start, end);
    }

    /**
     * @return {@code true} if both days are entered and the last one is before the first one
     */
    public boolean isEndBeforeStart() {
        return start != null && end != null && end.isBefore(start);
    }

    /**
     * Returns the last day, or the first one when the last one is empty: the end of the range where an empty end
     * means one day.
     */
    public LocalDate endOrStart() {
        // A range always has a day, so the start is entered when the end is empty
        return end != null ? end : Objects.requireNonNull(start);
    }
}
