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

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

/**
 * The time spent on a day against the time planned for it.
 *
 * @param date      the day
 * @param total     the time spent and planned on the day
 * @param offReason why the day is off: the name of the absence type if the day is in an absence, otherwise
 *                  the name of the holiday calendar day if it makes the day a day off, otherwise {@code null}
 */
public record DaySummary(LocalDate date, FactAndPlan total, @Nullable String offReason) {
}
