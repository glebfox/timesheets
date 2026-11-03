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

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ApprovalPeriodTest {

    @Test
    void weekOfStartsOnTheFirstDayOfWeek() {
        // Thursday, Oct 22
        LocalDate day = LocalDate.of(2026, 10, 22);

        assertThat(ApprovalPeriod.weekOf(day, DayOfWeek.SUNDAY)).isEqualTo(new ApprovalPeriod(
                ApprovalMode.WEEK, LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 24)));
        assertThat(ApprovalPeriod.weekOf(day, DayOfWeek.MONDAY)).isEqualTo(new ApprovalPeriod(
                ApprovalMode.WEEK, LocalDate.of(2026, 10, 19), LocalDate.of(2026, 10, 25)));
    }

    @Test
    void weekOfTheFirstDayOfWeekItselfStartsThere() {
        // Sunday, Oct 18
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 10, 18), DayOfWeek.SUNDAY);

        assertThat(week.start()).isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(week.end()).isEqualTo(LocalDate.of(2026, 10, 24));
    }

    @Test
    void monthOfCoversTheMonth() {
        ApprovalPeriod month = ApprovalPeriod.monthOf(YearMonth.of(2026, 10));

        assertThat(month).isEqualTo(new ApprovalPeriod(
                ApprovalMode.MONTH, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)));
        assertThat(month.mode()).isEqualTo(ApprovalMode.MONTH);
    }

    @Test
    void defaultWeekIsTheWeekBeforeToday() {
        // Friday, Oct 30
        LocalDate today = LocalDate.of(2026, 10, 30);

        assertThat(ApprovalPeriod.defaultOf(ApprovalMode.WEEK, today, DayOfWeek.SUNDAY)).isEqualTo(new ApprovalPeriod(
                ApprovalMode.WEEK, LocalDate.of(2026, 10, 18), LocalDate.of(2026, 10, 24)));
        assertThat(ApprovalPeriod.defaultOf(ApprovalMode.MONTH, today, DayOfWeek.SUNDAY))
                .isEqualTo(ApprovalPeriod.monthOf(YearMonth.of(2026, 10)));
    }

    @Test
    void previousAndNextStepByThePeriod() {
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 10, 22), DayOfWeek.SUNDAY);

        assertThat(week.previous()).isEqualTo(new ApprovalPeriod(
                ApprovalMode.WEEK, LocalDate.of(2026, 10, 11), LocalDate.of(2026, 10, 17)));
        assertThat(week.next()).isEqualTo(new ApprovalPeriod(
                ApprovalMode.WEEK, LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 31)));

        assertThat(ApprovalPeriod.monthOf(YearMonth.of(2026, 1)).previous())
                .isEqualTo(ApprovalPeriod.monthOf(YearMonth.of(2025, 12)));
        ApprovalPeriod november = ApprovalPeriod.monthOf(YearMonth.of(2026, 10)).next();
        assertThat(november).isEqualTo(ApprovalPeriod.monthOf(YearMonth.of(2026, 11)));
        assertThat(november.end()).isEqualTo(LocalDate.of(2026, 11, 30));
    }

    @Test
    void monthRowsIncludeNeighbouringDaysButContainsOnlyTheMonth() {
        ApprovalPeriod october = ApprovalPeriod.monthOf(YearMonth.of(2026, 10));

        List<List<LocalDate>> sundayRows = october.weekRows(DayOfWeek.SUNDAY);
        assertThat(sundayRows).hasSize(5).allSatisfy(row -> assertThat(row).hasSize(7));
        assertThat(sundayRows.getFirst().getFirst()).isEqualTo(LocalDate.of(2026, 9, 27));
        assertThat(sundayRows.getLast().getLast()).isEqualTo(LocalDate.of(2026, 10, 31));

        List<List<LocalDate>> mondayRows = october.weekRows(DayOfWeek.MONDAY);
        assertThat(mondayRows).hasSize(5).allSatisfy(row -> assertThat(row).hasSize(7));
        assertThat(mondayRows.getFirst().getFirst()).isEqualTo(LocalDate.of(2026, 9, 28));
        assertThat(mondayRows.getLast().getLast()).isEqualTo(LocalDate.of(2026, 11, 1));

        assertThat(october.contains(LocalDate.of(2026, 9, 30))).isFalse();
        assertThat(october.contains(LocalDate.of(2026, 10, 1))).isTrue();
        assertThat(october.contains(LocalDate.of(2026, 10, 31))).isTrue();
        assertThat(october.contains(LocalDate.of(2026, 11, 1))).isFalse();
    }

    @Test
    void weekRowsOfAWeekIsTheWeekItself() {
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 10, 22), DayOfWeek.SUNDAY);

        assertThat(week.weekRows(DayOfWeek.SUNDAY)).containsExactly(week.days());
    }

    @Test
    void weekRowsOfAWeekThatDoesNotStartOnTheFirstDayOfWeekSpanTwoRows() {
        // Sunday, Oct 18 to Saturday, Oct 24, laid out in rows that start on Monday
        ApprovalPeriod week = ApprovalPeriod.weekOf(LocalDate.of(2026, 10, 22), DayOfWeek.SUNDAY);

        List<List<LocalDate>> rows = week.weekRows(DayOfWeek.MONDAY);

        assertThat(rows).hasSize(2).allSatisfy(row -> assertThat(row).hasSize(7));
        assertThat(rows.getFirst().getFirst()).isEqualTo(LocalDate.of(2026, 10, 12));
        assertThat(rows.getLast().getFirst()).isEqualTo(LocalDate.of(2026, 10, 19));
        assertThat(rows.getLast().getLast()).isEqualTo(LocalDate.of(2026, 10, 25));
    }

    @Test
    void aWeekMustLastSevenDays() {
        LocalDate start = LocalDate.of(2026, 10, 18);

        assertThatThrownBy(() -> new ApprovalPeriod(ApprovalMode.WEEK, start, start.plusDays(5)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalPeriod(ApprovalMode.WEEK, start, start.plusDays(7)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aMonthMustRunFromItsFirstToItsLastDay() {
        assertThatThrownBy(() -> new ApprovalPeriod(
                ApprovalMode.MONTH, LocalDate.of(2026, 10, 2), LocalDate.of(2026, 10, 31)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ApprovalPeriod(
                ApprovalMode.MONTH, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void aPeriodMustNotEndBeforeItStarts() {
        assertThatThrownBy(() -> new ApprovalPeriod(
                ApprovalMode.WEEK, LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 18)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void daysListEveryDayOfThePeriod() {
        ApprovalPeriod february = ApprovalPeriod.monthOf(YearMonth.of(2028, 2));

        assertThat(february.days()).hasSize(29);
        assertThat(february.days().getFirst()).isEqualTo(LocalDate.of(2028, 2, 1));
        assertThat(february.days().getLast()).isEqualTo(LocalDate.of(2028, 2, 29));
    }

    @Test
    void weekStartOfFindsTheFirstDayOfWeekOnOrBefore() {
        assertThat(ApprovalPeriod.weekStartOf(LocalDate.of(2026, 10, 22), DayOfWeek.SUNDAY))
                .isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(ApprovalPeriod.weekStartOf(LocalDate.of(2026, 10, 18), DayOfWeek.SUNDAY))
                .isEqualTo(LocalDate.of(2026, 10, 18));
        assertThat(ApprovalPeriod.weekStartOf(LocalDate.of(2026, 10, 17), DayOfWeek.SUNDAY))
                .isEqualTo(LocalDate.of(2026, 10, 11));
    }
}
