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

import io.jmix.flowui.component.sidedialog.SideDialog;
import io.jmix.flowui.util.UnknownOperationResult;
import io.jmix.flowui.view.ChangeTrackerCloseAction;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewValidation;
import org.springframework.stereotype.Component;

/**
 * Asks before a dialog that collects input, but is not a detail view, discards that input on close: a view opened as
 * a dialog, or a side dialog.
 */
@Component("ts_UnsavedInputSupport")
public class UnsavedInputSupport {

    private final ViewValidation viewValidation;

    public UnsavedInputSupport(ViewValidation viewValidation) {
        this.viewValidation = viewValidation;
    }

    /**
     * Asks the framework's unsaved-changes question when the view is closed other than with
     * {@link StandardOutcome#SAVE} while its input differs from how it opened: Yes closes the view with
     * {@link StandardOutcome#DISCARD}, No keeps it open. Called from the view's
     * {@link View.BeforeCloseEvent} handler; the view itself tells whether its input changed.
     *
     * @param event        the view's before-close event
     * @param inputChanged whether any of the view's input differs from how the view opened
     */
    public void askBeforeDiscarding(View.BeforeCloseEvent event, boolean inputChanged) {
        // SAVE takes the input, and a close that does not check for changes, such as a discard, needs no question
        if (!inputChanged
                || event.closedWith(StandardOutcome.SAVE)
                || !(event.getCloseAction() instanceof ChangeTrackerCloseAction closeAction)
                || !closeAction.isCheckForUnsavedChanges()) {
            return;
        }

        View<?> view = event.getSource();
        UnknownOperationResult result = new UnknownOperationResult();
        viewValidation.showUnsavedChangesDialog(view)
                .onDiscard(() -> result.resume(view.close(StandardOutcome.DISCARD)))
                .onCancel(result::fail);
        event.preventClose(result);
    }

    /**
     * Closes a side dialog that collects input, asking the framework's unsaved-changes question first while its input
     * differs from how it opened: Yes closes the dialog, No keeps it open. Called for every way the dialog closes
     * without taking its input: its own cancel button and, from its close-action listener, Esc and a click outside
     * it. A side dialog is a component, not a view, so the question belongs to the view it was opened from.
     *
     * @param dialog       the dialog to close
     * @param owner        the view the dialog was opened from
     * @param inputChanged whether any of the dialog's input differs from how the dialog opened
     */
    public void askBeforeClosing(SideDialog dialog, View<?> owner, boolean inputChanged) {
        if (!inputChanged) {
            dialog.close();
            return;
        }

        viewValidation.showUnsavedChangesDialog(owner)
                .onDiscard(dialog::close);
    }
}
