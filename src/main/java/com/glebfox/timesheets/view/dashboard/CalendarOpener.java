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

import com.glebfox.timesheets.view.calendar.CalendarView;
import io.jmix.core.AccessManager;
import io.jmix.flowui.fragment.Fragment;
import io.jmix.flowui.fragment.FragmentUtils;
import io.jmix.tabbedmode.ViewBuilders;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Opens the calendar on the month of a date from a dashboard block, such as on a click on a day or a week of a
 * chart, if the current user may open the calendar; otherwise does nothing.
 */
@Component("ts_CalendarOpener")
public class CalendarOpener {

    private final AccessManager accessManager;
    private final ViewBuilders viewBuilders;

    public CalendarOpener(AccessManager accessManager, ViewBuilders viewBuilders) {
        this.accessManager = accessManager;
        this.viewBuilders = viewBuilders;
    }

    /**
     * @return whether the current user may open the calendar
     */
    public boolean isPermitted() {
        return DashboardAccess.isViewPermitted(accessManager, CalendarView.ID);
    }

    /**
     * Opens the calendar on the month of the date, in a tab of the view that holds the block, if the current user
     * may open the calendar.
     *
     * @param block the fragment the request comes from
     * @param date  the date whose month to open
     */
    public void open(Fragment<?> block, LocalDate date) {
        if (!isPermitted()) {
            return;
        }

        viewBuilders.view(FragmentUtils.getHostView(FragmentUtils.getParentController(block)), CalendarView.class)
                .withViewConfigurer(calendar -> calendar.showDate(date))
                .open();
    }
}
