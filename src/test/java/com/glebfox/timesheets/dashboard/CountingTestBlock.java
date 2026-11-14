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

package com.glebfox.timesheets.dashboard;

import com.glebfox.timesheets.view.dashboard.DashboardBlock;
import com.vaadin.flow.component.html.Div;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentDescriptor;

/**
 * A dashboard block for tests: it counts its refreshes, and shows or hides itself on each refresh as the test sets.
 */
@FragmentDescriptor("counting-test-block.xml")
public class CountingTestBlock extends Fragment<Div> implements DashboardBlock {

    private int refreshes;
    private boolean shown = true;

    @Override
    public void refresh() {
        refreshes++;
        setVisible(shown);
    }

    public int getRefreshes() {
        return refreshes;
    }

    /**
     * Sets whether the block shows itself on its next refresh.
     */
    public void setShown(boolean shown) {
        this.shown = shown;
    }
}
