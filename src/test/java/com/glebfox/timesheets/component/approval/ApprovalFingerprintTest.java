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

import com.glebfox.timesheets.datatype.WorkTime;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ApprovalFingerprintTest {

    // Monday, Oct 19 to Sunday, Oct 25
    static final LocalDate MONDAY = LocalDate.of(2026, 10, 19);
    static final LocalDate TUESDAY = MONDAY.plusDays(1);
    static final LocalDate WEDNESDAY = MONDAY.plusDays(2);
    static final List<LocalDate> WEEK = MONDAY.datesUntil(MONDAY.plusDays(7)).toList();

    @Test
    void drawsABarPerDayByHoursAndStatus() {
        String markup = ApprovalFingerprint.svg(WEEK, Map.of(
                MONDAY, List.of(entry(TimeEntryStatus.NEW, 8)),
                WEDNESDAY, List.of(entry(TimeEntryStatus.APPROVED, 6), entry(TimeEntryStatus.REJECTED, 6))));

        // What the Vaadin Svg component accepts as is
        assertThat(markup).startsWith("<svg");

        Element svg = parse(markup);
        List<Element> rects = svg.select("rect");
        assertThat(rects).hasSize(7);
        assertThat(rects).extracting(rect -> rect.attr("class")).containsOnly("bar new", "empty", "bar rejected");

        double fullHeight = Double.parseDouble(svg.attr("height"));
        Element monday = rects.get(0);
        Element tuesday = rects.get(1);
        Element wednesday = rects.get(2);

        assertThat(monday.attr("class")).isEqualTo("bar new");
        assertThat(heightOf(monday)).isCloseTo(fullHeight * 0.8, within(0.01));

        assertThat(tuesday.attr("class")).isEqualTo("empty");
        assertThat(heightOf(tuesday)).isEqualTo(2);

        // 12 hours: a rejected entry makes the day rejected, and the bar is capped at 10 hours
        assertThat(wednesday.attr("class")).isEqualTo("bar rejected");
        assertThat(heightOf(wednesday)).isEqualTo(fullHeight);

        // Every bar stands on the bottom line
        assertThat(rects).allSatisfy(rect ->
                assertThat(Double.parseDouble(rect.attr("y")) + heightOf(rect)).isCloseTo(fullHeight, within(0.01)));
    }

    @Test
    void barsFollowTheOrderOfTheDays() {
        String markup = ApprovalFingerprint.svg(List.of(WEDNESDAY, TUESDAY), Map.of(
                WEDNESDAY, List.of(entry(TimeEntryStatus.CLOSED, 4)),
                TUESDAY, List.of(entry(TimeEntryStatus.APPROVED, 4))));

        List<Element> rects = parse(markup).select("rect");
        assertThat(rects).extracting(rect -> rect.attr("class")).containsExactly("bar closed", "bar approved");
        assertThat(Double.parseDouble(rects.get(0).attr("x")))
                .isLessThan(Double.parseDouble(rects.get(1).attr("x")));
    }

    private static Element parse(String markup) {
        Document document = Jsoup.parse(markup, "", Parser.xmlParser());
        return document.selectFirst("svg");
    }

    private static double heightOf(Element rect) {
        return Double.parseDouble(rect.attr("height"));
    }

    private static TimeEntry entry(TimeEntryStatus status, int hours) {
        TimeEntry entry = new TimeEntry();
        entry.setId(UUID.randomUUID());
        entry.setStatus(status);
        entry.setSpentTime(WorkTime.ofHours(hours));
        return entry;
    }
}
