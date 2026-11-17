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

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.function.SerializableBiConsumer;
import com.vaadin.flow.function.SerializableFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * The buttons that approve in bulk, each in the {@code actions} slot of the part it belongs to: the person's period,
 * a week, or a day.
 * <p>
 * A button stays in its slot from the moment it is added, enabled while its scope has entries to approve and disabled
 * while it has none, so that the parts around it do not move as its entries are approved or put in the reject basket.
 * Whether it is enabled, and the text it shows, come from the entries that its scope covers, asked for through the
 * function given to the constructor, every time {@link #refresh} runs. A click is passed on with the scope of the
 * button, and whether it came from the browser.
 */
final class ApprovalBulkApproveButtons {

    private final SerializableFunction<ApprovalScope, List<TimeEntry>> entriesToApprove;
    private final SerializableBiConsumer<ApprovalScope, Boolean> onClick;
    private final List<Slot> slots = new ArrayList<>();

    /**
     * Creates a set of buttons that has none yet.
     *
     * @param entriesToApprove gives the entries that a bulk approve of a scope takes
     * @param onClick          called with the scope of a clicked button, and whether the click came from the browser
     */
    ApprovalBulkApproveButtons(SerializableFunction<ApprovalScope, List<TimeEntry>> entriesToApprove,
                               SerializableBiConsumer<ApprovalScope, Boolean> onClick) {
        this.entriesToApprove = entriesToApprove;
        this.onClick = onClick;
    }

    /**
     * Adds a button for a scope and puts it in its slot, disabled until the next {@link #refresh} finds entries to
     * approve in its scope.
     *
     * @param slot   the {@code actions} part to put the button in
     * @param button the button, with its id, look, and label
     * @param scope  the days that the button approves
     * @param text   gives the button's text for the number of entries to approve, zero included
     */
    void add(Div slot, Button button, ApprovalScope scope, SerializableFunction<Integer, String> text) {
        button.addClickListener(event -> onClick.accept(scope, event.isFromClient()));
        button.setText(text.apply(0));
        button.setEnabled(false);
        slot.add(button);
        slots.add(new Slot(button, scope, text));
    }

    /**
     * Brings every button up to date with the entries to approve: its text for their number, and enabled only while
     * there are some.
     */
    void refresh() {
        for (Slot slot : slots) {
            int count = entriesToApprove.apply(slot.scope()).size();
            slot.button().setText(slot.text().apply(count));
            slot.button().setEnabled(count > 0);
        }
    }

    /**
     * Forgets the buttons, as when the parts they were in are drawn anew.
     */
    void clear() {
        slots.clear();
    }

    private record Slot(Button button, ApprovalScope scope, SerializableFunction<Integer, String> text) {
    }
}
