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

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DateRangeTest {

    static final LocalDate MAR_2 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_9 = LocalDate.of(2026, 3, 9);
    static final LocalDate MAR_10 = LocalDate.of(2026, 3, 10);
    static final LocalDate MAR_13 = LocalDate.of(2026, 3, 13);

    @Test
    void ofBothEmptyIsNull() {
        assertThat(DateRange.of(null, null)).isNull();
        assertThat(DateRange.of(MAR_2, null)).isEqualTo(new DateRange(MAR_2, null));
        assertThat(DateRange.of(null, MAR_13)).isEqualTo(new DateRange(null, MAR_13));
    }

    @Test
    void constructorRefusesBothEmpty() {
        assertThatThrownBy(() -> new DateRange(null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void endOrStartTakesTheStartForAnEmptyEnd() {
        assertThat(new DateRange(MAR_2, null).endOrStart()).isEqualTo(MAR_2);
        assertThat(new DateRange(MAR_2, MAR_13).endOrStart()).isEqualTo(MAR_13);
    }

    @Test
    void isEndBeforeStart() {
        assertThat(new DateRange(MAR_10, MAR_9).isEndBeforeStart()).isTrue();
        assertThat(new DateRange(MAR_10, MAR_10).isEndBeforeStart()).isFalse();
        assertThat(new DateRange(MAR_10, null).isEndBeforeStart()).isFalse();
        assertThat(new DateRange(null, MAR_9).isEndBeforeStart()).isFalse();
    }
}
