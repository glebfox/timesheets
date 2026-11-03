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

import com.glebfox.timesheets.app.util.ApprovalTotals;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;

import java.util.List;

/**
 * A bar split by the statuses of time entries: one segment per status with entries, as wide as their share of the
 * entries, the settled time first, then what waits, then what was sent back.
 * <p>
 * The bar has the class {@code approval-status-bar}, and each segment the class of its status, by which it takes the
 * status color from the theme. The bar is hidden from assistive technology: the texts beside it tell the same.
 */
public class ApprovalStatusBar extends Composite<Div> implements HasStyle {

    static final String CLASS_NAME = "approval-status-bar";

    private static final List<TimeEntryStatus> ORDER = List.of(
            TimeEntryStatus.CLOSED, TimeEntryStatus.APPROVED, TimeEntryStatus.NEW, TimeEntryStatus.REJECTED);

    /**
     * Creates an empty bar.
     */
    public ApprovalStatusBar() {
        getContent().addClassName(CLASS_NAME);
        getContent().getElement().setAttribute("aria-hidden", "true");
    }

    /**
     * Splits the bar by the statuses of the entries.
     *
     * @param totals the totals of the entries; with none, the bar is empty
     */
    public void setTotals(ApprovalTotals totals) {
        Div bar = getContent();
        bar.removeAll();
        for (TimeEntryStatus status : ORDER) {
            int count = totals.count(status);
            if (count > 0) {
                Span segment = new Span();
                segment.addClassName(ApprovalStatusClassNames.of(status));
                // A share computed from the data, so it is set in place rather than by a class
                segment.getStyle().set("flex-grow", String.valueOf(count));
                bar.add(segment);
            }
        }
    }
}
