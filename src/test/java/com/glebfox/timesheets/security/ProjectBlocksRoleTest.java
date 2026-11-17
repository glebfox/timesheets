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

import com.glebfox.timesheets.entity.client.Client;
import com.glebfox.timesheets.entity.project.Project;
import com.glebfox.timesheets.entity.project.ProjectParticipant;
import com.glebfox.timesheets.entity.project.ProjectRole;
import com.glebfox.timesheets.entity.tag.Tag;
import com.glebfox.timesheets.entity.tag.TagGroup;
import com.glebfox.timesheets.entity.task.Task;
import com.glebfox.timesheets.entity.task.TaskType;
import com.glebfox.timesheets.entity.timeentry.TimeEntry;
import com.glebfox.timesheets.entity.user.User;
import com.glebfox.timesheets.test_support.AuthenticatedAsAdmin;
import com.glebfox.timesheets.view.calendar.CalendarView;
import com.glebfox.timesheets.view.timeentry.MyTimeEntryListView;
import com.glebfox.timesheets.view.timeentry.TimeEntryDetailView;
import io.jmix.core.AccessManager;
import io.jmix.core.DataManager;
import io.jmix.core.Id;
import io.jmix.core.Metadata;
import io.jmix.core.SaveContext;
import io.jmix.core.metamodel.model.MetaClass;
import io.jmix.core.metamodel.model.MetaProperty;
import io.jmix.core.security.SystemAuthenticator;
import io.jmix.data.PersistenceHints;
import io.jmix.flowui.accesscontext.UiEntityAttributeContext;
import io.jmix.flowui.accesscontext.UiEntityContext;
import io.jmix.flowui.accesscontext.UiMenuContext;
import io.jmix.flowui.accesscontext.UiShowViewContext;
import io.jmix.flowui.menu.MenuConfig;
import io.jmix.flowui.menu.MenuItem;
import io.jmix.security.role.assignment.RoleAssignmentRoleType;
import io.jmix.securitydata.entity.RoleAssignmentEntity;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The resource roles that project work is built from: Projects: read only, Projects: management, Time entries: read
 * only, Projects: create and remove, Client management, and Task types and project roles. Each test has a holder of
 * one of them, who signs in with UI: minimal access and holds no row-level role, so only that role's own grants are
 * checked.
 * <p>
 * The data layer checks the entity's READ only for the entity a load starts from, so the grants are checked with the
 * entity and attribute contexts of the UI, which check each entity.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class ProjectBlocksRoleTest {

    static final String USERNAME_PREFIX = "project-blocks-test-";

    /**
     * What the project screens show of a project, read by Projects: read only.
     */
    static final List<Class<?>> PROJECT_DATA = List.of(Project.class, Task.class, ProjectParticipant.class,
            ProjectRole.class, TagGroup.class, Tag.class, TaskType.class, Client.class, User.class);

    @Autowired
    DataManager dataManager;
    @Autowired
    AccessManager accessManager;
    @Autowired
    Metadata metadata;
    @Autowired
    MenuConfig menuConfig;
    @Autowired
    SystemAuthenticator systemAuthenticator;

    List<Id<?>> createdIds = new ArrayList<>();

    @Test
    void projectReadHolderReadsProjectDataOnly() {
        systemAuthenticator.runWithUser(holderOf(ProjectReadOnlyRole.CODE), () -> {
            for (Class<?> entityClass : PROJECT_DATA) {
                assertReadOnly(entityClass);
            }
            assertNotRead(TimeEntry.class);
        });
    }

    @Test
    void projectReadHolderOpensTheProjectScreens() {
        systemAuthenticator.runWithUser(holderOf(ProjectReadOnlyRole.CODE), () -> {
            assertViewsPermitted("ts_Project.list", "ts_Project.detail", "ts_Task.detail",
                    "ts_ProjectParticipant.detail", "ts_TagGroup.detail", "ts_Tag.detail");
            assertMenuItemsPermitted("ts_Project.list");

            // The editors' pickers are Projects: management's, and the Users screen is no project screen
            assertViewsDenied("ts_TagGroup.lookup", "ts_User.list", "ts_ProjectRole.list", "ts_TaskType.list",
                    "ts_Client.list", "ts_TimeEntry.list");
            assertMenuItemsDenied("ts_User.list", "ts_ProjectRole.list", "ts_TaskType.list", "ts_Client.list",
                    "ts_TimeEntry.list");
        });
    }

    @Test
    void projectManagementHolderChangesProjectsAndTheirDataButNeitherCreatesNorRemovesProjects() {
        systemAuthenticator.runWithUser(holderOf(ProjectManagementRole.CODE), () -> {
            assertGrants(Project.class, false, true, false);
            for (Class<?> entityClass : List.of(Task.class, ProjectParticipant.class, TagGroup.class, Tag.class)) {
                assertGrants(entityClass, true, true, true);
            }
            // What the editors show and offer of other entities stays read only, from Projects: read only
            for (Class<?> entityClass : List.of(ProjectRole.class, TaskType.class, Client.class, User.class)) {
                assertReadOnly(entityClass);
            }
            assertNotRead(TimeEntry.class);
        });
    }

    @Test
    void projectManagementHolderOpensThePickersButNotTheirMenuItems() {
        systemAuthenticator.runWithUser(holderOf(ProjectManagementRole.CODE), () -> {
            assertViewsPermitted("ts_Project.list", "ts_Project.detail", "ts_Task.detail",
                    "ts_ProjectParticipant.detail", "ts_TagGroup.detail", "ts_Tag.detail");
            assertMenuItemsPermitted("ts_Project.list");

            // The editors open these lists as pickers, and tabbed mode opens them by their URLs too, but the menu
            // does not list them
            assertViewsPermitted("ts_TagGroup.lookup", "ts_User.list", "ts_ProjectRole.list", "ts_TaskType.list",
                    "ts_Client.list");
            assertMenuItemsDenied("ts_User.list", "ts_ProjectRole.list", "ts_TaskType.list", "ts_Client.list");

            // Nor their editors: a project role, a task type, a client, or a user is not created here
            assertViewsDenied("ts_User.detail", "ts_ProjectRole.detail", "ts_TaskType.detail", "ts_Client.detail");
        });
    }

    @Test
    void timeEntryReadHolderReadsTimeEntriesAndWhatTheirScreensShow() {
        systemAuthenticator.runWithUser(holderOf(TimeEntryReadOnlyRole.CODE), () -> {
            for (Class<?> entityClass : List.of(TimeEntry.class, Task.class, Project.class, Tag.class,
                    TagGroup.class, User.class)) {
                assertReadOnly(entityClass);
            }
            for (Class<?> entityClass : List.of(ProjectParticipant.class, ProjectRole.class, TaskType.class,
                    Client.class)) {
                assertNotRead(entityClass);
            }
        });
    }

    @Test
    void timeEntryReadHolderOpensTheTimeEntriesListAndEditor() {
        systemAuthenticator.runWithUser(holderOf(TimeEntryReadOnlyRole.CODE), () -> {
            assertViewsPermitted("ts_TimeEntry.list", TimeEntryDetailView.ID);
            assertMenuItemsPermitted("ts_TimeEntry.list");

            // Logging one's own time is Time logging's, and the projects are Projects: read only's
            assertViewsDenied(CalendarView.ID, MyTimeEntryListView.ID, "ts_Task.my", "ts_Project.list",
                    "ts_Task.detail", "ts_User.list");
            assertMenuItemsDenied(CalendarView.ID, MyTimeEntryListView.ID, "ts_Task.my", "ts_Project.list");
        });
    }

    @Test
    void projectCreationHolderCreatesAndRemovesProjectsButOpensNoView() {
        systemAuthenticator.runWithUser(holderOf(ProjectCreationRole.CODE), () -> {
            assertGrants(Project.class, true, false, true);
            for (Class<?> entityClass : List.of(Task.class, ProjectParticipant.class, TagGroup.class, Tag.class,
                    Client.class)) {
                assertNotRead(entityClass);
            }

            // The project screens come with Projects: read only or Projects: management
            assertViewsDenied("ts_Project.list", "ts_Project.detail");
            assertMenuItemsDenied("ts_Project.list");
        });
    }

    @Test
    void clientManagementHolderManagesClientsInTheirScreens() {
        systemAuthenticator.runWithUser(holderOf(ClientManagementRole.CODE), () -> {
            assertGrants(Client.class, true, true, true);
            assertNotRead(Project.class);

            assertViewsPermitted("ts_Client.list", "ts_Client.detail");
            assertMenuItemsPermitted("ts_Client.list");
            assertViewsDenied("ts_Project.list");
            assertMenuItemsDenied("ts_Project.list");
        });
    }

    @Test
    void projectCatalogsHolderManagesTaskTypesAndProjectRolesInTheirScreens() {
        systemAuthenticator.runWithUser(holderOf(ProjectCatalogsRole.CODE), () -> {
            assertGrants(TaskType.class, true, true, true);
            assertGrants(ProjectRole.class, true, true, true);
            assertNotRead(Project.class);

            assertViewsPermitted("ts_TaskType.list", "ts_TaskType.detail", "ts_ProjectRole.list",
                    "ts_ProjectRole.detail");
            assertMenuItemsPermitted("ts_TaskType.list", "ts_ProjectRole.list");
            assertViewsDenied("ts_Project.list");
            assertMenuItemsDenied("ts_Project.list");
        });
    }

    @AfterEach
    void tearDown() {
        // Role assignments and users are soft-deleted, so they are removed for good, reloaded by id
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
     * Saves a user with UI: minimal access and the given role, and returns their username.
     */
    private String holderOf(String roleCode) {
        User user = dataManager.create(User.class);
        user.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        user = dataManager.save(user);
        createdIds.add(Id.of(user));
        assignRole(user, UiMinimalRole.CODE);
        assignRole(user, roleCode);
        return user.getUsername();
    }

    private void assertReadOnly(Class<?> entityClass) {
        String name = entityClass.getSimpleName();
        UiEntityContext context = entityContext(entityClass);
        assertThat(context.isViewPermitted()).as("read %s", name).isTrue();
        assertThat(context.isCreatePermitted()).as("create %s", name).isFalse();
        assertThat(context.isEditPermitted()).as("update %s", name).isFalse();
        assertThat(context.isDeletePermitted()).as("delete %s", name).isFalse();
        assertAttributes(entityClass, false);
    }

    /**
     * Asserts that the entity is read, with every attribute modifiable, and created, updated, and deleted as given.
     */
    private void assertGrants(Class<?> entityClass, boolean create, boolean update, boolean delete) {
        String name = entityClass.getSimpleName();
        UiEntityContext context = entityContext(entityClass);
        assertThat(context.isViewPermitted()).as("read %s", name).isTrue();
        assertThat(context.isCreatePermitted()).as("create %s", name).isEqualTo(create);
        assertThat(context.isEditPermitted()).as("update %s", name).isEqualTo(update);
        assertThat(context.isDeletePermitted()).as("delete %s", name).isEqualTo(delete);
        assertAttributes(entityClass, true);
    }

    private void assertNotRead(Class<?> entityClass) {
        assertThat(entityContext(entityClass).isViewPermitted())
                .as("read %s", entityClass.getSimpleName())
                .isFalse();
    }

    private void assertAttributes(Class<?> entityClass, boolean modifiable) {
        MetaClass metaClass = metadata.getClass(entityClass);
        for (MetaProperty property : metaClass.getProperties()) {
            UiEntityAttributeContext context = new UiEntityAttributeContext(metaClass, property.getName());
            accessManager.applyRegisteredConstraints(context);
            assertThat(context.canView())
                    .as("view %s.%s", entityClass.getSimpleName(), property.getName())
                    .isTrue();
            assertThat(context.canModify())
                    .as("modify %s.%s", entityClass.getSimpleName(), property.getName())
                    .isEqualTo(modifiable);
        }
    }

    private void assertViewsPermitted(String... viewIds) {
        for (String viewId : viewIds) {
            assertThat(viewPermitted(viewId)).as("view %s", viewId).isTrue();
        }
    }

    private void assertViewsDenied(String... viewIds) {
        for (String viewId : viewIds) {
            assertThat(viewPermitted(viewId)).as("view %s", viewId).isFalse();
        }
    }

    private void assertMenuItemsPermitted(String... menuItemIds) {
        for (String menuItemId : menuItemIds) {
            assertThat(menuItemPermitted(menuItemId)).as("menu item %s", menuItemId).isTrue();
        }
    }

    private void assertMenuItemsDenied(String... menuItemIds) {
        for (String menuItemId : menuItemIds) {
            assertThat(menuItemPermitted(menuItemId)).as("menu item %s", menuItemId).isFalse();
        }
    }

    private UiEntityContext entityContext(Class<?> entityClass) {
        UiEntityContext context = new UiEntityContext(metadata.getClass(entityClass));
        accessManager.applyRegisteredConstraints(context);
        return context;
    }

    private boolean viewPermitted(String viewId) {
        UiShowViewContext context = new UiShowViewContext(viewId);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    private boolean menuItemPermitted(String menuItemId) {
        MenuItem menuItem = findMenuItem(menuConfig.getRootItems(), menuItemId);
        assertThat(menuItem).as("menu item %s", menuItemId).isNotNull();
        UiMenuContext context = new UiMenuContext(menuItem);
        accessManager.applyRegisteredConstraints(context);
        return context.isPermitted();
    }

    @Nullable
    private static MenuItem findMenuItem(List<MenuItem> items, String id) {
        for (MenuItem item : items) {
            if (id.equals(item.getId())) {
                return item;
            }
            MenuItem child = findMenuItem(item.getChildren(), id);
            if (child != null) {
                return child;
            }
        }
        return null;
    }

    private void assignRole(User user, String roleCode) {
        RoleAssignmentEntity assignment = dataManager.create(RoleAssignmentEntity.class);
        assignment.setUsername(user.getUsername());
        assignment.setRoleCode(roleCode);
        assignment.setRoleType(RoleAssignmentRoleType.RESOURCE);
        createdIds.add(Id.of(dataManager.save(assignment)));
    }
}
