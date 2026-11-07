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

package com.glebfox.timesheets.view.dashboard;

import java.time.LocalTime;

/**
 * The dashboard's greeting by the time of day: morning from 05:00 to 11:59, afternoon from 12:00 to 17:59, and
 * evening otherwise, the night included.
 */
public enum Greeting {

    MORNING("dashboard.greeting.morning"),
    AFTERNOON("dashboard.greeting.afternoon"),
    EVENING("dashboard.greeting.evening");

    private static final LocalTime MORNING_START = LocalTime.of(5, 0);
    private static final LocalTime AFTERNOON_START = LocalTime.NOON;
    private static final LocalTime EVENING_START = LocalTime.of(18, 0);

    private final String messageKey;

    Greeting(String messageKey) {
        this.messageKey = messageKey;
    }

    /**
     * Returns the greeting for a time of day.
     *
     * @param time the time of day in the user's time zone
     * @return the greeting
     */
    public static Greeting of(LocalTime time) {
        if (time.isBefore(MORNING_START) || !time.isBefore(EVENING_START)) {
            return EVENING;
        }
        return time.isBefore(AFTERNOON_START) ? MORNING : AFTERNOON;
    }

    /**
     * Returns the key of the greeting's text in the dashboard's message group, a format with the name as {@code %s}.
     */
    public String getMessageKey() {
        return messageKey;
    }
}
