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

import com.glebfox.timesheets.app.util.ApprovalMode;
import com.glebfox.timesheets.app.util.ApprovalPeriod;
import com.google.common.base.Preconditions;
import io.jmix.core.Messages;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Formats the titles of approval periods, such as "October 2026", "Oct 18 – 24 2026", or "Oct 25 – Nov 1 2026", with
 * the date patterns of its message group.
 * <p>
 * A title is a list of parts, so that a year can be shown apart from the dates, as the secondary text it is. A week
 * across two years puts each year beside its own date, as in "Dec 28 2026 – Jan 3 2027".
 */
public final class ApprovalPeriodTitle {

    private static final String RANGE_KEY = "periodTitle.range";
    private static final String SAME_MONTH_KEY = "periodTitle.week.sameMonth";
    private static final String OTHER_MONTH_KEY = "periodTitle.week.otherMonth";
    private static final String PLACEHOLDER = "%s";
    private static final Part SPACE = new Part(" ", false);

    private final Function<String, String> messages;
    private final Locale locale;

    /**
     * Creates the formatter with the date patterns of the application's message bundle.
     *
     * @param messages gives the date patterns
     * @param locale   the locale of the month names and the dates
     */
    public ApprovalPeriodTitle(Messages messages, Locale locale) {
        this(key -> messages.getMessage(ApprovalPeriodTitle.class, key, locale), locale);
    }

    /**
     * Creates the formatter.
     *
     * @param messages gives the text of a key of the formatter's message group, such as {@code periodTitle.range}
     * @param locale   the locale of the month names and the dates
     */
    ApprovalPeriodTitle(Function<String, String> messages, Locale locale) {
        this.messages = messages;
        this.locale = locale;
    }

    /**
     * Formats the title of a period.
     *
     * @param period a month or a week
     * @return the parts of the title in order; their texts together make the whole title
     */
    public List<Part> format(ApprovalPeriod period) {
        LocalDate start = period.start();
        LocalDate end = period.end();

        if (period.mode() == ApprovalMode.MONTH) {
            String month = StringUtils.capitalize(start.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale));
            return List.of(new Part(month, false), SPACE, yearOf(start));
        }

        if (start.getYear() == end.getYear()) {
            return List.of(new Part(formatDates(start, end), false), SPACE, yearOf(start));
        }

        // The range is written around two placeholders, whatever stands before, between, and after them
        String[] around = messages.apply(RANGE_KEY).split(PLACEHOLDER, -1);
        Preconditions.checkState(around.length == 3, "The range must have two placeholders: %s", RANGE_KEY);

        List<Part> parts = new ArrayList<>();
        addText(parts, around[0]);
        parts.addAll(List.of(new Part(formatDate(start, OTHER_MONTH_KEY + ".start"), false), SPACE, yearOf(start)));
        addText(parts, around[1]);
        parts.addAll(List.of(new Part(formatDate(end, OTHER_MONTH_KEY + ".end"), false), SPACE, yearOf(end)));
        addText(parts, around[2]);
        return List.copyOf(parts);
    }

    /**
     * Formats a period as a sentence names it, such as "October 2026" in English and "октябрь 2026" in Russian: a
     * month's name as the language writes it within a sentence, where a title capitalizes it, and a week's dates as
     * its title has them.
     *
     * @param period a month or a week
     * @return the period's name, in one text
     */
    public String formatInText(ApprovalPeriod period) {
        if (period.mode() == ApprovalMode.MONTH) {
            LocalDate start = period.start();
            return start.getMonth().getDisplayName(TextStyle.FULL_STANDALONE, locale) + SPACE.text()
                    + start.getYear();
        }
        return format(period).stream().map(Part::text).collect(Collectors.joining());
    }

    /**
     * Formats the dates of days within one year, without the year, such as "Oct 18 – 24", "Oct 25 – Nov 1", or a
     * single "Oct 1".
     *
     * @param start the first day
     * @param end   the last day, not before the first one
     * @return the dates as a week's title shows them
     */
    public String formatDates(LocalDate start, LocalDate end) {
        Preconditions.checkArgument(!end.isBefore(start), "The end must not be before the start");
        if (start.equals(end)) {
            return formatDate(start, OTHER_MONTH_KEY + ".start");
        }

        String keyPrefix = YearMonth.from(start).equals(YearMonth.from(end)) ? SAME_MONTH_KEY : OTHER_MONTH_KEY;
        return messages.apply(RANGE_KEY).formatted(
                formatDate(start, keyPrefix + ".start"), formatDate(end, keyPrefix + ".end"));
    }

    private String formatDate(LocalDate date, String patternKey) {
        return date.format(DateTimeFormatter.ofPattern(messages.apply(patternKey), locale));
    }

    private static Part yearOf(LocalDate date) {
        return new Part(String.valueOf(date.getYear()), true);
    }

    private static void addText(List<Part> parts, String text) {
        if (!text.isEmpty()) {
            parts.add(new Part(text, false));
        }
    }

    /**
     * A part of a title.
     *
     * @param text the text of the part
     * @param year whether the part is a year, which is shown as secondary text
     */
    public record Part(String text, boolean year) {
    }
}
