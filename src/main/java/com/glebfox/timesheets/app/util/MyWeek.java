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
 * A user's week, whole: the time spent against the time planned in it and in each of its days.
 *
 * @param summary the week's total
 * @param days    the seven days of the week, in order
 */
public record MyWeek(WeekSummary summary, List<DaySummary> days) {

    public MyWeek {
        days = List.copyOf(days);
    }
}
