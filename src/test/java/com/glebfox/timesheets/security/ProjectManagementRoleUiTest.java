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
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskStatus;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.ApprovalTestData;
import com.glebfox.timesheets.view.client.ClientDetailView;
import com.glebfox.timesheets.view.client.ClientListView;
import com.glebfox.timesheets.view.project.ProjectDetailView;
import com.glebfox.timesheets.view.projectparticipant.ProjectParticipantDetailView;
import com.glebfox.timesheets.view.projectrole.ProjectRoleDetailView;
import com.glebfox.timesheets.view.projectrole.ProjectRoleListView;
import com.glebfox.timesheets.view.tag.TagDetailView;
import com.glebfox.timesheets.view.taggroup.TagGroupDetailView;
import com.glebfox.timesheets.view.task.TaskDetailView;
import com.glebfox.timesheets.view.tasktype.TaskTypeDetailView;
import com.glebfox.timesheets.view.tasktype.TaskTypeListView;
import com.glebfox.timesheets.view.user.UserDetailView;
import com.glebfox.timesheets.view.user.UserListView;
import com.vaadin.flow.component.UI;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Messages;
import io.jmix.core.SaveContext;
import io.jmix.core.entity.EntityValues;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.component.combobox.EntityComboBox;
import io.jmix.flowui.component.grid.DataGrid;
import io.jmix.flowui.component.tabsheet.JmixTabSheet;
import io.jmix.flowui.component.textfield.TypedTextField;
import io.jmix.flowui.kit.action.Action;
import io.jmix.flowui.kit.component.button.JmixButton;
import io.jmix.flowui.kit.component.dropdownbutton.DropdownButton;
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
 * role, as a Manager does. They are the Manager of one project and an Approver of another. Each project has a task,
 * and the approved one also a tag group with a tag.
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
    @Autowired
    Messages messages;

    ApprovalTestData data;
    User holder;
    TaskType taskType;
    Project managed;
    Task managedTask;
    Project approved;
    Task approvedTask;
    ProjectParticipant approvedParticipant;
    TagGroup approvedTagGroup;
    Tag approvedTag;

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
        managedTask = data.saveTask(managed, taskType);
        approved = data.saveProject("approved", client);
        approvedParticipant = data.saveParticipant(approved, holder, approverRole);
        approvedTask = data.saveTask(approved, taskType);
        approvedTagGroup = data.save(dataManager.create(TagGroup.class), group -> {
            group.setName(USERNAME_PREFIX + "tag group");
            group.setProject(approved);
        });
        approvedTag = data.save(dataManager.create(Tag.class), tag -> {
            tag.setName(USERNAME_PREFIX + "tag");
            tag.setTagGroup(approvedTagGroup);
        });
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
        assertThat(tasks).extracting(Task::getName).containsExactlyInAnyOrder(managedTask.getName(), taskName);
    }

    @Test
    void holderEditsAndClosesATaskOfAManagedProject() {
        systemAuthenticator.runWithUser(holder.getUsername(), () -> {
            ProjectDetailView editor = openEditor(managed);
            DataGrid<Task> tasksDataGrid = UiTestUtils.getComponent(editor, "tasksDataGrid");
            tasksDataGrid.select(listed(tasksDataGrid, managedTask));

            assertThat(Objects.requireNonNull(tasksDataGrid.getAction("edit")).getText())
                    .isEqualTo(messages.getMessage("actions.Edit"));
            TaskDetailView taskEditor = (TaskDetailView) performForDialog(tasksDataGrid, "edit");
            assertThat(taskEditor.isReadOnly()).isFalse();
            taskEditor.closeWithDiscard();

            Action close = Objects.requireNonNull(tasksDataGrid.getAction("taskClose"));
            assertThat(close.isEnabled()).isTrue();
            close.actionPerform(tasksDataGrid);
            UiTestUtils.<JmixButton>getComponent(editor, "saveAndCloseBtn").click();
        });

        assertThat(dataManager.load(Id.of(managedTask)).one().getStatus()).isEqualTo(TaskStatus.INACTIVE);
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
    void holderReadsTheChildrenOfAProjectTheyApproveButClosesNoTask() {
        systemAuthenticator.runWithUser(holder.getUsername(), () -> {
            ProjectDetailView editor = openEditor(approved);
            DataGrid<Task> tasksDataGrid = UiTestUtils.getComponent(editor, "tasksDataGrid");
            tasksDataGrid.select(listed(tasksDataGrid, approvedTask));

            // Its save would be refused, so the editor does not offer it
            assertThat(Objects.requireNonNull(tasksDataGrid.getAction("taskClose")).isEnabled()).isFalse();

            // The action reads its label through Messages, as this call does, so both resolve the same locale
            assertThat(Objects.requireNonNull(tasksDataGrid.getAction("edit")).getText())
                    .isEqualTo(messages.getMessage("actions.Read"));
            TaskDetailView taskEditor = (TaskDetailView) performForDialog(tasksDataGrid, "edit");
            assertThat(taskEditor.isReadOnly()).isTrue();
            taskEditor.closeWithDiscard();

            assertThat(Objects.requireNonNull(tasksDataGrid.getAction("remove")).isEnabled()).isFalse();

            // The participants and tag groups open the same way. The tab is selected first, as a tab that is not
            // disables its content whatever the editor's mode
            UiTestUtils.<JmixTabSheet>getComponent(editor, "tabSheet").setSelectedIndex(1);
            attachSelectedTab();
            DataGrid<ProjectParticipant> participantsDataGrid =
                    UiTestUtils.getComponent(editor, "participantsDataGrid");
            participantsDataGrid.select(listed(participantsDataGrid, approvedParticipant));
            assertThat(Objects.requireNonNull(participantsDataGrid.getAction("edit")).isEnabled()).isTrue();
            assertThat(UiTestUtils.<DropdownButton>getComponent(editor, "addParticipantsDropdown").isEnabled())
                    .isFalse();

            UiTestUtils.<JmixTabSheet>getComponent(editor, "tabSheet").setSelectedIndex(2);
            attachSelectedTab();
            DataGrid<TagGroup> tagGroupsDataGrid = UiTestUtils.getComponent(editor, "tagGroupsDataGrid");
            tagGroupsDataGrid.select(listed(tagGroupsDataGrid, approvedTagGroup));
            TagGroupDetailView tagGroupEditor = (TagGroupDetailView) performForDialog(tagGroupsDataGrid, "edit");
            assertThat(tagGroupEditor.isReadOnly()).isTrue();

            // And so do the tag group's tags
            DataGrid<Tag> tagsDataGrid = UiTestUtils.getComponent(tagGroupEditor, "tagsDataGrid");
            tagsDataGrid.select(listed(tagsDataGrid, approvedTag));
            TagDetailView tagEditor = (TagDetailView) performForDialog(tagsDataGrid, "edit");
            assertThat(tagEditor.isReadOnly()).isTrue();
        });
    }

    @Test
    void participantDialogOffersProjectRolesToPickButNotToCreate() {
        systemAuthenticator.runWithUser(holder.getUsername(), () -> {
            ProjectDetailView editor = openEditor(managed);
            // The participants are on the second tab, whose content stays disabled until it is selected
            UiTestUtils.<JmixTabSheet>getComponent(editor, "tabSheet").setSelectedIndex(1);
            attachSelectedTab();
            assertThat(UiTestUtils.<DropdownButton>getComponent(editor, "addParticipantsDropdown").isEnabled())
                    .isTrue();
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

    @Test
    void pickersOfferTheirEditorsToTheHoldersOfTheirRoles() {
        User editorsHolder = data.saveUser("editors-holder", "Editors", "Holder");
        for (String roleCode : List.of(UiMinimalRole.CODE, ProjectManagementRole.CODE, ClientManagementRole.CODE,
                ProjectCatalogsRole.CODE, UserReadOnlyRole.CODE)) {
            assignRole(editorsHolder, roleCode, RoleAssignmentRoleType.RESOURCE);
        }
        assignRole(editorsHolder, ProjectScopeRole.CODE, RoleAssignmentRoleType.ROW_LEVEL);
        data.saveParticipant(managed, editorsHolder, data.saveRole(ProjectRoleType.MANAGER));

        systemAuthenticator.runWithUser(editorsHolder.getUsername(), this::assertPickersOpenTheirEditors);
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
     * Returns the item of the grid that is the given entity, by its id.
     */
    private static <E> E listed(DataGrid<E> grid, Object entity) {
        Object id = EntityValues.getId(entity);
        return Objects.requireNonNull(grid.getItems()).getItems().stream()
                .filter(item -> Objects.equals(EntityValues.getId(item), id))
                .findFirst()
                .orElseThrow(() -> new AssertionError(entity + " in " + grid.getId().orElse("the grid")));
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

    /**
     * Opens each picker of the managed project's editor and its dialogs, selects a row, and checks that Edit is offered
     * there and opens the list's editor.
     */
    private void assertPickersOpenTheirEditors() {
        ProjectDetailView editor = openEditor(managed);
        assertPickerEdit(UiTestUtils.getComponent(editor, "clientField"), "clientsDataGrid",
                ClientListView.class, ClientDetailView.class);

        DataGrid<Task> tasksDataGrid = UiTestUtils.getComponent(editor, "tasksDataGrid");
        TaskDetailView taskEditor = (TaskDetailView) performForDialog(tasksDataGrid, "create");
        assertPickerEdit(UiTestUtils.getComponent(taskEditor, "typeField"), "taskTypesDataGrid",
                TaskTypeListView.class, TaskTypeDetailView.class);
        taskEditor.closeWithDiscard();

        UiTestUtils.<JmixTabSheet>getComponent(editor, "tabSheet").setSelectedIndex(1);
        attachSelectedTab();
        DataGrid<ProjectParticipant> participantsDataGrid = UiTestUtils.getComponent(editor, "participantsDataGrid");
        ProjectParticipantDetailView participantEditor =
                (ProjectParticipantDetailView) performForDialog(participantsDataGrid, "create");
        assertPickerEdit(UiTestUtils.getComponent(participantEditor, "roleField"), "projectRolesDataGrid",
                ProjectRoleListView.class, ProjectRoleDetailView.class);
        assertPickerEdit(UiTestUtils.getComponent(participantEditor, "userField"), "usersDataGrid",
                UserListView.class, UserDetailView.class);
        participantEditor.closeWithDiscard();
    }

    private static void assertPickerEdit(EntityComboBox<?> field, String gridId,
                                         Class<? extends View<?>> listClass, Class<? extends View<?>> editorClass) {
        Action lookup = Objects.requireNonNull(field.getAction("entityLookup"), "the lookup of " + listClass);
        assertThat(lookup.isVisible()).isTrue();
        assertThat(lookup.isEnabled()).isTrue();
        lookup.actionPerform(field);
        View<?> list = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the lookup dialog");
        assertThat(list).isInstanceOf(listClass);

        DataGrid<Object> grid = UiTestUtils.getComponent(list, gridId);
        grid.select(Objects.requireNonNull(grid.getItems()).getItems().iterator().next());
        Action edit = Objects.requireNonNull(grid.getAction("edit"), "the edit of " + listClass);
        assertThat(edit.isEnabled()).as("edit in %s", listClass.getSimpleName()).isTrue();
        edit.actionPerform(grid);
        View<?> opened = Objects.requireNonNull(UiTestUtils.getLastOpenedViewDialog(), "the editor");
        assertThat(opened).isInstanceOf(editorClass);
        opened.close(StandardOutcome.CLOSE);
        list.close(StandardOutcome.CLOSE);
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
