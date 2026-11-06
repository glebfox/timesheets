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

package com.glebfox.timesheets.component.emptystate;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.security.UiMinimalRole;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.internal.UIInternals;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.app.datagrid.DataGridEmptyStateByPermissionsFragment;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
class GridEmptyStateUiTest {

    static final String NAME_PREFIX = "grid-empty-state-test-";
    static final String USERNAME_PREFIX = "grid-empty-state-ui-test-";

    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    DataManager dataManager;
    @Autowired
    Messages messages;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void loadsIconAndText() {
        GridEmptyState emptyState = emptyStateOf(openView(), "refreshGrid");

        assertThat(emptyState.getIcon()).isEqualTo("ts:task-types");
        assertThat(emptyState.getText()).isEqualTo("No task types");
        assertThat(emptyState.hasClassName("grid-empty-state")).isTrue();
    }

    @Test
    void refreshShowsWhereTheGridHasListRefresh() {
        JmixButton refreshButton = emptyStateOf(openView(), "refreshGrid").getRefreshButton();

        assertThat(refreshButton.isVisible()).isTrue();
        assertThat(refreshButton.getText()).isEqualTo(messages.getMessage("actions.Refresh"));
    }

    @Test
    void noRefreshWithoutListRefresh() {
        JmixButton refreshButton = emptyStateOf(openView(), "plainGrid").getRefreshButton();

        assertThat(refreshButton.isVisible()).isFalse();
    }

    @Test
    void refreshFollowsTheActionsState() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        Action refreshAction = Objects.requireNonNull(grid.getAction("refresh"));
        JmixButton refreshButton = emptyStateOf(view, "refreshGrid").getRefreshButton();

        refreshAction.setEnabled(false);
        assertThat(refreshButton.isVisible()).isFalse();

        refreshAction.setEnabled(true);
        assertThat(refreshButton.isVisible()).isTrue();

        refreshAction.setVisible(false);
        assertThat(refreshButton.isVisible()).isFalse();

        refreshAction.setVisible(true);
        assertThat(refreshButton.isVisible()).isTrue();

        // Shown only while the action is both visible and enabled
        refreshAction.setEnabled(false);
        refreshAction.setVisible(false);
        refreshAction.setVisible(true);
        assertThat(refreshButton.isVisible()).isFalse();
    }

    @Test
    void refreshLoadsRowsSavedAfterOpening() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        assertThat(grid.getItems().getItems()).isEmpty();

        saveTaskType(NAME_PREFIX + 1);
        emptyStateOf(view, "refreshGrid").getRefreshButton().click();

        assertThat(grid.getItems().getItems())
                .extracting(TaskType::getName)
                .containsExactly(NAME_PREFIX + 1);
    }

    @Test
    void reattachBindsOnce() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        Action refreshAction = Objects.requireNonNull(grid.getAction("refresh"));
        GridEmptyState emptyState = emptyStateOf(view, "refreshGrid");
        JmixButton refreshButton = emptyState.getRefreshButton();
        HasComponents parent = (HasComponents) grid.getParent().orElseThrow();

        parent.remove(grid);
        // Detached, the component no longer follows the action: its listener was removed
        refreshAction.setEnabled(false);
        assertThat(refreshButton.isVisible()).isTrue();

        parent.add(grid);
        // Attached again, it follows the action again
        assertThat(refreshButton.isVisible()).isFalse();
        refreshAction.setEnabled(true);
        assertThat(refreshButton.isVisible()).isTrue();

        CollectionLoader<TaskType> loader = ViewControllerUtils.getViewData(view).getLoader("taskTypesDl");
        AtomicInteger loads = new AtomicInteger();
        loader.addPostLoadListener(event -> loads.incrementAndGet());

        refreshButton.click();

        assertThat(loads).hasValue(1);
    }

    @Test
    void refreshMovesTheFocusToTheGridWhenRowsArrive() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        // Drops what opening the view queued for the browser
        browserCalls(grid);

        saveTaskType(NAME_PREFIX + 1);
        emptyStateOf(view, "refreshGrid").getRefreshButton().click();

        assertThat(Objects.requireNonNull(grid.getItems()).getItems()).isNotEmpty();
        // The grid's host takes no focus, so the call focuses the body cell that Tab would land on
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("this._itemsFocusable?.focus()"));
    }

    @Test
    void refreshKeepsTheFocusWhileTheGridStaysEmpty() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        browserCalls(grid);

        emptyStateOf(view, "refreshGrid").getRefreshButton().click();

        assertThat(Objects.requireNonNull(grid.getItems()).getItems()).isEmpty();
        assertThat(browserCalls(grid)).noneMatch(call -> call.contains("focus"));
    }

    @Test
    void refreshShowsOnATreeDataGrid() {
        GridEmptyStateTestView view = openView();
        TreeDataGrid<TaskType> grid = UiTestUtils.getComponent(view, "treeGrid");
        CollectionLoader<TaskType> loader = ViewControllerUtils.getViewData(view).getLoader("treeTaskTypesDl");
        AtomicInteger loads = new AtomicInteger();
        loader.addPostLoadListener(event -> loads.incrementAndGet());
        JmixButton refreshButton = emptyStateOf(grid).getRefreshButton();

        assertThat(refreshButton.isVisible()).isTrue();
        assertThat(refreshButton.getText()).isEqualTo(messages.getMessage("actions.Refresh"));

        refreshButton.click();

        assertThat(loads).hasValue(1);
    }

    @Test
    void accessDeniedWinsWhenSecurityHidesEveryColumn() {
        User user = saveUser();
        assignRole(user, UiMinimalRole.CODE);
        assignRole(user, TaskTypeNoAttributesTestRole.CODE);

        systemAuthenticator.runWithUser(user.getUsername(), () -> {
            viewBuilders.view(UiTestUtils.getCurrentView(), GridEmptyStateTestView.class).open();
            GridEmptyStateTestView view = UiTestUtils.getCurrentView();
            DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");

            // The delegate keeps returning the component that was set and gives the fragment only to the slot
            Component emptyStateSlot = grid.getChildren()
                    .filter(c -> "empty-state".equals(c.getElement().getAttribute("slot")))
                    .findFirst()
                    .orElseThrow();
            assertThat(emptyStateSlot).isInstanceOf(DataGridEmptyStateByPermissionsFragment.class);
        });
    }

    @AfterEach
    void tearDown() {
        // Task types, users, and role assignments are soft-deleted, so they are removed for good, reloaded by id
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

    private GridEmptyStateTestView openView() {
        viewBuilders.view(UiTestUtils.getCurrentView(), GridEmptyStateTestView.class).open();
        return UiTestUtils.getCurrentView();
    }

    private static GridEmptyState emptyStateOf(GridEmptyStateTestView view, String gridId) {
        return emptyStateOf(UiTestUtils.getComponent(view, gridId));
    }

    private static GridEmptyState emptyStateOf(Grid<?> grid) {
        assertThat(grid.getEmptyStateComponent()).isInstanceOf(GridEmptyState.class);
        return (GridEmptyState) grid.getEmptyStateComponent();
    }

    /**
     * Returns the calls to the grid's browser side queued since the last check, as a response would send them.
     */
    private static List<String> browserCalls(Grid<?> grid) {
        UIInternals internals = UI.getCurrent().getInternals();
        internals.getStateTree().runExecutionsBeforeClientResponse();
        return internals.dumpPendingJavaScriptInvocations().stream()
                .filter(invocation -> invocation.getOwner() == grid.getElement().getNode())
                .map(invocation -> invocation.getInvocation().getExpression())
                .toList();
    }

    private void saveTaskType(String name) {
        TaskType taskType = dataManager.create(TaskType.class);
        taskType.setName(name);
        createdIds.add(Id.of(dataManager.save(taskType)));
    }

    private User saveUser() {
        User user = dataManager.create(User.class);
        user.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        User saved = dataManager.save(user);
        createdIds.add(Id.of(saved));
        return saved;
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }
}
