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

import static org.assertj.core.api.Assertions.assertThat;

class FactAndPlanTest {

    @Test
    void differenceTest() {
        assertThat(new FactAndPlan(WorkTime.ofHours(36), WorkTime.ofHours(32)).difference())
                .isEqualTo(WorkTime.ofHours(4));
        assertThat(new FactAndPlan(WorkTime.of(35, 30), WorkTime.ofHours(40)).difference())
                .isEqualTo(WorkTime.of(4, 30));
        assertThat(new FactAndPlan(WorkTime.ofHours(40), WorkTime.ofHours(40)).difference())
                .isEqualTo(WorkTime.ZERO);
    }

    @Test
    void percentOfPlanTest() {
        assertThat(new FactAndPlan(WorkTime.of(131, 30), WorkTime.ofHours(176)).percentOfPlan()).isEqualTo(75);
        assertThat(new FactAndPlan(WorkTime.ofHours(185), WorkTime.ofHours(176)).percentOfPlan()).isEqualTo(105);
        assertThat(new FactAndPlan(WorkTime.ofHours(40), WorkTime.ofHours(40)).percentOfPlan()).isEqualTo(100);
        // 12.5% rounds half up to 13; half even would give 12
        assertThat(new FactAndPlan(WorkTime.ofMinutes(1), WorkTime.ofMinutes(8)).percentOfPlan()).isEqualTo(13);
        assertThat(new FactAndPlan(WorkTime.ZERO, WorkTime.ZERO).percentOfPlan()).isZero();
        assertThat(new FactAndPlan(WorkTime.ZERO, WorkTime.ofHours(40)).percentOfPlan()).isZero();
    }
}
