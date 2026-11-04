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

import io.jmix.core.Messages;
import io.jmix.core.metamodel.datatype.DatatypeFormatter;
import io.jmix.core.security.CurrentAuthentication;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

/**
 * Formats a range of days, both ends included, in the current user's locale: the number of days, a summary that
 * names the number and the weekdays, and the dates.
 */
@Component("ts_DateRangeFormatter")
public class DateRangeFormatter {

    private final Messages messages;
    private final DatatypeFormatter datatypeFormatter;
    private final CurrentAuthentication currentAuthentication;

    public DateRangeFormatter(Messages messages,
                              DatatypeFormatter datatypeFormatter,
                              CurrentAuthentication currentAuthentication) {
        this.messages = messages;
        this.datatypeFormatter = datatypeFormatter;
        this.currentAuthentication = currentAuthentication;
    }

    /**
     * Returns a number of days in the form the number takes, such as "14 days" or "1 day".
     *
     * @param days the number of days, not negative
     */
    public String formatDays(long days) {
        return formatDays(days, currentAuthentication.getLocale());
    }

    /**
     * Returns the number of days of a range and its weekdays, such as "8 days, Friday to Friday", or "1 day, Friday"
     * for one day; empty when the range ends before it starts.
     *
     * @param firstDay the first day of the range
     * @param lastDay  the last day of the range, the same as the first one for one day
     */
    public String formatSummary(LocalDate firstDay, LocalDate lastDay) {
        if (lastDay.isBefore(firstDay)) {
            return "";
        }

        Locale locale = currentAuthentication.getLocale();
        long days = ChronoUnit.DAYS.between(firstDay, lastDay) + 1;
        String daysText = formatDays(days, locale);
        if (days == 1) {
            return messages.formatMessage(DateRangeFormatter.class, "dateRange.oneDay", locale,
                    daysText, getMessage("dateRange.weekday." + firstDay.getDayOfWeek(), locale));
        }
        return messages.formatMessage(DateRangeFormatter.class, "dateRange.range", locale, daysText,
                getMessage("dateRange.from." + firstDay.getDayOfWeek(), locale),
                getMessage("dateRange.to." + lastDay.getDayOfWeek(), locale));
    }

    /**
     * Returns the dates of a range, such as "02/03/2026 – 13/03/2026", or one date, such as "02/03/2026", for
     * one day.
     *
     * @param firstDay the first day of the range
     * @param lastDay  the last day of the range, the same as the first one for one day
     */
    public String formatDates(LocalDate firstDay, LocalDate lastDay) {
        if (firstDay.equals(lastDay)) {
            return datatypeFormatter.formatLocalDate(firstDay);
        }
        return messages.formatMessage(DateRangeFormatter.class, "dateRange.dates", currentAuthentication.getLocale(),
                datatypeFormatter.formatLocalDate(firstDay), datatypeFormatter.formatLocalDate(lastDay));
    }

    private String formatDays(long days, Locale locale) {
        return messages.formatMessage(DateRangeFormatter.class, PluralForms.key("dateRange.days", days, locale),
                locale, days);
    }

    private String getMessage(String key, Locale locale) {
        return messages.getMessage(DateRangeFormatter.class, key, locale);
    }
}
