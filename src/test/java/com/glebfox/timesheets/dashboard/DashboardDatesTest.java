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

import com.glebfox.timesheets.app.UserTimeZoneSupport;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.glebfox.timesheets.view.dashboard.DashboardDates;
import io.jmix.core.security.CurrentAuthentication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class DashboardDatesTest {

    static final Locale EN = Locale.ENGLISH;
    static final Locale RU = Locale.forLanguageTag("ru-RU");
    static final int YEAR = 2026;

    @Autowired
    DashboardDates dashboardDates;
    @Autowired
    UserTimeZoneSupport userTimeZoneSupport;
    @Autowired
    CurrentAuthentication currentAuthentication;

    @Test
    void dateHasYearOnlyOutsideCurrentYearTest() {
        assertThat(dashboardDates.formatDate(LocalDate.of(YEAR, 11, 10), YEAR, EN)).isEqualTo("Nov 10");
        assertThat(dashboardDates.formatDate(LocalDate.of(YEAR - 1, 12, 30), YEAR, EN)).isEqualTo("Dec 30, 2025");
        assertThat(dashboardDates.formatDate(LocalDate.of(YEAR, 11, 10), YEAR, RU)).isEqualTo("10 нояб.");
        assertThat(dashboardDates.formatDate(LocalDate.of(YEAR + 1, 1, 3), YEAR, RU)).isEqualTo("3 янв. 2027");
    }

    @Test
    void rangeNamesItsMonthOnceWithinOneMonthTest() {
        LocalDate first = LocalDate.of(YEAR, 10, 18);
        LocalDate last = LocalDate.of(YEAR, 10, 24);

        assertThat(dashboardDates.formatDates(first, last, YEAR, EN)).isEqualTo("Oct 18 – 24");
        assertThat(dashboardDates.formatDates(first, last, YEAR, RU)).isEqualTo("18 – 24 окт.");
        assertThat(dashboardDates.formatDates(first.minusYears(1), last.minusYears(1), YEAR, EN))
                .isEqualTo("Oct 18 – 24, 2025");
        assertThat(dashboardDates.formatDates(first.minusYears(1), last.minusYears(1), YEAR, RU))
                .isEqualTo("18 – 24 окт. 2025");
    }

    @Test
    void rangeAcrossMonthsAndYearsTest() {
        assertThat(dashboardDates.formatDates(LocalDate.of(YEAR, 9, 27), LocalDate.of(YEAR, 10, 3), YEAR, EN))
                .isEqualTo("Sep 27 – Oct 3");
        assertThat(dashboardDates.formatDates(LocalDate.of(YEAR, 9, 27), LocalDate.of(YEAR, 10, 3), YEAR, RU))
                .isEqualTo("27 сент. – 3 окт.");
        assertThat(dashboardDates.formatDates(LocalDate.of(YEAR + 1, 9, 27), LocalDate.of(YEAR + 1, 10, 3), YEAR,
                EN)).isEqualTo("Sep 27 – Oct 3, 2027");
        assertThat(dashboardDates.formatDates(LocalDate.of(YEAR - 1, 12, 28), LocalDate.of(YEAR, 1, 3), YEAR, EN))
                .isEqualTo("Dec 28, 2025 – Jan 3, 2026");
        assertThat(dashboardDates.formatDates(LocalDate.of(YEAR - 1, 12, 28), LocalDate.of(YEAR, 1, 3), YEAR, RU))
                .isEqualTo("28 дек. 2025 – 3 янв. 2026");
    }

    @Test
    void rangeOfOneDayIsTheDateTest() {
        LocalDate day = LocalDate.of(YEAR, 1, 1);

        assertThat(dashboardDates.formatDates(day, day, YEAR, EN)).isEqualTo("Jan 1");
    }

    @Test
    void currentUserYearAndLocaleByDefaultTest() {
        LocalDate today = userTimeZoneSupport.today();
        Locale locale = currentAuthentication.getLocale();

        assertThat(dashboardDates.formatDate(today)).isEqualTo(dashboardDates.formatDate(today, today.getYear(), locale))
                .doesNotContain(String.valueOf(today.getYear()));
        assertThat(dashboardDates.formatDate(today.minusYears(1))).contains(String.valueOf(today.getYear() - 1));
    }
}
