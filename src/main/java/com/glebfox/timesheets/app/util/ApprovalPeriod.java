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

import com.google.common.base.Preconditions;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * The period whose time entries are approved: a week from the first day of the week, or a calendar month.
 *
 * @param mode  the kind of the period
 * @param start the first day of the period
 * @param end   the last day of the period, not before the first day
 */
public record ApprovalPeriod(ApprovalMode mode, LocalDate start, LocalDate end) {

    private static final int DAYS_IN_WEEK = 7;

    public ApprovalPeriod {
        Preconditions.checkNotNull(mode);
        Preconditions.checkNotNull(start);
        Preconditions.checkNotNull(end);
        Preconditions.checkArgument(!start.isAfter(end), "start must not be after end");
        if (mode == ApprovalMode.WEEK) {
            Preconditions.checkArgument(start.plusDays(DAYS_IN_WEEK - 1).equals(end),
                    "a week must last seven days");
        } else {
            YearMonth month = YearMonth.from(start);
            Preconditions.checkArgument(start.equals(month.atDay(1)) && end.equals(month.atEndOfMonth()),
                    "a month must run from its first day to its last day");
        }
    }

    /**
     * Returns the week that contains the specified day.
     *
     * @param day            any day of the week
     * @param firstDayOfWeek the day the week starts on
     * @return the week from its first day to the day before the next first day
     */
    public static ApprovalPeriod weekOf(LocalDate day, DayOfWeek firstDayOfWeek) {
        LocalDate start = weekStartOf(day, firstDayOfWeek);
        return new ApprovalPeriod(ApprovalMode.WEEK, start, start.plusDays(DAYS_IN_WEEK - 1));
    }

    /**
     * Returns the specified month as a period.
     *
     * @param month the month
     * @return the period from the first to the last day of the month
     */
    public static ApprovalPeriod monthOf(YearMonth month) {
        return new ApprovalPeriod(ApprovalMode.MONTH, month.atDay(1), month.atEndOfMonth());
    }

    /**
     * Returns the period to approve first: for a week, the week before the one that contains the specified day,
     * which is over by then; for a month, the month of that day.
     *
     * @param mode           the kind of the period
     * @param today          the current date
     * @param firstDayOfWeek the day the week starts on
     * @return the period to show when approval is opened
     */
    public static ApprovalPeriod defaultOf(ApprovalMode mode, LocalDate today, DayOfWeek firstDayOfWeek) {
        return switch (mode) {
            case WEEK -> weekOf(today, firstDayOfWeek).previous();
            case MONTH -> monthOf(YearMonth.from(today));
        };
    }

    /**
     * Returns the first day of the week that contains the specified day.
     *
     * @param day            any day of the week
     * @param firstDayOfWeek the day the week starts on
     * @return the specified day if it is the first day of the week, otherwise the closest such day before it
     */
    public static LocalDate weekStartOf(LocalDate day, DayOfWeek firstDayOfWeek) {
        return day.with(TemporalAdjusters.previousOrSame(firstDayOfWeek));
    }

    /**
     * Returns the period of the same kind that comes right before this one.
     *
     * @return the week seven days earlier, or the month before
     */
    public ApprovalPeriod previous() {
        return switch (mode) {
            case WEEK -> new ApprovalPeriod(mode, start.minusDays(DAYS_IN_WEEK), end.minusDays(DAYS_IN_WEEK));
            case MONTH -> monthOf(YearMonth.from(start).minusMonths(1));
        };
    }

    /**
     * Returns the period of the same kind that comes right after this one.
     *
     * @return the week seven days later, or the month after
     */
    public ApprovalPeriod next() {
        return switch (mode) {
            case WEEK -> new ApprovalPeriod(mode, start.plusDays(DAYS_IN_WEEK), end.plusDays(DAYS_IN_WEEK));
            case MONTH -> monthOf(YearMonth.from(start).plusMonths(1));
        };
    }

    /**
     * Determines whether the specified day belongs to the period.
     *
     * @param date the day to check
     * @return {@code true} if the day is from the start to the end of the period, inclusive
     */
    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    /**
     * Lists the days of the period.
     *
     * @return every day from the start to the end of the period, in order
     */
    public List<LocalDate> days() {
        return start.datesUntil(end.plusDays(1)).toList();
    }

    /**
     * Lays the period out as the rows of a calendar. The first row starts on the first day of the week
     * on or before the start, and the last row ends on the day before the first day of the week after the end,
     * so the rows include the neighbouring days that {@link #contains(LocalDate)} rejects.
     *
     * @param firstDayOfWeek the day a row starts on
     * @return the rows of seven days each, in order
     */
    public List<List<LocalDate>> weekRows(DayOfWeek firstDayOfWeek) {
        LocalDate lastRowStart = weekStartOf(end, firstDayOfWeek);
        List<List<LocalDate>> rows = new ArrayList<>();
        for (LocalDate rowStart = weekStartOf(start, firstDayOfWeek); !rowStart.isAfter(lastRowStart);
             rowStart = rowStart.plusDays(DAYS_IN_WEEK)) {
            rows.add(rowStart.datesUntil(rowStart.plusDays(DAYS_IN_WEEK)).toList());
        }
        return List.copyOf(rows);
    }
}
