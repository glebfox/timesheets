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

import com.glebfox.timesheets.view.approval.ApprovalOpening;
import com.glebfox.timesheets.view.approval.TimeEntryApprovalView;
import io.jmix.core.AccessManager;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.tabbedmode.ViewBuilders;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * Opens the approval screen from a dashboard block, as it opens from the menu or in a given state, such as on a
 * click on a person, if the current user may open the approval screen; otherwise does nothing.
 */
@Component("ts_ApprovalOpener")
public class ApprovalOpener {

    private final AccessManager accessManager;
    private final ViewBuilders viewBuilders;

    public ApprovalOpener(AccessManager accessManager, ViewBuilders viewBuilders) {
        this.accessManager = accessManager;
        this.viewBuilders = viewBuilders;
    }

    /**
     * @return whether the current user may open the approval screen
     */
    public boolean isPermitted() {
        return DashboardAccess.isViewPermitted(accessManager, TimeEntryApprovalView.ID);
    }

    /**
     * Opens the approval screen in a tab of the view that holds the block, if the current user may open it.
     *
     * @param block   the fragment the request comes from
     * @param opening the state to open the screen in, or {@code null} to open it as the menu does
     */
    public void open(Fragment<?> block, @Nullable ApprovalOpening opening) {
        if (!isPermitted()) {
            return;
        }

        viewBuilders.view(FragmentUtils.getHostView(FragmentUtils.getParentController(block)),
                        TimeEntryApprovalView.class)
                .withViewConfigurer(view -> {
                    if (opening != null) {
                        view.setOpening(opening);
                    }
                })
                .open();
    }
}
