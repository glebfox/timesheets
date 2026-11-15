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

import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentOwner;
import io.jmix.flowui.fragment.FragmentUtils;

/**
 * A block of the dashboard: a fragment nested in {@link DashboardFragment}, declared in the dashboard's descriptor
 * inside the container of its section, {@code myTimeBlocks} or {@code myTeamBlocks}, in the order the spec gives.
 * <p>
 * The block's root is a {@code card}, titled with its {@code title} attribute, so it takes Aura's card look. On the
 * section's grid it is one column wide; the class {@code wide} makes it two columns wide where the grid has two or
 * more, and the class {@code full} spans the whole row. A section may place its blocks by classes of its own instead,
 * as My team does, so that every row of its grid is full.
 * <p>
 * The dashboard calls {@link #refresh()} each time it is shown and on its Refresh button, except on the blocks of
 * the My team section for a user who does not see that section; a block loads nothing before, so it loads nothing on
 * its own {@code ReadyEvent}. A block that has nothing it may show hides itself with
 * {@code setVisible(false)} in {@code refresh()}, and shows itself again in a later one that finds something; a
 * section whose blocks are all hidden hides too.
 * <p>
 * A block that changes data the dashboard shows, such as through an editor opened in a dialog, asks the whole
 * dashboard to reload with {@link #refreshDashboard()} after the save, so every block, not only its own, shows it.
 */
public interface DashboardBlock {

    /**
     * Loads the block's data again and shows or hides the block by what it finds.
     */
    void refresh();

    /**
     * Refreshes the dashboard that holds this block, every block and the header included. The dashboard is found
     * among the block's parent controllers, so the block is a fragment created with the dashboard, or a fragment
     * inside it, as its parent.
     *
     * @throws IllegalStateException if this block is not a fragment held by a {@link DashboardFragment}
     */
    default void refreshDashboard() {
        if (!(this instanceof Fragment<?> fragment)) {
            throw new IllegalStateException("A dashboard block must be a fragment: " + getClass().getName());
        }

        FragmentOwner owner = FragmentUtils.getParentController(fragment);
        while (!(owner instanceof DashboardFragment)) {
            if (!(owner instanceof Fragment<?> parent)) {
                throw new IllegalStateException("No dashboard holds the block " + getClass().getName());
            }
            owner = FragmentUtils.getParentController(parent);
        }
        ((DashboardFragment) owner).refresh();
    }
}
