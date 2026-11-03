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

package com.glebfox.timesheets.component.approval;

import com.glebfox.timesheets.app.util.ApprovalPeriod;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The titles are formatted with the date patterns of the application's own message bundles, in English and Russian.
 */
class ApprovalPeriodTitleTest {

    static final Locale RUSSIAN = Locale.of("ru", "RU");
    static final String KEY_PREFIX = "com.glebfox.timesheets.component.approval/";

    @Test
    void monthReadsItsNameAndYear() {
        ApprovalPeriod october = ApprovalPeriod.monthOf(YearMonth.of(2026, 10));

        assertThat(title(Locale.ENGLISH, october)).isEqualTo(new Title("October 2026", List.of("2026")));
        assertThat(title(RUSSIAN, october)).isEqualTo(new Title("Октябрь 2026", List.of("2026")));
    }

    @Test
    void weekWithinAMonthNamesItOnce() {
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 10, 18), DayOfWeek.SUNDAY);

        assertThat(title(Locale.ENGLISH, week)).isEqualTo(new Title("Oct 18 – 24 2026", List.of("2026")));
        assertThat(title(RUSSIAN, week)).isEqualTo(new Title("18 – 24 окт. 2026", List.of("2026")));
    }

    @Test
    void weekAcrossTwoMonthsNamesBoth() {
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 9, 28), DayOfWeek.MONDAY);

        assertThat(title(Locale.ENGLISH, week)).isEqualTo(new Title("Sep 28 – Oct 4 2026", List.of("2026")));
        assertThat(title(RUSSIAN, week)).isEqualTo(new Title("28 сент. – 4 окт. 2026", List.of("2026")));
    }

    @Test
    void weekAcrossTwoYearsPutsEachYearBesideItsDate() {
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 12, 28), DayOfWeek.MONDAY);

        assertThat(title(Locale.ENGLISH, week))
                .isEqualTo(new Title("Dec 28 2026 – Jan 3 2027", List.of("2026", "2027")));
        assertThat(title(RUSSIAN, week))
                .isEqualTo(new Title("28 дек. 2026 – 3 янв. 2027", List.of("2026", "2027")));
    }

    @Test
    void periodWithinASentenceKeepsTheMonthAsTheLanguageWritesIt() {
        ApprovalPeriod october = ApprovalPeriod.monthOf(YearMonth.of(2026, 10));
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 10, 18), DayOfWeek.SUNDAY);

        assertThat(formatter(Locale.ENGLISH).formatInText(october)).isEqualTo("October 2026");
        assertThat(formatter(RUSSIAN).formatInText(october)).isEqualTo("октябрь 2026");
        assertThat(formatter(Locale.ENGLISH).formatInText(week)).isEqualTo("Oct 18 – 24 2026");
        assertThat(formatter(RUSSIAN).formatInText(week)).isEqualTo("18 – 24 окт. 2026");
    }

    @Test
    void datesOfPartOfAWeekHaveNoYear() {
        LocalDate first = LocalDate.of(2026, 10, 1);
        LocalDate third = LocalDate.of(2026, 10, 3);

        assertThat(formatter(Locale.ENGLISH).formatDates(first, third)).isEqualTo("Oct 1 – 3");
        assertThat(formatter(Locale.ENGLISH).formatDates(first, first)).isEqualTo("Oct 1");
        assertThat(formatter(RUSSIAN).formatDates(first, third)).isEqualTo("1 – 3 окт.");
        assertThat(formatter(RUSSIAN).formatDates(first, first)).isEqualTo("1 окт.");
    }

    private static Title title(Locale locale, ApprovalPeriod period) {
        List<ApprovalPeriodTitle.Part> parts = formatter(locale).format(period);
        return new Title(
                parts.stream().map(ApprovalPeriodTitle.Part::text).collect(Collectors.joining()),
                parts.stream().filter(ApprovalPeriodTitle.Part::year).map(ApprovalPeriodTitle.Part::text).toList());
    }

    private static ApprovalPeriodTitle formatter(Locale locale) {
        Properties bundle = loadBundle(locale.equals(RUSSIAN) ? "messages_ru_RU.properties" : "messages_en.properties");
        return new ApprovalPeriodTitle(key -> Objects.requireNonNull(bundle.getProperty(KEY_PREFIX + key),
                () -> "No message " + key), locale);
    }

    private static Properties loadBundle(String fileName) {
        Properties properties = new Properties();
        try (Reader reader = new InputStreamReader(Objects.requireNonNull(ApprovalPeriodTitleTest.class
                .getResourceAsStream("/com/glebfox/timesheets/" + fileName), fileName), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }

    /**
     * The whole text of a title and the parts of it that are years.
     */
    private record Title(String text, List<String> years) {
    }
}
