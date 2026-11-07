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

package com.glebfox.timesheets.dashboard;

import com.glebfox.timesheets.view.dashboard.Greeting;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

class GreetingTest {

    @Test
    void nightIsEvening() {
        assertThat(Greeting.of(LocalTime.of(4, 59))).isEqualTo(Greeting.EVENING);
    }

    @Test
    void morningStartsAtFive() {
        assertThat(Greeting.of(LocalTime.of(5, 0))).isEqualTo(Greeting.MORNING);
    }

    @Test
    void morningLastsUntilNoon() {
        assertThat(Greeting.of(LocalTime.of(11, 59))).isEqualTo(Greeting.MORNING);
    }

    @Test
    void afternoonStartsAtNoon() {
        assertThat(Greeting.of(LocalTime.of(12, 0))).isEqualTo(Greeting.AFTERNOON);
    }

    @Test
    void afternoonLastsUntilSix() {
        assertThat(Greeting.of(LocalTime.of(17, 59))).isEqualTo(Greeting.AFTERNOON);
    }

    @Test
    void eveningStartsAtSix() {
        assertThat(Greeting.of(LocalTime.of(18, 0))).isEqualTo(Greeting.EVENING);
    }
}
