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

package com.glebfox.timesheets.view.timeentry.approval;

import com.glebfox.timesheets.app.util.ApprovalTotals;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Draws a person's fingerprint for a period: one bar per day, its height the time logged on the day and its class
 * the status of the day, so the theme gives it the status color.
 * <p>
 * The drawing is as tall as {@link #HEIGHT} pixels and stretches to the width of its container: the horizontal units
 * are only proportions, while one vertical unit is one pixel.
 */
public final class ApprovalFingerprint {

    /**
     * The height of the drawing in pixels, which a day of {@link #FULL_MINUTES} or more fills.
     */
    static final int HEIGHT = 24;

    /**
     * The height of the base line of a day without entries, and the least height of a day with any.
     */
    static final int BASE_HEIGHT = 2;

    private static final int FULL_MINUTES = 10 * 60;

    // In proportional units: each day takes the same slot, with the bar centered in it
    private static final int DAY_WIDTH = 10;
    private static final int BAR_WIDTH = 8;
    private static final int BAR_OFFSET = (DAY_WIDTH - BAR_WIDTH) / 2;

    private ApprovalFingerprint() {
    }

    /**
     * Draws the fingerprint of the days.
     *
     * @param days         the days to draw, in order, one bar each
     * @param entriesByDay the entries of each day; a day that is absent or has no entries is drawn as a base line
     * @return the SVG markup, starting with {@code <svg}; a day with entries is a {@code rect} with the classes
     * {@code bar} and its status ({@code new}, {@code approved}, {@code rejected}, or {@code closed}), a day without
     * them a {@code rect} with the class {@code empty}
     */
    public static String svg(List<LocalDate> days, Map<LocalDate, List<TimeEntry>> entriesByDay) {
        StringBuilder svg = new StringBuilder()
                .append("<svg xmlns=\"http://www.w3.org/2000/svg\" class=\"approval-fingerprint\"")
                .append(" viewBox=\"0 0 ").append(days.size() * DAY_WIDTH).append(' ').append(HEIGHT).append('"')
                .append(" width=\"100%\" height=\"").append(HEIGHT).append('"')
                .append(" preserveAspectRatio=\"none\" aria-hidden=\"true\" focusable=\"false\">");

        for (int i = 0; i < days.size(); i++) {
            List<TimeEntry> entries = entriesByDay.getOrDefault(days.get(i), List.of());
            Optional<TimeEntryStatus> status = ApprovalTotals.dayStatus(entries);
            double height = status.isPresent() ? heightOf(entries) : BASE_HEIGHT;
            String className = status.map(value -> "bar " + statusClassName(value)).orElse("empty");

            svg.append("<rect class=\"").append(className).append('"')
                    .append(" x=\"").append(i * DAY_WIDTH + BAR_OFFSET).append('"')
                    .append(" y=\"").append(format(HEIGHT - height)).append('"')
                    .append(" width=\"").append(BAR_WIDTH).append('"')
                    .append(" height=\"").append(format(height)).append("\"/>");
        }

        return svg.append("</svg>").toString();
    }

    private static double heightOf(List<TimeEntry> entries) {
        long minutes = ApprovalTotals.of(entries).time().toMinutes();
        double height = (double) HEIGHT * Math.min(minutes, FULL_MINUTES) / FULL_MINUTES;
        return Math.max(height, BASE_HEIGHT);
    }

    private static String statusClassName(TimeEntryStatus status) {
        return switch (status) {
            case NEW -> "new";
            case APPROVED -> "approved";
            case REJECTED -> "rejected";
            case CLOSED -> "closed";
        };
    }

    // SVG takes a dot for decimals whatever the user's locale
    private static String format(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
