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

package com.glebfox.timesheets.view;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.component.emptystate.GridEmptyState;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.schedule.HolidayCalendar;
import com.glebfox.timesheets.entity.schedule.WorkSchedule;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.view.absence.AbsenceListView;
import com.glebfox.timesheets.view.absencetype.AbsenceTypeListView;
import com.glebfox.timesheets.view.client.ClientLookupView;
import com.glebfox.timesheets.view.holidaycalendar.HolidayCalendarListView;
import com.glebfox.timesheets.view.project.ProjectListView;
import com.glebfox.timesheets.view.projectrole.ProjectRoleListView;
import com.glebfox.timesheets.view.tag.TagLookupView;
import com.glebfox.timesheets.view.taggroup.TagGroupLookupView;
import com.glebfox.timesheets.view.task.MyTaskListView;
import com.glebfox.timesheets.view.task.TaskLookupView;
import com.glebfox.timesheets.view.tasktype.TaskTypeListView;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryListView;
import com.glebfox.timesheets.view.user.UserListView;
import com.glebfox.timesheets.view.workschedule.WorkScheduleListView;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.SaveContext;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.propertyfilter.PropertyFilter;
import io.jmix.flowui.view.View;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

/**
 * Every data grid with an empty state shows it with the right icon and text, and the Refresh button where the grid
 * has a {@code list_refresh} action.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class GridEmptyStatesUiTest {

    static final String NAME_PREFIX = "grid-empty-state-list-test-";

    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    DataManager dataManager;

    List<Id<?>> createdIds = new ArrayList<>();

    static Stream<Arguments> emptyStates() {
        Function<ViewBuilders, View<?>> projectEditor = editorOpener(Project.class);
        return Stream.of(
                // name, opener, grid id, icon, EN text, refresh
                arguments("absences", listOpener(AbsenceListView.class),
                        "absencesDataGrid", "ts:absences", "No absences", true),
                arguments("absence types", listOpener(AbsenceTypeListView.class),
                        "absenceTypesDataGrid", "ts:absence-types", "No absence types", true),
                arguments("holiday calendars", listOpener(HolidayCalendarListView.class),
                        "holidayCalendarsDataGrid", "ts:holiday-calendars", "No holiday calendars", true),
                arguments("projects", listOpener(ProjectListView.class),
                        "projectsDataGrid", "ts:projects", "No projects", true),
                arguments("project roles", listOpener(ProjectRoleListView.class),
                        "projectRolesDataGrid", "ts:project-roles", "No project roles", true),
                arguments("my tasks", listOpener(MyTaskListView.class),
                        "tasksDataGrid", "ts:my-tasks", "No tasks", true),
                arguments("task types", listOpener(TaskTypeListView.class),
                        "taskTypesDataGrid", "ts:task-types", "No task types", true),
                arguments("my time entries", listOpener(MyTimeEntryListView.class),
                        "timeEntriesDataGrid", "ts:my-time-entries", "No time entries", true),
                arguments("time entries", listOpener(TimeEntryListView.class),
                        "timeEntriesDataGrid", "ts:time-entries", "No time entries", true),
                arguments("users", listOpener(UserListView.class),
                        "usersDataGrid", "ts:users", "No users", true),
                arguments("work schedules", listOpener(WorkScheduleListView.class),
                        "workSchedulesDataGrid", "ts:work-schedules", "No work schedules", true),
                arguments("client lookup", listOpener(ClientLookupView.class),
                        "clientsDataGrid", "ts:clients", "No clients", true),
                arguments("tag lookup", listOpener(TagLookupView.class),
                        "tagsDataGrid", "ts:tag", "No tags", true),
                arguments("tag group lookup", listOpener(TagGroupLookupView.class),
                        "tagGroupsDataGrid", "ts:tag-group", "No tag groups", true),
                arguments("task lookup", listOpener(TaskLookupView.class),
                        "tasksDataGrid", "ts:my-tasks", "No tasks", true),
                // A new entity in each editor: its table is empty, and none of them gets Refresh
                arguments("user editor assignments", editorOpener(User.class),
                        "assignmentsDataGrid", "ts:work-schedules", "No assignments", false),
                arguments("work schedule editor assignments", editorOpener(WorkSchedule.class),
                        "assignmentsDataGrid", "ts:users", "No assignments", false),
                arguments("holiday calendar editor days", editorOpener(HolidayCalendar.class),
                        "daysDataGrid", "ts:holiday-calendars", "No days", false),
                arguments("project editor tasks", projectEditor,
                        "tasksDataGrid", "ts:my-tasks", "No tasks", false),
                arguments("project editor participants", projectEditor,
                        "participantsDataGrid", "ts:users", "No participants", false),
                arguments("project editor tag groups", projectEditor,
                        "tagGroupsDataGrid", "ts:tag-group", "No tag groups", false),
                arguments("tag group editor tags", editorOpener(TagGroup.class),
                        "tagsDataGrid", "ts:tag", "No tags", false)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("emptyStates")
    void everyGridShowsItsEmptyState(String name, Function<ViewBuilders, View<?>> opener, String gridId,
                                     String icon, String text, boolean refresh) {
        View<?> view = opener.apply(viewBuilders);
        DataGrid<?> grid = UiTestUtils.getComponent(view, gridId);

        assertThat(grid.getEmptyStateComponent()).isInstanceOf(GridEmptyState.class);
        GridEmptyState emptyState = (GridEmptyState) grid.getEmptyStateComponent();
        assertThat(emptyState.getIcon()).isEqualTo(icon);
        assertThat(emptyState.getText()).isEqualTo(text);
        assertThat(emptyState.getRefreshButton().isVisible()).isEqualTo(refresh);
    }

    @Test
    void refreshKeepsTheAppliedFilter() {
        viewBuilders.view(UiTestUtils.getCurrentView(), TaskTypeListView.class).open();
        View<?> view = UiTestUtils.getCurrentView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "taskTypesDataGrid");
        PropertyFilter<String> nameFilter = UiTestUtils.getComponent(view, "nameFilter");
        nameFilter.setValue(NAME_PREFIX + "a");
        // A value set from the server does not apply the filter by itself
        nameFilter.apply();
        assertThat(grid.getItems().getItems()).isEmpty();

        saveTaskType(NAME_PREFIX + "a1");
        saveTaskType(NAME_PREFIX + "b1");
        ((GridEmptyState) grid.getEmptyStateComponent()).getRefreshButton().click();

        assertThat(grid.getItems().getItems())
                .extracting(TaskType::getName)
                .containsExactly(NAME_PREFIX + "a1");
        assertThat(nameFilter.getValue()).isEqualTo(NAME_PREFIX + "a");
    }

    @AfterEach
    void tearDown() {
        // Task types are soft-deleted, so they are removed for good, reloaded by id
        for (Id<?> id : createdIds.reversed()) {
            dataManager.load(id)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .optional()
                    .ifPresent(entity -> dataManager.save(new SaveContext()
                            .removing(entity)
                            .setHint(PersistenceHints.SOFT_DELETION, false)));
        }
        createdIds.clear();
    }

    /**
     * Opens a list, a lookup, or any other view that needs no entity as the current view.
     */
    private static <V extends View<?>> Function<ViewBuilders, View<?>> listOpener(Class<V> viewClass) {
        return viewBuilders -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), viewClass).open();
            return UiTestUtils.getCurrentView();
        };
    }

    /**
     * Opens the editor of a new entity as the current view.
     */
    private static Function<ViewBuilders, View<?>> editorOpener(Class<?> entityClass) {
        return viewBuilders -> {
            viewBuilders.detail(UiTestUtils.getCurrentView(), entityClass).newEntity().open();
            return UiTestUtils.getCurrentView();
        };
    }

    private void saveTaskType(String name) {
        TaskType taskType = dataManager.create(TaskType.class);
        taskType.setName(name);
        createdIds.add(Id.of(dataManager.save(taskType)));
    }
}
