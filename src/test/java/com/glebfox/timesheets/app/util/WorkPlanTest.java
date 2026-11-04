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
import com.glebfox.timesheets.entity.schedule.HolidayCalendarDayType;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Test
    void findsHolidayDaysOnlyInItsRange() {
        WorkPlan plan = new WorkPlan(
                Map.of(LocalDate.of(2026, 11, 2), WorkTime.ofHours(8),
                        LocalDate.of(2026, 11, 4), WorkTime.ZERO),
                Map.of(LocalDate.of(2026, 11, 4), new WorkPlan.HolidayDay("Unity Day", HolidayCalendarDayType.DAY_OFF)));

        assertThat(plan.findHolidayDay(LocalDate.of(2026, 11, 4)))
                .contains(new WorkPlan.HolidayDay("Unity Day", HolidayCalendarDayType.DAY_OFF));
        assertThat(plan.findHolidayDay(LocalDate.of(2026, 11, 2))).isEmpty();
        assertThatThrownBy(() -> plan.findHolidayDay(LocalDate.of(2026, 11, 3)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesHolidayDaysOutsideItsRange() {
        assertThatThrownBy(() -> new WorkPlan(
                Map.of(LocalDate.of(2026, 11, 2), WorkTime.ofHours(8)),
                Map.of(LocalDate.of(2026, 11, 4), new WorkPlan.HolidayDay("Unity Day", HolidayCalendarDayType.DAY_OFF))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void absencesOutsideTheRangeAreRefused() {
        WorkPlan.AbsencePeriod december = new WorkPlan.AbsencePeriod(UUID.randomUUID(), "Vacation",
                LocalDate.of(2026, 12, 5), LocalDate.of(2026, 12, 7));

        assertThatThrownBy(() -> new WorkPlan(novemberDays(), Map.of(), List.of(december)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void absenceEndingBeforeItStartsIsRefused() {
        WorkPlan.AbsencePeriod backwards = new WorkPlan.AbsencePeriod(UUID.randomUUID(), "Vacation",
                LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 9));

        // 10-9 Nov has no day in the plan either, so only the message tells the order check from the coverage one
        assertThatThrownBy(() -> new WorkPlan(novemberDays(), Map.of(), List.of(backwards)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The absence " + backwards.id() + " ends before it starts");
    }

    @Test
    void absenceOverlappingTheRangeIsKept() {
        WorkPlan.AbsencePeriod acrossStart = new WorkPlan.AbsencePeriod(UUID.randomUUID(), "Vacation",
                LocalDate.of(2026, 10, 28), LocalDate.of(2026, 11, 3));
        WorkPlan.AbsencePeriod acrossEnd = new WorkPlan.AbsencePeriod(UUID.randomUUID(), "Sick leave",
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 12, 4));

        WorkPlan plan = new WorkPlan(novemberDays(), Map.of(), List.of(acrossStart, acrossEnd));

        assertThat(plan.absences()).containsExactly(acrossStart, acrossEnd);
    }

    @Test
    void planWithoutAbsencesHasNone() {
        assertThat(new WorkPlan(novemberDays()).absences()).isEmpty();
        assertThat(new WorkPlan(novemberDays(), Map.of()).absences()).isEmpty();
    }

    private static Map<LocalDate, WorkTime> novemberDays() {
        return LocalDate.of(2026, 11, 1).datesUntil(LocalDate.of(2026, 12, 1))
                .collect(Collectors.toMap(Function.identity(), date -> WorkTime.ofHours(8)));
    }
}
