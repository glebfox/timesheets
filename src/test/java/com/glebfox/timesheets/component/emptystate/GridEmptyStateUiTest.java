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
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.FontIcon;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.internal.UIInternals;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.UiComponents;
import io.jmix.flowui.app.datagrid.DataGridEmptyStateByPermissionsFragment;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.grid.TreeDataGrid;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.action.BaseAction;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.model.CollectionLoader;
import io.jmix.flowui.view.ViewControllerUtils;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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
    // A spy, so a test can count the components the empty state creates through it
    @MockitoSpyBean
    UiComponents uiComponents;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void loadsTheIconTextAndActionFromXml() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        GridEmptyState emptyState = emptyStateOf(grid);

        assertThat(emptyState.getIcon())
                .isInstanceOfSatisfying(Icon.class, icon -> assertThat(icon.getIcon()).isEqualTo("ts:task-types"));
        assertDecorative(emptyState.getIcon());
        assertThat(emptyState.getText()).isEqualTo("No task types");
        assertThat(emptyState.hasClassName("grid-empty-state")).isTrue();
        assertThat(emptyState.getAction()).isSameAs(grid.getAction("refresh"));
    }

    @Test
    void loadsANestedIconComponent() {
        GridEmptyState emptyState = emptyStateOf(openView(), "plainGrid");

        assertThat(emptyState.getIcon())
                .isInstanceOfSatisfying(FontIcon.class, icon -> assertThat(icon.getLigature()).isEqualTo("sell"));
        assertDecorative(emptyState.getIcon());
    }

    @Test
    void noActionNoButton() {
        GridEmptyState emptyState = emptyStateOf(openView(), "plainGrid");

        assertThat(emptyState.getAction()).isNull();
        assertThat(buttonsOf(emptyState)).isEmpty();
    }

    @Test
    void theButtonShowsTheGridsRefresh() {
        JmixButton button = actionButtonOf(emptyStateOf(openView(), "refreshGrid"));

        assertThat(button.isVisible()).isTrue();
        assertThat(button.isEnabled()).isTrue();
        assertThat(button.getText()).isEqualTo(messages.getMessage("actions.Refresh"));
        assertThat(button.getThemeNames()).contains(ButtonVariant.TERTIARY.getVariantName());
        assertThat(button.hasClassName("action-button")).isTrue();
    }

    @Test
    void setActionCreatesTheButtonOnceAndNullRemovesIt() {
        GridEmptyState emptyState = uiComponents.create(GridEmptyState.class);
        clearInvocations(uiComponents);
        AtomicInteger firstRuns = new AtomicInteger();
        AtomicInteger secondRuns = new AtomicInteger();
        Action first = new BaseAction("first").withText("First").withHandler(event -> firstRuns.incrementAndGet());
        Action second = new BaseAction("second").withText("Second").withHandler(event -> secondRuns.incrementAndGet());

        emptyState.setAction(first);
        JmixButton button = actionButtonOf(emptyState);
        assertThat(button.getText()).isEqualTo("First");
        assertThat(emptyState.getAction()).isSameAs(first);

        emptyState.setAction(second);
        assertThat(buttonsOf(emptyState)).containsExactly(button);
        assertThat(button.getText()).isEqualTo("Second");
        assertThat(emptyState.getAction()).isSameAs(second);
        verify(uiComponents, times(1)).create(JmixButton.class);

        button.click();
        assertThat(firstRuns).hasValue(0);
        assertThat(secondRuns).hasValue(1);

        emptyState.setAction(null);
        assertThat(emptyState.getAction()).isNull();
        assertThat(buttonsOf(emptyState)).isEmpty();
        // The removed button no longer follows the action
        second.setText("Changed");
        assertThat(button.getText()).isEqualTo("Second");
    }

    @Test
    void theButtonFollowsTheActionsVisibleAndEnabled() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        Action refreshAction = Objects.requireNonNull(grid.getAction("refresh"));
        JmixButton button = actionButtonOf(emptyStateOf(grid));

        refreshAction.setEnabled(false);
        assertThat(button.isVisible()).isTrue();
        assertThat(button.isEnabled()).isFalse();

        refreshAction.setEnabled(true);
        assertThat(button.isEnabled()).isTrue();

        refreshAction.setVisible(false);
        assertThat(button.isVisible()).isFalse();

        refreshAction.setVisible(true);
        assertThat(button.isVisible()).isTrue();
    }

    @Test
    void refreshLoadsRowsSavedAfterOpening() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        assertThat(grid.getItems().getItems()).isEmpty();

        saveTaskType(NAME_PREFIX + 1);
        actionButtonOf(emptyStateOf(grid)).click();

        assertThat(grid.getItems().getItems())
                .extracting(TaskType::getName)
                .containsExactly(NAME_PREFIX + 1);
    }

    @Test
    void refreshMovesTheFocusToTheGridWhenRowsArrive() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        // Drops what opening the view queued for the browser
        browserCalls(grid);

        saveTaskType(NAME_PREFIX + 1);
        actionButtonOf(emptyStateOf(grid)).click();

        assertThat(Objects.requireNonNull(grid.getItems()).getItems()).isNotEmpty();
        // The grid's host takes no focus, so the call focuses the body cell that Tab would land on
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("this._itemsFocusable?.focus()"));
    }

    @Test
    void refreshKeepsTheFocusWhileTheGridStaysEmpty() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        browserCalls(grid);

        actionButtonOf(emptyStateOf(grid)).click();

        assertThat(Objects.requireNonNull(grid.getItems()).getItems()).isEmpty();
        assertThat(browserCalls(grid)).noneMatch(call -> call.contains("focus"));
    }

    @Test
    void aLaterActionAlsoMovesTheFocus() {
        GridEmptyStateTestView view = openView();
        DataGrid<TaskType> grid = UiTestUtils.getComponent(view, "refreshGrid");
        CollectionLoader<TaskType> loader = ViewControllerUtils.getViewData(view).getLoader("taskTypesDl");
        GridEmptyState emptyState = emptyStateOf(grid);
        // The button runs the new action from a click listener registered after the one that moves the focus
        emptyState.setAction(new BaseAction("load").withText("Load").withHandler(event -> loader.load()));
        browserCalls(grid);

        saveTaskType(NAME_PREFIX + 1);
        actionButtonOf(emptyState).click();

        assertThat(Objects.requireNonNull(grid.getItems()).getItems()).isNotEmpty();
        assertThat(browserCalls(grid)).anyMatch(call -> call.contains("this._itemsFocusable?.focus()"));
    }

    @Test
    void refreshWorksOnATreeDataGrid() {
        GridEmptyStateTestView view = openView();
        TreeDataGrid<TaskType> grid = UiTestUtils.getComponent(view, "treeGrid");
        CollectionLoader<TaskType> loader = ViewControllerUtils.getViewData(view).getLoader("treeTaskTypesDl");
        AtomicInteger loads = new AtomicInteger();
        loader.addPostLoadListener(event -> loads.incrementAndGet());
        GridEmptyState emptyState = emptyStateOf(grid);
        JmixButton button = actionButtonOf(emptyState);

        assertThat(emptyState.getAction()).isSameAs(grid.getAction("refresh"));
        assertThat(button.isVisible()).isTrue();
        assertThat(button.getText()).isEqualTo(messages.getMessage("actions.Refresh"));

        button.click();

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
     * Returns the button of the empty state's action, found as any child component is.
     */
    private static JmixButton actionButtonOf(GridEmptyState emptyState) {
        List<JmixButton> buttons = buttonsOf(emptyState);
        assertThat(buttons).hasSize(1);
        return buttons.getFirst();
    }

    private static List<JmixButton> buttonsOf(Component component) {
        return descendants(component)
                .filter(JmixButton.class::isInstance)
                .map(JmixButton.class::cast)
                .toList();
    }

    private static Stream<Component> descendants(Component component) {
        return component.getChildren().flatMap(child -> Stream.concat(Stream.of(child), descendants(child)));
    }

    private static void assertDecorative(@Nullable Component icon) {
        assertThat(icon).isNotNull();
        assertThat(Objects.requireNonNull(icon).getElement().getAttribute("aria-hidden")).isEqualTo("true");
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
