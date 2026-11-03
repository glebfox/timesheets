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

package com.glebfox.timesheets.timeentry;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.timeentry.TimeEntryStatus;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryListView;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.badge.Badge;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.internal.UIInternals;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import io.jmix.core.DataManager;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.View;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * My time entries and Time entries mark a rejected entry and show its reason on hover.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class TimeEntryListViewsUiTest {

    static final String REASON = "Logged on the wrong task.";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;

    static Stream<Class<? extends View<?>>> listViews() {
        return Stream.of(MyTimeEntryListView.class, TimeEntryListView.class);
    }

    @ParameterizedTest
    @MethodSource("listViews")
    void listHasNoRejectionReasonColumn(Class<? extends View<?>> viewClass) {
        DataGrid<TimeEntry> grid = openGrid(viewClass);

        assertThat(grid.getColumnByKey("rejectionReason")).isNull();
    }

    @ParameterizedTest
    @MethodSource("listViews")
    void rejectedRowGetsPartName(Class<? extends View<?>> viewClass) {
        DataGrid<TimeEntry> grid = openGrid(viewClass);

        assertThat(grid.getPartNameGenerator()).isNotNull();
        assertThat(grid.getPartNameGenerator().apply(entry(TimeEntryStatus.REJECTED, REASON)))
                .isEqualTo("rejected-entry");
        assertThat(grid.getPartNameGenerator().apply(entry(TimeEntryStatus.NEW, null))).isNull();
    }

    @ParameterizedTest
    @MethodSource("listViews")
    void rejectedStatusCellShowsReason(Class<? extends View<?>> viewClass) {
        DataGrid<TimeEntry> grid = openGrid(viewClass);
        Grid.Column<TimeEntry> status = Objects.requireNonNull(grid.getColumnByKey("status"));

        assertThat(grid.isTooltipMarkdownEnabled()).isTrue();
        assertThat(status.getTooltipGenerator()).isNotNull();
        assertThat(status.getTooltipGenerator().apply(entry(TimeEntryStatus.REJECTED, REASON))).isEqualTo(REASON);
        assertThat(status.getTooltipGenerator().apply(entry(TimeEntryStatus.NEW, null))).isNull();
        assertThat(status.getTooltipGenerator().apply(entry(TimeEntryStatus.APPROVED, REASON))).isNull();
    }

    @ParameterizedTest
    @MethodSource("listViews")
    void blankReasonHasNoTooltip(Class<? extends View<?>> viewClass) {
        DataGrid<TimeEntry> grid = openGrid(viewClass);
        Grid.Column<TimeEntry> status = Objects.requireNonNull(grid.getColumnByKey("status"));

        assertThat(status.getTooltipGenerator()).isNotNull();
        assertThat(status.getTooltipGenerator().apply(entry(TimeEntryStatus.REJECTED, "  "))).isNull();
    }

    @ParameterizedTest
    @MethodSource("listViews")
    @SuppressWarnings("unchecked")
    void rejectedBadgeHasIcon(Class<? extends View<?>> viewClass) {
        DataGrid<TimeEntry> grid = openGrid(viewClass);
        ComponentRenderer<Badge, TimeEntry> renderer = (ComponentRenderer<Badge, TimeEntry>)
                Objects.requireNonNull(grid.getColumnByKey("status")).getRenderer();

        assertThat(renderer.createComponent(entry(TimeEntryStatus.REJECTED, REASON)).getIcon()).isNotNull();
        assertThat(renderer.createComponent(entry(TimeEntryStatus.NEW, null)).getIcon()).isNull();
    }

    @ParameterizedTest
    @MethodSource("listViews")
    void reloadSizesColumnsAgain(Class<? extends View<?>> viewClass) {
        DataGrid<TimeEntry> grid = openGrid(viewClass);
        browserCalls(grid);

        CollectionLoader<TimeEntry> loader =
                ViewControllerUtils.getViewData(UiTestUtils.getCurrentView()).getLoader("timeEntriesDl");
        loader.load();

        // A Rejected badge, the widest with its icon, may come only with rows loaded after the grid was sized
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("recalculateColumnWidths"));
    }

    private DataGrid<TimeEntry> openGrid(Class<? extends View<?>> viewClass) {
        viewBuilders.view(UiTestUtils.getCurrentView(), viewClass).open();
        View<?> view = UiTestUtils.getCurrentView();
        return UiTestUtils.getComponent(view, "timeEntriesDataGrid");
    }

    private static List<String> browserCalls(DataGrid<TimeEntry> grid) {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == grid.getElement().getNode())
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
    }

    // The grid's generators and renderers take an entry as it is, so it need not be saved
    private TimeEntry entry(TimeEntryStatus status, String rejectionReason) {
        TimeEntry entry = dataManager.create(TimeEntry.class);
        entry.setStatus(status);
        entry.setRejectionReason(rejectionReason);
        return entry;
    }
}
