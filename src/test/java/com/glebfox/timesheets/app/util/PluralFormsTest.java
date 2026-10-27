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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class PluralFormsTest {

    static final Locale RUSSIAN = Locale.of("ru", "RU");

    @ParameterizedTest
    @CsvSource({
            "0, days.many",
            "1, days.one",
            "2, days.few",
            "5, days.many",
            "11, days.many",
            "14, days.many",
            "21, days.one",
            "22, days.few",
            "25, days.many",
            "111, days.many"
    })
    void russianCountsTakeOneFewOrMany(long count, String expectedKey) {
        assertThat(PluralForms.key("days", count, RUSSIAN)).isEqualTo(expectedKey);
    }

    @ParameterizedTest
    @CsvSource({
            "0, days.many",
            "1, days.one",
            "2, days.many"
    })
    void englishCountsTakeOneOrMany(long count, String expectedKey) {
        assertThat(PluralForms.key("days", count, Locale.ENGLISH)).isEqualTo(expectedKey);
    }
}
