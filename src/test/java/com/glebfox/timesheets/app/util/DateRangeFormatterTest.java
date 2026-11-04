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

import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import io.jmix.core.security.ClientDetails;
import io.jmix.core.security.SecurityContextHelper;
import io.jmix.core.security.SystemAuthenticator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.AbstractAuthenticationToken;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class DateRangeFormatterTest {

    static final Locale RUSSIAN = Locale.of("ru", "RU");

    static final LocalDate MAR_2_2026 = LocalDate.of(2026, 3, 2);
    static final LocalDate MAR_13_2026 = LocalDate.of(2026, 3, 13);
    static final LocalDate DEC_31_2026 = LocalDate.of(2026, 12, 31);
    static final LocalDate JAN_1_2027 = LocalDate.of(2027, 1, 1);
    static final LocalDate JAN_8_2027 = LocalDate.of(2027, 1, 8);

    @Autowired
    DateRangeFormatter dateRangeFormatter;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    @Test
    void daysTakeTheFormOfTheirCount() {
        assertThat(dateRangeFormatter.formatDays(14)).isEqualTo("14 days");
        assertThat(dateRangeFormatter.formatDays(1)).isEqualTo("1 day");
    }

    @Test
    void summaryNamesTheCountAndTheWeekdays() {
        assertThat(dateRangeFormatter.formatSummary(JAN_1_2027, JAN_8_2027)).isEqualTo("8 days, Friday to Friday");
        assertThat(dateRangeFormatter.formatSummary(JAN_1_2027, JAN_1_2027)).isEqualTo("1 day, Friday");
    }

    @Test
    void summaryOfARangeThatEndsBeforeItStartsIsEmpty() {
        assertThat(dateRangeFormatter.formatSummary(JAN_1_2027, DEC_31_2026)).isEmpty();
    }

    @Test
    void datesShowBothDaysOrTheOneDay() {
        assertThat(dateRangeFormatter.formatDates(MAR_2_2026, MAR_13_2026)).isEqualTo("02/03/2026 – 13/03/2026");
        assertThat(dateRangeFormatter.formatDates(MAR_2_2026, MAR_2_2026)).isEqualTo("02/03/2026");
    }

    @Test
    void russianTextsTakeTheFormsAndCasesOfTheirWords() {
        systemAuthenticator.runWithUser("admin", () -> {
            ((AbstractAuthenticationToken) Objects.requireNonNull(SecurityContextHelper.getAuthentication()))
                    .setDetails(ClientDetails.builder().locale(RUSSIAN).build());

            assertThat(dateRangeFormatter.formatDays(14)).isEqualTo("14 дней");
            assertThat(dateRangeFormatter.formatDays(1)).isEqualTo("1 день");
            assertThat(dateRangeFormatter.formatDays(2)).isEqualTo("2 дня");

            assertThat(dateRangeFormatter.formatSummary(JAN_1_2027, JAN_8_2027))
                    .isEqualTo("8 дней, с пятницы по пятницу");
            assertThat(dateRangeFormatter.formatSummary(JAN_1_2027, JAN_1_2027)).isEqualTo("1 день, пятница");

            assertThat(dateRangeFormatter.formatDates(MAR_2_2026, MAR_13_2026)).isEqualTo("02.03.2026 – 13.03.2026");
        });
    }
}
