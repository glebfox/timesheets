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
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkPlanTest {

    @Test
    void coversOnlyItsRange() {
        WorkPlan plan = new WorkPlan(Map.of(
                LocalDate.of(2026, 11, 2), WorkTime.ofHours(8),
                LocalDate.of(2026, 11, 7), WorkTime.ZERO));

        assertThat(plan.covers(LocalDate.of(2026, 11, 2))).isTrue();
        assertThat(plan.covers(LocalDate.of(2026, 11, 3))).isFalse();
        assertThat(plan.getPlannedTime(LocalDate.of(2026, 11, 2))).isEqualTo(WorkTime.ofHours(8));
        assertThat(plan.isWorkday(LocalDate.of(2026, 11, 2))).isTrue();
        assertThat(plan.isWorkday(LocalDate.of(2026, 11, 7))).isFalse();
        assertThatThrownBy(() -> plan.getPlannedTime(LocalDate.of(2026, 11, 3)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> plan.isWorkday(LocalDate.of(2026, 11, 3)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
