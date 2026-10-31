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
 * Whether a button is there, and the count its text tells, come from the entries that its scope covers, asked for
 * through the function given to the constructor, every time {@link #refresh} runs. A scope with no entries to approve
 * has no button: it is taken out of its slot, so that an empty slot takes no room. A click is passed on with the scope
 * of the button, and whether it came from the browser.
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
     * Adds a button for a scope. It reaches its slot at the next {@link #refresh}, if its scope has entries to
     * approve.
     *
     * @param slot   the {@code actions} part to put the button in
     * @param button the button, with its id, look, and label
     * @param scope  the days that the button approves
     * @param text   gives the button's text for the number of entries to approve, never zero
     */
    void add(Div slot, Button button, ApprovalScope scope, SerializableFunction<Integer, String> text) {
        button.addClickListener(event -> onClick.accept(scope, event.isFromClient()));
        slots.add(new Slot(slot, button, scope, text));
    }

    /**
     * Brings every button up to date with the entries to approve: a button of a scope with none leaves its slot, one of
     * a scope with some is in its slot and tells their number.
     */
    void refresh() {
        for (Slot slot : slots) {
            int count = entriesToApprove.apply(slot.scope()).size();
            if (count == 0) {
                if (slot.button().getParent().isPresent()) {
                    slot.container().remove(slot.button());
                }
                continue;
            }

            slot.button().setText(slot.text().apply(count));
            if (slot.button().getParent().isEmpty()) {
                slot.container().add(slot.button());
            }
        }
    }

    /**
     * Forgets the buttons, as when the parts they were in are drawn anew.
     */
    void clear() {
        slots.clear();
    }

    private record Slot(Div container, Button button, ApprovalScope scope, SerializableFunction<Integer, String> text) {
    }
}
