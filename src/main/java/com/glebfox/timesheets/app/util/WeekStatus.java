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

/**
 * Where the time spent in a calendar week stands against the time planned for it, as of a given day.
 *
 * @see WeekSummary#getStatus(java.time.LocalDate)
 */
public enum WeekStatus {

    /**
     * More time is spent than planned.
     */
    OVER,

    /**
     * Exactly the planned time is spent.
     */
    ON_PLAN,

    /**
     * Less time is spent than planned, and the week is over.
     */
    SHORT,

    /**
     * Less time is spent than planned, but the week is not over yet.
     */
    IN_PROGRESS,

    /**
     * Less time is spent than planned, and the week has not started yet.
     */
    UPCOMING,

    /**
     * Nothing is planned and nothing is spent: the week has no workdays and no time entries.
     */
    NO_WORKDAYS
}
