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

package com.glebfox.timesheets.security;

import com.glebfox.timesheets.TimesheetsApplication;
import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.project.ProjectRoleType;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.project.ProjectDetailView;
import com.glebfox.timesheets.view.projectparticipant.ProjectParticipantDetailView;
import com.glebfox.timesheets.view.projectrole.ProjectRoleListView;
import com.glebfox.timesheets.view.task.TaskDetailView;
import com.glebfox.timesheets.view.user.UserListView;
import com.vaadin.flow.component.UI;
import io.jmix.core.DataManager;
import io.jmix.core.SaveContext;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.view.StandardOutcome;
import io.jmix.flowui.view.View;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import io.jmix.tabbedmode.ViewBuilders;
import io.jmix.tabbedmode.testassist.TabbedModeTestAssistConfiguration;
import io.jmix.tabbedmode.testassist.UiTest;
import io.jmix.tabbedmode.testassist.UiTestUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The holder signs in with UI: minimal access, holds Projects: management, and has Project scope as their row-level
 * role, as a Manager does. They are the Manager of one project and an Approver of another.
 */
@UiTest
@SpringBootTest(classes = {TimesheetsApplication.class, TabbedModeTestAssistConfiguration.class})
public class ProjectManagementRoleUiTest {

    static final String USERNAME_PREFIX = "project-management-ui-test-";

    @Autowired
    DataManager dataManager;
    @Autowired
    ViewBuilders viewBuilders;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    ApprovalTestData data;
    User holder;
    TaskType taskType;
    Project managed;
    Project approved;

    @BeforeEach
    void setUp() {
        data = new ApprovalTestData(dataManager, USERNAME_PREFIX + UUID.randomUUID().toString().substring(0, 8));

        holder = data.saveUser("holder", "Holder", "Manager");
        assignRole(holder, UiMinimalRole.CODE, RoleAssignmentRoleType.RESOURCE);
        assignRole(holder, ProjectManagementRole.CODE, RoleAssignmentRoleType.RESOURCE);
        assignRole(holder, ProjectScopeRole.CODE, RoleAssignmentRoleType.ROW_LEVEL);

        Client client = data.saveClient();
        taskType = data.saveTaskType();
        ProjectRole managerRole = data.saveRole(ProjectRoleType.MANAGER);
        ProjectRole approverRole = data.saveRole(ProjectRoleType.APPROVER);

        managed = data.saveProject("managed", client);
        data.saveParticipant(managed, holder, managerRole);
        approved = data.saveProject("approved", client);
        data.saveParticipant(approved, holder, approverRole);
    }

    @Test
    void holderAddsATaskToAManagedProject() {
        String taskName = USERNAME_PREFIX + "added task";

        systemAuthenticator.runWithUser(holder.getUsername(), () -> {
            ProjectDetailView editor = openEditor(managed);
            assertThat(editor.isReadOnly()).isFalse();
            assertThat(UiTestUtils.<TypedTextField<String>>getComponent(editor, "nameField").isReadOnly()).isFalse();

            DataGrid<Task> tasksDataGrid = UiTestUtils.getComponent(editor, "tasksDataGrid");
            TaskDetailView taskEditor = (TaskDetailView) performForDialog(tasksDataGrid, "create");
            assertThat(taskEditor.isReadOnly()).isFalse();

            TypedTextField<String> nameField = UiTestUtils.getComponent(taskEditor, "nameField");
            EntityComboBox<TaskType> typeField = UiTestUtils.getComponent(taskEditor, "typeField");
            assertThat(nameField.isReadOnly()).isFalse();
            assertThat(typeField.isReadOnly()).isFalse();
            nameField.setTypedValue(taskName);
            typeField.setValue(taskType);
            UiTestUtils.<JmixButton>getComponent(taskEditor, "saveAndCloseBtn").click();
            assertThat(UiTestUtils.getOpenedViewDialogs()).doesNotContain(taskEditor);

            // The task waits for the project editor's OK, as the project's composition
            UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();
        });

        List<Task> tasks = dataManager.load(Task.class)
                .query("e.project = ?1", managed)
                .list();
        assertThat(tasks).extracting(Task::getName).containsExactly(taskName);
    }

    @Test
    void holderOpensAProjectTheyApproveReadOnly() {
        systemAuthenticator.runWithUser(holder.getUsername(), () -> {
            ProjectDetailView editor = openEditor(approved);

            assertThat(editor.isReadOnly()).isTrue();
            assertThat(UiTestUtils.<TypedTextField<String>>getComponent(editor, "nameField").isReadOnly()).isTrue();
            DataGrid<Task> tasksDataGrid = UiTestUtils.getComponent(editor, "tasksDataGrid");
            assertThat(Objects.requireNonNull(tasksDataGrid.getAction("create")).isEnabled()).isFalse();
        });
    }

    @Test
    void participantDialogOffersProjectRolesToPickButNotToCreate() {
        systemAuthenticator.runWithUser(holder.getUsername(), () -> {
            ProjectDetailView editor = openEditor(managed);
            // The participants are on the second tab, whose content stays disabled until it is selected
            UiTestUtils.<JmixTabSheet>getComponent(editor, "tabSheet").setSelectedIndex(1);
            attachSelectedTab();
            DataGrid<ProjectParticipant> participantsDataGrid =
                    UiTestUtils.getComponent(editor, "participantsDataGrid");
            ProjectParticipantDetailView participantEditor =
                    (ProjectParticipantDetailView) performForDialog(participantsDataGrid, "create");
            assertThat(participantEditor.isReadOnly()).isFalse();

            // A Manager may not create project roles, so the role field offers none to create
            EntityComboBox<ProjectRole> roleField = UiTestUtils.getComponent(participantEditor, "roleField");
            assertThat(Objects.requireNonNull(roleField.getAction("entityCreate")).isVisible()).isFalse();

            // The pickers of both fields open their lists, which the role grants as lookups
            openAndCloseLookup(roleField, ProjectRoleListView.class);
            openAndCloseLookup(UiTestUtils.getComponent(participantEditor, "userField"), UserListView.class);

            participantEditor.closeWithDiscard();
        });
    }

    @AfterEach
    void tearDown() {
        // The tasks saved through the editor first, then the fixture. All are soft-deleted, so they are removed for
        // good
        if (managed != null) {
            List<Task> tasks = dataManager.load(Task.class)
                    .query("e.project = ?1", managed)
                    .hint(PersistenceHints.SOFT_DELETION, false)
                    .list();
            if (!tasks.isEmpty()) {
                dataManager.save(new SaveContext()
                        .removing(tasks.toArray())
                        .setHint(PersistenceHints.SOFT_DELETION, false));
            }
        }
        data.removeAll();
    }

    private ProjectDetailView openEditor(Project project) {
        viewBuilders.detail(UiTestUtils.getCurrentView(), Project.class, ProjectDetailView.class)
                .editEntity(project)
                .open();
        ProjectDetailView editor = UiTestUtils.getCurrentView();
        attachSelectedTab();
        return editor;
    }

    /**
     * The tab sheet attaches the selected tab's content to the view only before the client response, which a UI test
     * never sends; until then a grid on it is not in the view, and its create action cannot open a dialog.
     */
    private static void attachSelectedTab() {
        UI.getCurrent().getInternals().getStateTree().runExecutionsBeforeClientResponse();
    }

    /**
     * Performs a list action that opens an editor as a dialog over the project editor, and returns the editor.
     */
    private static View<?> performForDialog(DataGrid<?> grid, String actionId) {
        Action action = Objects.requireNonNull(grid.getAction(actionId), "action " + actionId);
        assertThat(action.isEnabled()).as("action %s", actionId).isTrue();
        action.actionPerform(grid);
        return Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the dialog of " + actionId);
    }

    private static void openAndCloseLookup(EntityComboBox<?> field, Class<? extends View<?>> lookupClass) {
        Action lookup = Objects.requireNonNull(field.getAction("entityLookup"), "the lookup of " + lookupClass);
        assertThat(lookup.isVisible()).isTrue();
        assertThat(lookup.isEnabled()).isTrue();
        lookup.actionPerform(field);

        View<?> opened = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the lookup dialog");
        assertThat(opened).isInstanceOf(lookupClass);
        opened.close(StandardOutcome.CLOSE);
    }

    private void assignRole(User user, String roleCode, String roleType) {
        data.save(dataManager.create(RoleAssignmentEntity.class), assignment -> {
            assignment.setUsername(user.getUsername());
            assignment.setRoleCode(roleCode);
            assignment.setRoleType(roleType);
        });
    }
}
