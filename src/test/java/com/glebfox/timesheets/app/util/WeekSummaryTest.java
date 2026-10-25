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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The weeks are the rows of September 2026, which starts on Tuesday, with the weeks starting on Sunday.
 */
class WeekSummaryTest {

    static final LocalDate TODAY = LocalDate.of(2026, 9, 23);

    @Test
    void noWorkdaysTest() {
        // Saturday, Aug 1: the only day of its row in August
        WeekSummary week = week(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1), WorkTime.ZERO, WorkTime.ZERO);

        assertThat(week.getStatus(LocalDate.of(2026, 7, 15))).isEqualTo(WeekStatus.NO_WORKDAYS);
        assertThat(week.getStatus(TODAY)).isEqualTo(WeekStatus.NO_WORKDAYS);
    }

    @Test
    void weekendTimeWithoutWorkdaysIsOverTest() {
        WeekSummary week = week(LocalDate.of(2026, 8, 1), LocalDate.of(2026, 8, 1),
                WorkTime.ofHours(2), WorkTime.ZERO);

        assertThat(week.getStatus(TODAY)).isEqualTo(WeekStatus.OVER);
    }

    @Test
    void overTest() {
        WeekSummary week = week(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 5),
                WorkTime.ofHours(36), WorkTime.ofHours(32));

        assertThat(week.getStatus(TODAY)).isEqualTo(WeekStatus.OVER);
    }

    @Test
    void onPlanTest() {
        WeekSummary week = week(LocalDate.of(2026, 9, 13), LocalDate.of(2026, 9, 19),
                WorkTime.ofHours(40), WorkTime.ofHours(40));

        assertThat(week.getStatus(TODAY)).isEqualTo(WeekStatus.ON_PLAN);
    }

    @Test
    void shortTest() {
        WeekSummary week = week(LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 12),
                WorkTime.of(35, 30), WorkTime.ofHours(40));

        assertThat(week.getStatus(TODAY)).isEqualTo(WeekStatus.SHORT);
    }

    @Test
    void inProgressTest() {
        WeekSummary week = week(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 26),
                WorkTime.ofHours(20), WorkTime.ofHours(40));

        assertThat(week.getStatus(TODAY)).isEqualTo(WeekStatus.IN_PROGRESS);
        assertThat(week.getStatus(LocalDate.of(2026, 9, 20))).isEqualTo(WeekStatus.IN_PROGRESS);
        assertThat(week.getStatus(LocalDate.of(2026, 9, 26))).isEqualTo(WeekStatus.IN_PROGRESS);
    }

    @Test
    void currentWeekAtOrOverPlanTest() {
        WeekSummary onPlan = week(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 26),
                WorkTime.ofHours(40), WorkTime.ofHours(40));
        WeekSummary over = week(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 26),
                WorkTime.ofHours(42), WorkTime.ofHours(40));

        assertThat(onPlan.isCurrent(TODAY)).isTrue();
        assertThat(onPlan.getStatus(TODAY)).isEqualTo(WeekStatus.ON_PLAN);
        assertThat(over.isCurrent(TODAY)).isTrue();
        assertThat(over.getStatus(TODAY)).isEqualTo(WeekStatus.OVER);
    }

    @Test
    void upcomingTest() {
        WeekSummary empty = week(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 30),
                WorkTime.ZERO, WorkTime.ofHours(24));
        WeekSummary started = week(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 30),
                WorkTime.ofHours(2), WorkTime.ofHours(24));

        assertThat(empty.getStatus(TODAY)).isEqualTo(WeekStatus.UPCOMING);
        assertThat(started.getStatus(TODAY)).isEqualTo(WeekStatus.UPCOMING);
    }

    @Test
    void todayInNextMonthDaysTest() {
        // Today, Oct 1, is on the same calendar row but outside the displayed month
        WeekSummary week = week(LocalDate.of(2026, 9, 27), LocalDate.of(2026, 9, 30),
                WorkTime.ofHours(10), WorkTime.ofHours(24));
        LocalDate today = LocalDate.of(2026, 10, 1);

        assertThat(week.isCurrent(today)).isFalse();
        assertThat(week.getStatus(today)).isEqualTo(WeekStatus.SHORT);
    }

    @Test
    void firstDayAfterLastDayTest() {
        assertThatThrownBy(() -> week(LocalDate.of(2026, 9, 6), LocalDate.of(2026, 9, 5),
                WorkTime.ZERO, WorkTime.ZERO))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private WeekSummary week(LocalDate firstDay, LocalDate lastDay, WorkTime fact, WorkTime plan) {
        return new WeekSummary(firstDay, lastDay, new FactAndPlan(fact, plan));
    }
}
