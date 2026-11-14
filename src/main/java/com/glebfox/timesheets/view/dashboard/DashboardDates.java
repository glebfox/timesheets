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

package com.glebfox.timesheets.view.dashboard;

import io.jmix.core.Messages;
import io.jmix.core.TimeSource;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Formats the dates the dashboard shows, all in one short form, such as "Nov 10", with the year only for a date
 * outside the current user's current year, such as "Dec 30, 2025", and a range as compactly, such as
 * "Oct 18 – 24" or "Sep 27 – Oct 3". The patterns are messages, so each locale orders the parts its own way.
 */
@Component("ts_DashboardDates")
public class DashboardDates {

    private final Messages messages;
    private final TimeSource timeSource;
    private final CurrentAuthentication currentAuthentication;

    public DashboardDates(Messages messages, TimeSource timeSource, CurrentAuthentication currentAuthentication) {
        this.messages = messages;
        this.timeSource = timeSource;
        this.currentAuthentication = currentAuthentication;
    }

    /**
     * Returns a date in the current user's locale, with the year only outside their current year.
     *
     * @param date the date
     */
    public String formatDate(LocalDate date) {
        return formatDate(date, currentYear(), currentAuthentication.getLocale());
    }

    /**
     * Returns a range of dates in the current user's locale, or one date for a range of one day, with the year only
     * where the range reaches outside their current year.
     *
     * @param firstDay the first day of the range
     * @param lastDay  the last day of the range, the same as the first one for one day
     */
    public String formatDates(LocalDate firstDay, LocalDate lastDay) {
        return formatDates(firstDay, lastDay, currentYear(), currentAuthentication.getLocale());
    }

    /**
     * Returns a date in the locale, with the year only when it is not the current year.
     *
     * @param date        the date
     * @param currentYear the year in which a date goes without its year
     * @param locale      the locale
     */
    public String formatDate(LocalDate date, int currentYear, Locale locale) {
        return format(date, date.getYear() == currentYear ? "dates.day" : "dates.dayWithYear", locale);
    }

    /**
     * Returns a range of dates in the locale, or one date for a range of one day: within one month, the month is
     * named once, and the year, when it is not the current year, is named once for a range within one year.
     *
     * @param firstDay    the first day of the range
     * @param lastDay     the last day of the range, the same as the first one for one day
     * @param currentYear the year in which a date goes without its year
     * @param locale      the locale
     */
    public String formatDates(LocalDate firstDay, LocalDate lastDay, int currentYear, Locale locale) {
        if (firstDay.equals(lastDay)) {
            return formatDate(firstDay, currentYear, locale);
        }

        String first;
        String last;
        if (firstDay.getYear() != lastDay.getYear()) {
            first = format(firstDay, "dates.dayWithYear", locale);
            last = format(lastDay, "dates.dayWithYear", locale);
        } else {
            boolean withYear = firstDay.getYear() != currentYear;
            if (firstDay.getMonth() == lastDay.getMonth()) {
                first = format(firstDay, "dates.sameMonth.first", locale);
                last = format(lastDay, withYear ? "dates.sameMonth.lastWithYear" : "dates.sameMonth.last", locale);
            } else {
                first = format(firstDay, "dates.day", locale);
                last = format(lastDay, withYear ? "dates.dayWithYear" : "dates.day", locale);
            }
        }
        return messages.formatMessage(DashboardDates.class, "dates.range", locale, first, last);
    }

    private String format(LocalDate date, String patternKey, Locale locale) {
        String pattern = messages.getMessage(DashboardDates.class, patternKey, locale);
        return DateTimeFormatter.ofPattern(pattern, locale).format(date);
    }

    private int currentYear() {
        return DashboardBlock.today(timeSource, currentAuthentication).getYear();
    }
}
