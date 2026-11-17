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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The holder signs in with UI: minimal access and holds the role besides, and no row-level role, so only the role's
 * own grants are checked.
 */
@SpringBootTest
@ExtendWith(AuthenticatedAsAdmin.class)
public class TimeLoggingRoleTest {

    static final String USERNAME_PREFIX = "time-logging-test-";

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

    String holder;
    List<Id<?>> createdIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        User holderUser = dataManager.create(User.class);
        holderUser.setUsername(USERNAME_PREFIX + UUID.randomUUID());
        holderUser = dataManager.save(holderUser);
        createdIds.add(Id.of(holderUser));
        assignRole(holderUser, UiMinimalRole.CODE);
        assignRole(holderUser, TimeLoggingRole.CODE);
        holder = holderUser.getUsername();
    }

    @Test
    void holderWritesTimeEntriesButNotTheirUserStatusOrReason() {
        systemAuthenticator.runWithUser(holder, () -> {
            UiEntityContext context = entityContext(TimeEntry.class);
            assertThat(context.isViewPermitted()).isTrue();
            assertThat(context.isCreatePermitted()).isTrue();
            assertThat(context.isEditPermitted()).isTrue();
            assertThat(context.isDeletePermitted()).isTrue();

            // The user stays view-only on create too: every editor that creates an own entry sets the user itself
            Set<String> modifiable = Set.of("date", "task", "spentTime", "description", "tags");
            MetaClass metaClass = metadata.getClass(TimeEntry.class);
            for (MetaProperty property : metaClass.getProperties()) {
                UiEntityAttributeContext attributeContext =
                        new UiEntityAttributeContext(metaClass, property.getName());
                accessManager.applyRegisteredConstraints(attributeContext);
                assertThat(attributeContext.canView()).as("view %s", property.getName()).isTrue();
                assertThat(attributeContext.canModify())
                        .as("modify %s", property.getName())
                        .isEqualTo(modifiable.contains(property.getName()));
            }
        });
    }

    @Test
    void holderReadsWhatTheScreensShow() {
        // The data layer checks the entity's READ only for the entity a load starts from, so a load cannot show the
        // grants of the entities the screens reach through others. The entity context of the UI checks each of them
        systemAuthenticator.runWithUser(holder, () -> {
            for (Class<?> entityClass : List.of(Task.class, Project.class, ProjectParticipant.class,
                    ProjectRole.class, TagGroup.class, Tag.class, TaskType.class, User.class)) {
                UiEntityContext context = entityContext(entityClass);
                assertThat(context.isViewPermitted()).as("read %s", entityClass.getSimpleName()).isTrue();
                assertThat(context.isEditPermitted()).as("update %s", entityClass.getSimpleName()).isFalse();
                assertThat(context.isCreatePermitted()).as("create %s", entityClass.getSimpleName()).isFalse();
                assertThat(context.isDeletePermitted()).as("delete %s", entityClass.getSimpleName()).isFalse();

                MetaClass metaClass = metadata.getClass(entityClass);
                for (MetaProperty property : metaClass.getProperties()) {
                    UiEntityAttributeContext attributeContext =
                            new UiEntityAttributeContext(metaClass, property.getName());
                    accessManager.applyRegisteredConstraints(attributeContext);
                    assertThat(attributeContext.canView())
                            .as("view %s.%s", entityClass.getSimpleName(), property.getName())
                            .isTrue();
                    assertThat(attributeContext.canModify())
                            .as("modify %s.%s", entityClass.getSimpleName(), property.getName())
                            .isFalse();
                }
            }
        });
    }

    @Test
    void holderOpensTheTimesheetsScreensAndMenuItems() {
        systemAuthenticator.runWithUser(holder, () -> {
            for (String viewId : List.of(CalendarView.ID, MyTimeEntryListView.ID, "ts_Task.my",
                    TimeEntryDetailView.ID, "ts_Task.lookup", "ts_Task.detail")) {
                assertThat(viewPermitted(viewId)).as("view %s", viewId).isTrue();
            }
            for (String menuItemId : List.of(CalendarView.ID, MyTimeEntryListView.ID, "ts_Task.my")) {
                assertThat(menuItemPermitted(menuItemId)).as("menu item %s", menuItemId).isTrue();
            }

            // Everyone's time entries, the projects, and the users are other roles' screens
            for (String viewId : List.of("ts_TimeEntry.list", "ts_Project.list", "ts_Project.detail",
                    "ts_User.list")) {
                assertThat(viewPermitted(viewId)).as("view %s", viewId).isFalse();
            }
            for (String menuItemId : List.of("ts_TimeEntry.list", "ts_Project.list", "ts_User.list")) {
                assertThat(menuItemPermitted(menuItemId)).as("menu item %s", menuItemId).isFalse();
            }
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
