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
import com.glebfox.timesheets.app.util.ComponentSupport;
import com.glebfox.timesheets.app.util.PluralForms;
import com.glebfox.timesheets.data.renderer.BadgeStyle;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.vaadin.flow.component.badge.Badge;
import io.jmix.core.Messages;

import java.util.List;
import java.util.Locale;

/**
 * Creates the badges that tell the state of a set of time entries, the same way wherever the approval screen shows
 * one, such as a person's roster card or a week of the month grid.
 */
final class ApprovalStatusBadges {

    private ApprovalStatusBadges() {
    }

    /**
     * Creates the badges of the entries: "N waiting", with a red bare count of the rejected ones beside it when there
     * are any; "N rejected" alone when nothing waits; "✓ Approved" when every entry is accepted.
     *
     * @param totals           the totals of the entries
     * @param componentSupport builds the badges
     * @param messages         gives the texts
     * @param locale           the locale of the texts
     * @return the badges in order, none for no entries
     */
    static List<Badge> of(ApprovalTotals totals, ComponentSupport componentSupport, Messages messages,
                          Locale locale) {
        if (totals.count() == 0) {
            return List.of();
        }

        int waiting = totals.count(TimeEntryStatus.NEW);
        int rejected = totals.count(TimeEntryStatus.REJECTED);

        if (waiting > 0) {
            Badge waitingBadge = componentSupport.createBadge(
                    countText("statusBadge.waiting", waiting, messages, locale), BadgeStyle.WARNING);
            if (rejected == 0) {
                return List.of(waitingBadge);
            }

            // Beside the waiting count, the rejected one is a bare number that names itself to assistive technology
            String rejectedText = countText("statusBadge.rejected", rejected, messages, locale);
            Badge rejectedBadge = componentSupport.createBadge(String.valueOf(rejected), BadgeStyle.ERROR);
            rejectedBadge.getElement().setAttribute("title", rejectedText);
            rejectedBadge.getElement().setAttribute("aria-label", rejectedText);
            return List.of(waitingBadge, rejectedBadge);
        }

        if (rejected > 0) {
            return List.of(componentSupport.createBadge(
                    countText("statusBadge.rejected", rejected, messages, locale), BadgeStyle.ERROR));
        }

        return List.of(componentSupport.createBadge(
                messages.getMessage(ApprovalStatusBadges.class, "statusBadge.approved", locale), BadgeStyle.SUCCESS));
    }

    /**
     * Returns the text a badge tells assistive technology: its label when it has one, as a bare count does, otherwise
     * its text.
     *
     * @param badge a badge from {@link #of}
     * @return what the badge says
     */
    static String spokenTextOf(Badge badge) {
        String label = badge.getElement().getAttribute("aria-label");
        return label != null ? label : badge.getText();
    }

    private static String countText(String key, int count, Messages messages, Locale locale) {
        return messages.formatMessage(ApprovalStatusBadges.class, PluralForms.key(key, count, locale), locale, count);
    }
}
