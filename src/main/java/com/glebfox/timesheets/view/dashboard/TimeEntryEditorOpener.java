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

import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import io.jmix.core.AccessManager;
import io.jmix.core.Metadata;
import io.jmix.flowui.DialogWindows;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.builder.DetailWindowClassBuilder;
import org.springframework.stereotype.Component;

import java.util.function.UnaryOperator;

/**
 * Opens the time entry editor from the dashboard in a dialog, on the current user's own entry: a new one, such as on
 * Log time or Copy to today, or an existing one, such as on a click on a recent entry. The caller shows the control
 * that opens it only if {@link #isOpenPermitted()} or {@link #isCreatePermitted()} allows.
 */
@Component("ts_TimeEntryEditorOpener")
public class TimeEntryEditorOpener {

    private final AccessManager accessManager;
    private final Metadata metadata;
    private final DialogWindows dialogWindows;

    public TimeEntryEditorOpener(AccessManager accessManager, Metadata metadata, DialogWindows dialogWindows) {
        this.accessManager = accessManager;
        this.metadata = metadata;
        this.dialogWindows = dialogWindows;
    }

    /**
     * @return whether the current user may open the time entry editor
     */
    public boolean isOpenPermitted() {
        return DashboardAccess.isViewPermitted(accessManager, TimeEntryDetailView.ID);
    }

    /**
     * @return whether the current user may create time entries
     */
    public boolean isCreatePermitted() {
        UiEntityContext context = new UiEntityContext(metadata.getClass(TimeEntry.class));
        accessManager.applyRegisteredConstraints(context);
        return context.isCreatePermitted();
    }

    /**
     * Opens the editor in a dialog over the view that holds the fragment, as the editor of the current user's own
     * entry.
     *
     * @param origin the fragment the request comes from: the dashboard, or a block
     * @param entity the builder's entity step: which entry the editor opens on, such as
     *               {@code builder -> builder.editEntity(entry)}
     * @param onSave what to do after the editor closes with a save, such as refreshing the dashboard
     */
    public void open(Fragment<?> origin,
                     UnaryOperator<DetailWindowClassBuilder<TimeEntry, TimeEntryDetailView>> entity,
                     Runnable onSave) {
        DetailWindowClassBuilder<TimeEntry, TimeEntryDetailView> builder = dialogWindows
                .detail(FragmentUtils.getHostView(FragmentUtils.getParentController(origin)), TimeEntry.class)
                .withViewClass(TimeEntryDetailView.class);
        entity.apply(builder)
                .withViewConfigurer(editor -> editor.setOwnTimeEntry(true))
                .withAfterCloseListener(closeEvent -> {
                    if (closeEvent.closedWith(StandardOutcome.SAVE)) {
                        onSave.run();
                    }
                })
                .open();
    }
}
