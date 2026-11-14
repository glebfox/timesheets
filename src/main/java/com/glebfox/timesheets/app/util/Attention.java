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

import java.util.List;

/**
 * What of a user's own time needs their attention.
 *
 * @param rejectedCount the number of the user's rejected time entries
 * @param shortDays     the days of the current and the previous week that are over and short, newest first
 * @param shortWeeks    the recent weeks that are over and short, newest first
 */
public record Attention(long rejectedCount, List<ShortDay> shortDays, List<WeekSummary> shortWeeks) {

    public Attention {
        shortDays = List.copyOf(shortDays);
        shortWeeks = List.copyOf(shortWeeks);
    }
}
